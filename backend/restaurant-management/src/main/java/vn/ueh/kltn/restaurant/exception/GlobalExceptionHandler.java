package vn.ueh.kltn.restaurant.exception;

import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InventoryAvailabilityException.class)
    ResponseEntity<?> inventoryAvailability(InventoryAvailabilityException exception) {
        return ResponseEntity.status(409)
                .body(
                        Map.of(
                                "status",
                                409,
                                "code",
                                "INSUFFICIENT_STOCK",
                                "message",
                                exception.getMessage(),
                                "fieldErrors",
                                Map.of(),
                                "ingredients",
                                exception.getIngredients()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException exception) {
        return response(
                ErrorResponse.of(exception.getErrorCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        collectErrors(exception.getBindingResult(), fields);
        return invalidFields(fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> invalidRequest(HttpMessageNotReadableException exception) {
        return response(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return response(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED), exception.getHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return response(ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE), exception.getHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ErrorResponse> notAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return response(ErrorResponse.of(ErrorCode.NOT_ACCEPTABLE), exception.getHeaders());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException exception) {
        return invalidFields(
                Map.of(exception.getParameterName(), "Vui lòng nhập tham số bắt buộc"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return invalidFields(Map.of(exception.getName(), "Giá trị tham số không hợp lệ"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception
                .getConstraintViolations()
                .forEach(
                        violation -> {
                            String key = violation.getPropertyPath().toString();
                            addError(
                                    fields,
                                    key.isBlank() ? "_global" : key,
                                    violation.getMessage());
                        });
        return invalidFields(fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException exception) {
        // An invalid controller return value is a server fault, not bad client input.
        if (exception.isForReturnValue()) {
            log.error("Controller returned a value that failed validation", exception);
            return response(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
        }
        Map<String, String> fields = new LinkedHashMap<>();
        exception
                .getParameterValidationResults()
                .forEach(
                        result -> {
                            if (result instanceof ParameterErrors errors) {
                                collectErrors(errors, fields);
                            } else {
                                String name = result.getMethodParameter().getParameterName();
                                String key =
                                        name == null
                                                ? "arg"
                                                        + result.getMethodParameter()
                                                                .getParameterIndex()
                                                : name;
                                result.getResolvableErrors()
                                        .forEach(
                                                error ->
                                                        addError(
                                                                fields,
                                                                key,
                                                                validationMessage(error)));
                            }
                        });
        exception
                .getCrossParameterValidationResults()
                .forEach(error -> addError(fields, "_global", validationMessage(error)));
        return invalidFields(fields);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void disconnectedClient(AsyncRequestNotUsableException exception) {
        // The connection is already unusable; attempting to write JSON would fail again.
        log.debug("Async client disconnected: {}", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataConflict(DataIntegrityViolationException exception) {
        return response(ErrorResponse.of(ErrorCode.DATA_CONFLICT));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NoResourceFoundException exception) {
        return response(ErrorResponse.of(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> forbidden(AccessDeniedException exception) {
        return response(ErrorResponse.of(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthorized(AuthenticationException exception) {
        return response(ErrorResponse.of(ErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        log.error("Unhandled server error", exception);
        return response(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ErrorResponse> response(ErrorResponse body) {
        return response(body, org.springframework.http.HttpHeaders.EMPTY);
    }

    private ResponseEntity<ErrorResponse> response(
            ErrorResponse body, org.springframework.http.HttpHeaders headers) {
        return ResponseEntity.status(body.status())
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private ResponseEntity<ErrorResponse> invalidFields(Map<String, String> fields) {
        return response(
                ErrorResponse.of(
                        ErrorCode.INVALID_INPUT, "Vui lòng kiểm tra thông tin đã nhập", fields));
    }

    private void collectErrors(Errors errors, Map<String, String> fields) {
        errors.getFieldErrors()
                .forEach(error -> addError(fields, error.getField(), validationMessage(error)));
        errors.getGlobalErrors()
                .forEach(error -> addError(fields, "_global", validationMessage(error)));
    }

    private void addError(Map<String, String> fields, String key, String message) {
        fields.merge(
                key, message, (first, next) -> first.equals(next) ? first : first + "; " + next);
    }

    private String validationMessage(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null
                ? "Giá trị không hợp lệ"
                : error.getDefaultMessage();
    }
}
