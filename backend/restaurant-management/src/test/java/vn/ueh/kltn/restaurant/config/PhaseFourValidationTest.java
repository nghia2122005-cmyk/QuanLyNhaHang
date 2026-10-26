package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static vn.ueh.kltn.restaurant.TestFixtures.NOW;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.ueh.kltn.restaurant.dto.request.CreateReconciliationRequest;

import java.math.BigDecimal;

class PhaseFourValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.1", "100.0", "100000000000000"})
    void rejectsInvalidAmount(String amount) {
        assertFalse(
                validator
                        .validate(
                                new CreateReconciliationRequest(
                                        NOW, NOW.plusSeconds(1), new BigDecimal(amount), null))
                        .isEmpty());
    }

    @Test
    void acceptsZeroOptionalReasonAndExactPrecisionLimit() {
        assertTrue(
                validator
                        .validate(
                                new CreateReconciliationRequest(
                                        NOW, NOW.plusSeconds(1), BigDecimal.ZERO, null))
                        .isEmpty());
        assertTrue(
                validator
                        .validate(
                                new CreateReconciliationRequest(
                                        NOW,
                                        NOW.plusSeconds(1),
                                        new BigDecimal("99999999999999"),
                                        "x".repeat(500)))
                        .isEmpty());
    }

    @Test
    void rejectsMissingFieldsAndLongReason() {
        assertFalse(
                validator
                        .validate(
                                new CreateReconciliationRequest(null, null, null, "x".repeat(501)))
                        .isEmpty());
    }
}
