package vn.ueh.kltn.restaurant.dto.response;

import java.time.Instant;
import java.util.List;

public record StockReceiptResponse(
        Long id,
        Long createdById,
        String createdByName,
        Instant createdAt,
        List<StockReceiptItemResponse> items) {}
