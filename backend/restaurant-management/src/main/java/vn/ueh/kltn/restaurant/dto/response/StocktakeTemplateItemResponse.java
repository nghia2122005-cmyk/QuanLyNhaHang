package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record StocktakeTemplateItemResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal currentStock) {}
