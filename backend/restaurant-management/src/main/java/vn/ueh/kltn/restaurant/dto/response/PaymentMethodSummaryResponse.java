package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodSummaryResponse(
        PaymentMethod paymentMethod, long invoiceCount, BigDecimal revenue) {}
