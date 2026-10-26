package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.OpenSessionRequest;
import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.dto.response.SessionDetailResponse;

import java.util.List;

public interface ServiceSessionService {
    OpenSessionResponse open(OpenSessionRequest request);

    SessionDetailResponse detail(Long id);

    FoodOrderResponse order(Long id, CreateOrderRequest request);

    OrderItemResponse cancel(Long id);

    OrderItemResponse serve(Long id);

    List<OrderQueueItemResponse> awaitingService();

    OrderItemResponse acknowledgeUnavailable(Long id);
}
