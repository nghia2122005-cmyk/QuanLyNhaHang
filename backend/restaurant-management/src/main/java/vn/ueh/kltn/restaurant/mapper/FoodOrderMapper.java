package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.OrderItem;

import java.util.Comparator;

public final class FoodOrderMapper {
    private FoodOrderMapper() {}

    public static OrderItemResponse item(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getMenuItem().getName(),
                item.getQuantity(),
                item.getNote(),
                item.getStatus(),
                item.getUnavailableReason(),
                item.getUnitPriceAtOrder(),
                item.calculateSubtotal());
    }

    public static FoodOrderResponse order(FoodOrder order) {
        return new FoodOrderResponse(
                order.getId(),
                order.getCreatedBy().getId(),
                order.getCreatedBy().getFullName(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getId))
                        .map(FoodOrderMapper::item)
                        .toList(),
                order.calculateTotal());
    }

    public static OrderQueueItemResponse queueItem(OrderItem item) {
        FoodOrder order = item.getOrder();
        return new OrderQueueItemResponse(
                item.getId(),
                order.getSession().getId(),
                order.getId(),
                order.getSession().getTable().getId(),
                order.getSession().getTable().getName(),
                item.getMenuItem().getName(),
                item.getQuantity(),
                item.getNote(),
                order.getCreatedAt(),
                item.getStatus(),
                item.getUnavailableReason(),
                order.getSession().getTable().getCode());
    }
}
