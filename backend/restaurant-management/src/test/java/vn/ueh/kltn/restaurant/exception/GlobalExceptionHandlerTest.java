package vn.ueh.kltn.restaurant.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @Test
    void unsupportedContentTypeIs415() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void wrongMethodIs405AndPreservesAllowHeader() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST")).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unacceptableResponseIs406WithJsonError() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN)
                        .content("{\"phone\":\"0900000001\",\"password\":\"Demo@123\"}"))
                .andExpect(status().isNotAcceptable()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void requiredQueryParameterIs400WithFieldError() throws Exception {
        mvc.perform(get("/query")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.guestCount").value("Vui lòng nhập tham số bắt buộc"));
    }

    @Test
    void invalidQueryTypeIs400WithFieldError() throws Exception {
        mvc.perform(get("/query").param("guestCount", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.guestCount").value("Giá trị tham số không hợp lệ"));
    }

    @Test
    void methodParameterValidationIs400WithFieldError() throws Exception {
        mvc.perform(get("/query").param("guestCount", "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.guestCount").value("Số khách phải lớn hơn 0"));
    }

    @Test
    void invalidReturnValueRemainsServerError() throws Exception {
        mvc.perform(get("/invalid-return")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void fieldAndClassLevelErrorsAreBothKept() throws Exception {
        mvc.perform(get("/global-errors")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Số điện thoại không hợp lệ"))
                .andExpect(jsonPath("$.fieldErrors._global").value("Khoảng thời gian không hợp lệ"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void disconnectedSseClientDoesNotReceiveJsonBody() throws Exception {
        mvc.perform(get("/disconnect")).andExpect(content().string(""));
    }

    @Test
    void dataIntegrityFailureMapsTo409() throws Exception {
        mvc.perform(get("/conflict")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @RestController
    static class ProbeController {
        @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
        LoginRequest login(@jakarta.validation.Valid @RequestBody LoginRequest request) { return request; }

        @GetMapping("/query")
        int query(@RequestParam @Min(value = 1, message = "Số khách phải lớn hơn 0") int guestCount) { return guestCount; }

        @GetMapping("/invalid-return")
        @NotNull String invalidReturn() { return null; }

        @GetMapping("/global-errors")
        void globalErrors() throws Exception {
            var errors = new BeanPropertyBindingResult(new LoginRequest("bad", "password"), "request");
            errors.rejectValue("phone", "phone.invalid", "Số điện thoại không hợp lệ");
            errors.reject("range.invalid", "Khoảng thời gian không hợp lệ");
            var parameter = new MethodParameter(ProbeController.class.getDeclaredMethod("login", LoginRequest.class), 0);
            throw new MethodArgumentNotValidException(parameter, errors);
        }

        @GetMapping("/disconnect")
        void disconnect() throws AsyncRequestNotUsableException { throw new AsyncRequestNotUsableException("client disconnected"); }

        @GetMapping("/conflict")
        void conflict() { throw new DataIntegrityViolationException("test constraint"); }
    }
}
