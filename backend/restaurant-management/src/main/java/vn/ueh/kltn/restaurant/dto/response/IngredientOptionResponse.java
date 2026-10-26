package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.IngredientStatus;

public record IngredientOptionResponse(
        Long id, String name, String unit, IngredientStatus status) {}
