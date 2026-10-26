package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.IngredientOptionResponse;
import vn.ueh.kltn.restaurant.entity.Ingredient;

public final class IngredientMapper {
    private IngredientMapper() {}

    public static IngredientOptionResponse toOption(Ingredient i) {
        return new IngredientOptionResponse(i.getId(), i.getName(), i.getUnit(), i.getStatus());
    }
}
