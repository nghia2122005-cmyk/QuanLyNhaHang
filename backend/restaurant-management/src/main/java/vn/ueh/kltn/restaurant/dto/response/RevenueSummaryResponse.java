package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RevenueSummaryResponse(
        Instant from,
        Instant to,
        long invoiceCount,
        BigDecimal systemRevenue,
        List<PaymentMethodSummaryResponse> methods) {}
