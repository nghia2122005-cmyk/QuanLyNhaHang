package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.dto.request.StartCookingRequest;
import vn.ueh.kltn.restaurant.dto.request.UnavailableRequest;
import vn.ueh.kltn.restaurant.dto.response.CookingStartResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.service.KitchenService;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@PreAuthorize("hasAnyRole('KITCHEN_STAFF','HEAD_CHEF')")
@RequiredArgsConstructor
public class KitchenController {
    private final KitchenService kitchen;

    @GetMapping("/queue")
    public List<OrderQueueItemResponse> queue() {
        return kitchen.queue();
    }

    @GetMapping("/orders/{id}")
    public List<OrderQueueItemResponse> batch(@PathVariable Long id) {
        return kitchen.batch(id);
    }

    @PatchMapping("/items/{id}/start")
    public CookingStartResponse start(
            @PathVariable Long id, @RequestBody(required = false) StartCookingRequest request) {
        return kitchen.start(id, request != null && Boolean.TRUE.equals(request.confirmLowStock()));
    }

    @PatchMapping("/items/{id}/unavailable")
    public OrderItemResponse unavailable(
            @PathVariable Long id, @Valid @RequestBody UnavailableRequest request) {
        return kitchen.unavailable(id, request.reason());
    }

    @PatchMapping("/items/{id}/complete")
    public OrderItemResponse complete(@PathVariable Long id) {
        return kitchen.complete(id);
    }
}
