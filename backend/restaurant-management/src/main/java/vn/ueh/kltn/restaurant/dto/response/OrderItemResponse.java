package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.OrderItemStatus;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        int quantity,
        String note,
        OrderItemStatus status,
        String unavailableReason,
        BigDecimal unitPriceAtOrder,
        BigDecimal subtotal) {}
