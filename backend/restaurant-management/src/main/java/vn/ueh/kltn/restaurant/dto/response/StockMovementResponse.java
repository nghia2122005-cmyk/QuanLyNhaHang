package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockMovementResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unit,
        Long lotId,
        String lotNumber,
        LocalDate expiryDate,
        MovementType movementType,
        BigDecimal quantityChange,
        String reason,
        Long performedById,
        String performedByName,
        Instant occurredAt,
        String source) {}
