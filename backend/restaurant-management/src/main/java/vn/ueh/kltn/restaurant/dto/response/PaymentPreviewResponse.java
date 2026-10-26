package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.OrderItemStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentPreviewResponse(
        Long sessionId,
        Long tableId,
        String tableName,
        String areaName,
        Instant openedAt,
        BigDecimal provisionalTotal,
        boolean hasBlockingItems,
        List<BlockingItemResponse> blockingItems,
        List<OrderPreviewResponse> orders,
        String tableCode) {

    public record BlockingItemResponse(
            Long id,
            String menuItemName,
            int quantity,
            OrderItemStatus status,
            Instant createdAt) {}

    public record OrderPreviewResponse(
            Long orderId,
            Instant createdAt,
            String createdByName,
            List<OrderItemPreviewResponse> items) {}

    public record OrderItemPreviewResponse(
            Long id,
            Long menuItemId,
            String menuItemName,
            int quantity,
            BigDecimal unitPriceAtOrder,
            BigDecimal subtotal,
            OrderItemStatus status,
            String note,
            String unavailableReason) {}
}
