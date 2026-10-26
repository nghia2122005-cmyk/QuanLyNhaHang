package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;

public record DiningTableResponse(
        Long id,
        String name,
        String code,
        int capacity,
        TableStatus status,
        Long areaId,
        String areaName,
        AreaStatus areaStatus,
        Long activeSessionId,
        boolean hasLiveReservation) {}
