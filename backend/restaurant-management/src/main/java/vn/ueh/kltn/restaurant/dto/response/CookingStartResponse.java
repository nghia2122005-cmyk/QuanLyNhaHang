package vn.ueh.kltn.restaurant.dto.response;

import java.util.List;

public record CookingStartResponse(
        boolean started,
        OrderItemResponse item,
        List<IngredientAvailabilityResponse> ingredients) {}
