package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record RecipeLineResponse(
        Long ingredientId, String ingredientName, String unit, BigDecimal quantityPerServing) {}
