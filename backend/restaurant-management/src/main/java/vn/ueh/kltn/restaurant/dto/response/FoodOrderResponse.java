package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FoodOrderResponse(
        Long id,
        Long createdById,
        String createdByName,
        Instant createdAt,
        List<OrderItemResponse> items,
        BigDecimal total) {}
