package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SessionDetailResponse(
        Long sessionId,
        Long tableId,
        String tableName,
        String areaName,
        ReservationResponse reservation,
        Instant openedAt,
        Instant closedAt,
        SessionStatus status,
        List<FoodOrderResponse> orders,
        BigDecimal total,
        String tableCode) {}
