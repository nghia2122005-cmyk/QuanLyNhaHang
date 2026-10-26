package vn.ueh.kltn.restaurant.dto.response;

import java.time.Instant;

public record UpcomingReservationResponse(
        Long id, String customerName, int guestCount, Instant startTime, Instant endTime) {}
