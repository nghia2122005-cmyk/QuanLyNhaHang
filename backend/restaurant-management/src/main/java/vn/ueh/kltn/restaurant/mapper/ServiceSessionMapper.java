package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.OpenSessionResponse;
import vn.ueh.kltn.restaurant.entity.ServiceSession;

public final class ServiceSessionMapper {
    private ServiceSessionMapper() {}

    public static OpenSessionResponse toResponse(ServiceSession s) {
        return new OpenSessionResponse(
                s.getId(),
                s.getTable().getId(),
                s.getReservation() == null ? null : s.getReservation().getId(),
                s.getOpenedAt(),
                s.getStatus());
    }
}
