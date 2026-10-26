package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.MenuItemStatus;

import java.math.BigDecimal;

public record MenuItemResponse(Long id, String name, BigDecimal price, MenuItemStatus status) {}
