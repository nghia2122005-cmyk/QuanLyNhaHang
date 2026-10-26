package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.ReservationResponse;
import vn.ueh.kltn.restaurant.dto.response.UpcomingReservationResponse;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;

import java.time.Instant;

public final class ReservationMapper {
    private ReservationMapper() {}

    public static ReservationResponse toResponse(Reservation r, Instant now) {
        return new ReservationResponse(
                r.getId(),
                r.getTable().getId(),
                r.getTable().getName(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getGuestCount(),
                r.getStartTime(),
                r.getEndTime(),
                r.getNote(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getCreatedBy().getId(),
                r.getStatus() == ReservationStatus.CONFIRMED && r.getEndTime().isBefore(now),
                r.getTable().getCode());
    }

    public static UpcomingReservationResponse upcoming(Reservation r) {
        return new UpcomingReservationResponse(
                r.getId(),
                r.getCustomerName(),
                r.getGuestCount(),
                r.getStartTime(),
                r.getEndTime());
    }
}
