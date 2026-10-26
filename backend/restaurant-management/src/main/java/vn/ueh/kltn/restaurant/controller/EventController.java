package vn.ueh.kltn.restaurant.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.ueh.kltn.restaurant.realtime.SseHub;

@RestController
@RequiredArgsConstructor
public class EventController {
    private final SseHub hub;

    @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter events() {
        return hub.subscribe();
    }
}
