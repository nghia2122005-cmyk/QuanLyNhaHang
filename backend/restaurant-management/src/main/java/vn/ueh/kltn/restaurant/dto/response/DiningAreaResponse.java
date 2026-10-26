package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.AreaStatus;

public record DiningAreaResponse(Long id, String name, String code, AreaStatus status) {}
