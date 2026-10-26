package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngredientLotAttentionResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unit,
        String lotNumber,
        LocalDate expiryDate,
        BigDecimal initialQuantity,
        String status,
        long daysRemaining) {}
