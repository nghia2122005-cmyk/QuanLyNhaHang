package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record StocktakeItemResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal systemQuantity,
        BigDecimal actualQuantity,
        BigDecimal difference,
        String note) {}
