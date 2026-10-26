package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.AvailableTableResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningTableResponse;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanTableResponse;
import vn.ueh.kltn.restaurant.dto.response.UpcomingReservationResponse;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;

public final class TableMapper {
    private TableMapper() {}

    public static DiningAreaResponse area(DiningArea area) {
        return new DiningAreaResponse(
                area.getId(), area.getName(), area.getCode(), area.getStatus());
    }

    public static DiningTableResponse managed(
            DiningTable table, Long sessionId, boolean liveReservation) {
        return new DiningTableResponse(
                table.getId(),
                table.getName(),
                table.getCode(),
                table.getCapacity(),
                table.getStatus(),
                table.getArea().getId(),
                table.getArea().getName(),
                table.getArea().getStatus(),
                sessionId,
                liveReservation);
    }

    public static AvailableTableResponse available(DiningTable table) {
        return new AvailableTableResponse(
                table.getId(),
                table.getName(),
                table.getCapacity(),
                table.getArea().getName(),
                table.getCode());
    }

    public static FloorPlanTableResponse floor(
            DiningTable t, Long sessionId, UpcomingReservationResponse upcoming) {
        return new FloorPlanTableResponse(
                t.getId(),
                t.getName(),
                t.getCapacity(),
                t.getStatus(),
                sessionId,
                upcoming,
                t.getCode());
    }
}
