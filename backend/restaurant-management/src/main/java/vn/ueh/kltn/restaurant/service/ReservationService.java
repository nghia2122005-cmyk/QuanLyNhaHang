package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.dto.response.AvailableTableResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.ReservationResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface ReservationService {
    PageResponse<ReservationResponse> list(
            LocalDate date,
            ReservationStatus status,
            String q,
            Long tableId,
            boolean unexpired,
            int page,
            int size);

    List<AvailableTableResponse> available(Instant startTime, Instant endTime, int guestCount);

    List<AvailableTableResponse> available(
            Instant startTime, Instant endTime, int guestCount, Long excludedId);

    ReservationResponse update(Long id, CreateReservationRequest request);

    ReservationResponse create(CreateReservationRequest request);

    ReservationResponse cancel(Long id);

    // Internal seed entry point; never accepts a creator from an HTTP request.
    ReservationResponse createForSeed(CreateReservationRequest request, Account creator);
}
