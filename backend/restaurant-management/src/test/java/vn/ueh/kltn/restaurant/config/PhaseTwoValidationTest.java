package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.OrderLineRequest;
import vn.ueh.kltn.restaurant.dto.request.UnavailableRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PhaseTwoValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nestedItemsRequiredAndPositiveWithColumnLengthLimits() {
        assertEquals(1, validator.validate(new CreateOrderRequest(null)).size());
        assertEquals(1, validator.validate(new CreateOrderRequest(List.of())).size());
        assertEquals(
                1,
                validator
                        .validate(new CreateOrderRequest(Arrays.asList((OrderLineRequest) null)))
                        .size());
        assertEquals(
                3,
                validator
                        .validate(
                                new CreateOrderRequest(
                                        List.of(new OrderLineRequest(null, 0, "x".repeat(501)))))
                        .size());
        assertEquals(1, validator.validate(new OrderLineRequest(1L, null, null)).size());
        assertTrue(
                validator
                        .validate(
                                new CreateOrderRequest(
                                        List.of(new OrderLineRequest(1L, 1, "x".repeat(500)))))
                        .isEmpty());
    }

    @Test
    void unavailableReasonIsMandatoryAndBounded() {
        assertEquals(1, validator.validate(new UnavailableRequest(null)).size());
        assertEquals(1, validator.validate(new UnavailableRequest("   ")).size());
        assertEquals(1, validator.validate(new UnavailableRequest("x".repeat(501))).size());
        assertTrue(validator.validate(new UnavailableRequest("Hết nguyên liệu")).isEmpty());
    }

    @Test
    void rejectsMoreThanNinetyNinePortionsAndFiftyLinesWithFieldPaths() {
        assertEquals(1, validator.validate(new OrderLineRequest(1L, 100, null)).size());
        assertEquals(
                1, validator.validate(new OrderLineRequest(1L, Integer.MAX_VALUE, null)).size());
        assertTrue(validator.validate(new OrderLineRequest(1L, 99, null)).isEmpty());
        var fifty =
                new CreateOrderRequest(Collections.nCopies(50, new OrderLineRequest(1L, 99, null)));
        assertTrue(validator.validate(fifty).isEmpty());
        var tooMany =
                new CreateOrderRequest(Collections.nCopies(51, new OrderLineRequest(1L, 1, null)));
        assertEquals(
                "items",
                validator.validate(tooMany).iterator().next().getPropertyPath().toString());
        var quantity = new CreateOrderRequest(List.of(new OrderLineRequest(1L, 100, null)));
        assertEquals(
                "items[0].quantity",
                validator.validate(quantity).iterator().next().getPropertyPath().toString());
    }
}
