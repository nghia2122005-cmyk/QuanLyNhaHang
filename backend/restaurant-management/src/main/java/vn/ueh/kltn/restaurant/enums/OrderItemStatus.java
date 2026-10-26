package vn.ueh.kltn.restaurant.enums;

import java.util.Set;

public enum OrderItemStatus {
    PENDING,
    COOKING,
    READY,
    SERVED,
    UNAVAILABLE,
    CANCELLED;

    public boolean blocksPayment() {
        return this == PENDING || this == COOKING || this == READY;
    }

    public static final Set<OrderItemStatus> NON_BILLABLE = Set.of(CANCELLED, UNAVAILABLE);
}
