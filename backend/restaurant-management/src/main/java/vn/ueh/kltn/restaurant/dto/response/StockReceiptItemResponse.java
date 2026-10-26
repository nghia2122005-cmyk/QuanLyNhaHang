package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockReceiptItemResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unit,
        Long lotId,
        String lotNumber,
        LocalDate expiryDate,
        BigDecimal quantity) {}
