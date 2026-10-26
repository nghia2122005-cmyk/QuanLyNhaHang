package vn.ueh.kltn.restaurant.dto.response;

import java.time.Instant;
import java.util.List;

public record StocktakeResponse(
        Long id,
        Long createdById,
        String createdByName,
        Instant createdAt,
        List<StocktakeItemResponse> items) {}
