package vn.ueh.kltn.restaurant.dto.response;

import java.util.List;

public record FloorPlanAreaResponse(
        Long id, String name, List<FloorPlanTableResponse> tables, String code) {}
