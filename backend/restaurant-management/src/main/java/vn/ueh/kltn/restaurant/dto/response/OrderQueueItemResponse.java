package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.OrderItemStatus;

import java.time.Instant;

public record OrderQueueItemResponse(
        Long id,
        Long sessionId,
        Long orderId,
        Long tableId,
        String tableName,
        String menuItemName,
        int quantity,
        String note,
        Instant orderedAt,
        OrderItemStatus status,
        String unavailableReason,
        String tableCode) {}
