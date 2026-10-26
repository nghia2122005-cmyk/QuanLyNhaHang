package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.SessionStatus;

import java.time.Instant;

public record OpenSessionResponse(
        Long sessionId, Long tableId, Long reservationId, Instant openedAt, SessionStatus status) {}
