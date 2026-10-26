package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record DailyRevenueResponse(
        String date,
        long invoiceCount,
        BigDecimal revenue,
        BigDecimal cashRevenue,
        BigDecimal bankTransferRevenue) {}
