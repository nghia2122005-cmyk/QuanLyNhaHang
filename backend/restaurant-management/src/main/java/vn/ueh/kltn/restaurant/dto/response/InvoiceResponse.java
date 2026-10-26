package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
        Long id,
        Long sessionId,
        String tableName,
        String areaName,
        Long cashierId,
        String cashierName,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        Instant paidAt) {}
