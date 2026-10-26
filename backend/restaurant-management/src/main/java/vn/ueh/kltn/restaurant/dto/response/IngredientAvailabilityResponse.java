package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record IngredientAvailabilityResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal currentStock,
        BigDecimal reservedStock,
        BigDecimal availableStock,
        BigDecimal requiredQuantity,
        BigDecimal projectedStock,
        BigDecimal minimumStock,
        BigDecimal shortage) {}
