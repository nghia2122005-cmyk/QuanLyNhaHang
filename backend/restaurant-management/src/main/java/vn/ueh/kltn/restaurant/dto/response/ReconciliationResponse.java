package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ReconciliationResponse(
        Long id,
        Long cashierId,
        String cashierName,
        Long managerId,
        String managerName,
        Instant startTime,
        Instant endTime,
        BigDecimal systemRevenue,
        BigDecimal actualAmount,
        BigDecimal difference,
        String differenceReason,
        Instant reconciledAt,
        boolean confirmed) {}
