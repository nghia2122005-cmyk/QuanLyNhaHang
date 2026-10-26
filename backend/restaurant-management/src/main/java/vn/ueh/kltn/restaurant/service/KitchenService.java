package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.response.CookingStartResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;

import java.util.List;

public interface KitchenService {
    List<OrderQueueItemResponse> queue();

    List<OrderQueueItemResponse> batch(Long orderId);

    CookingStartResponse start(Long id, boolean confirmLowStock);

    OrderItemResponse unavailable(Long id, String reason);

    OrderItemResponse complete(Long id);
}
