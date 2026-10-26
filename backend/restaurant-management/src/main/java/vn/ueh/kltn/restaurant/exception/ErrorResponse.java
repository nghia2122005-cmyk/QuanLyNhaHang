package vn.ueh.kltn.restaurant.exception;

import java.util.Map;

public record ErrorResponse(int status, String code, String message, Map<String, String> fieldErrors) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage(), Map.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message, Map.copyOf(fieldErrors));
    }
}
