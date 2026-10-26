package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long tableId,
        String tableName,
        String customerName,
        String customerPhone,
        int guestCount,
        Instant startTime,
        Instant endTime,
        String note,
        ReservationStatus status,
        Instant createdAt,
        Long createdById,
        boolean overdue,
        String tableCode) {}
