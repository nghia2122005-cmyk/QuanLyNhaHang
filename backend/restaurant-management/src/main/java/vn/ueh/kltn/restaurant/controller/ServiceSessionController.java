package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.OpenSessionRequest;
import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.dto.response.SessionDetailResponse;
import vn.ueh.kltn.restaurant.service.ServiceSessionService;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ServiceSessionController {
    private final ServiceSessionService sessions;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAITER','MANAGER')")
    public OpenSessionResponse open(@Valid @RequestBody OpenSessionRequest request) {
        return sessions.open(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','CASHIER')")
    public SessionDetailResponse detail(@PathVariable Long id) {
        return sessions.detail(id);
    }

    @PostMapping("/{id}/orders")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER')")
    public FoodOrderResponse order(
            @PathVariable Long id, @Valid @RequestBody CreateOrderRequest request) {
        return sessions.order(id, request);
    }

    @PatchMapping("/items/{orderItemId}/cancel")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER')")
    public OrderItemResponse cancel(@PathVariable Long orderItemId) {
        return sessions.cancel(orderItemId);
    }

    @PatchMapping("/items/{orderItemId}/serve")
    @PreAuthorize("hasRole('WAITER')")
    public OrderItemResponse serve(@PathVariable Long orderItemId) {
        return sessions.serve(orderItemId);
    }

    @PatchMapping("/items/{orderItemId}/acknowledge-unavailable")
    @PreAuthorize("hasRole('WAITER')")
    public OrderItemResponse acknowledgeUnavailable(@PathVariable Long orderItemId) {
        return sessions.acknowledgeUnavailable(orderItemId);
    }

    @GetMapping("/items/awaiting-service")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','CASHIER','OWNER')")
    public List<OrderQueueItemResponse> awaitingService() {
        return sessions.awaitingService();
    }
}
