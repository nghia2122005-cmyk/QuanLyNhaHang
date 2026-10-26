package vn.ueh.kltn.restaurant.exception;

import vn.ueh.kltn.restaurant.dto.response.IngredientAvailabilityResponse;

import java.util.List;

public class InventoryAvailabilityException extends BusinessException {
    private final List<IngredientAvailabilityResponse> ingredients;

    public InventoryAvailabilityException(List<IngredientAvailabilityResponse> ingredients) {
        super(ErrorCode.INSUFFICIENT_STOCK, "Không đủ nguyên vật liệu khả dụng để chế biến");
        this.ingredients = List.copyOf(ingredients);
    }

    public List<IngredientAvailabilityResponse> getIngredients() {
        return ingredients;
    }
}
