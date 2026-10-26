package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lot metadata and received quantity, never a remaining lot balance. */
public record IngredientLotResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unit,
        String lotNumber,
        LocalDate expiryDate,
        BigDecimal initialQuantity,
        String status,
        Long daysRemaining) {}
