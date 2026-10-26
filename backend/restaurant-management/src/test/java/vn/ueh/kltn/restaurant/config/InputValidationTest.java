package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import vn.ueh.kltn.restaurant.dto.request.CreateAccountRequest;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;
import vn.ueh.kltn.restaurant.enums.Role;

import java.util.Set;

class InputValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void accountPasswordRespectsUtf8ByteLimitAndColumnLengths() {
        var valid = new CreateAccountRequest("Nhân viên", "0900000001", "Demo@123", Role.WAITER);
        assertTrue(validator.validate(valid).isEmpty());
        var tooLong =
                new CreateAccountRequest(
                        "x".repeat(101), "0900000001", "ế".repeat(25), Role.WAITER);
        Set<ConstraintViolation<CreateAccountRequest>> errors = validator.validate(tooLong);
        assertTrue(
                errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("password")));
        assertTrue(
                errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    void blankPhoneHasOnlyRequiredMessage() {
        for (String phone : new String[] {"", "  "}) {
            Set<ConstraintViolation<LoginRequest>> errors =
                    validator.validate(new LoginRequest(phone, "Demo@123"));
            assertEquals(1, errors.size());
            assertEquals("Vui lòng nhập số điện thoại", errors.iterator().next().getMessage());
        }
    }

    @Test
    void phoneFormatAndBlankPasswordAreValidated() {
        assertEquals(2, validator.validate(new LoginRequest("123", "")).size());
        assertTrue(validator.validate(new LoginRequest("0900000001", "Demo@123")).isEmpty());
    }

    @Test
    void placeholderShortSecretAndNonpositiveExpiryAreRejected() {
        assertFalse(
                validator
                        .validate(
                                new AppProperties.Jwt("<random-secret-at-least-32-characters>", 12))
                        .isEmpty());
        assertFalse(validator.validate(new AppProperties.Jwt("short", 12)).isEmpty());
        assertFalse(validator.validate(new AppProperties.Jwt("a".repeat(40), 0)).isEmpty());
        assertTrue(validator.validate(new AppProperties.Jwt("a".repeat(40), 12)).isEmpty());
    }
}
