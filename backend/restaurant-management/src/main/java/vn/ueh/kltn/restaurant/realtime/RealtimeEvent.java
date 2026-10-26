package vn.ueh.kltn.restaurant.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RealtimeEvent(Type type, Long sessionId) {
    public enum Type {
        FLOOR_PLAN,
        RESERVATION,
        ORDER
    }
}
