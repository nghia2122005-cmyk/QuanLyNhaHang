package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.TableStatus;

public record FloorPlanTableResponse(
        Long id,
        String name,
        int capacity,
        TableStatus status,
        Long activeSessionId,
        UpcomingReservationResponse upcomingReservation,
        String code) {}
