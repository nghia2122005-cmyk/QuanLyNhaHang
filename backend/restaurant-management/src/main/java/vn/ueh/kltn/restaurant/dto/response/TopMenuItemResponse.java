package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;

public record TopMenuItemResponse(
        Long menuItemId, String menuItemName, long quantity, BigDecimal revenue) {}
