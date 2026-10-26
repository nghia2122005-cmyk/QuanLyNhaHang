package vn.ueh.kltn.restaurant.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static vn.ueh.kltn.restaurant.TestFixtures.item;
import static vn.ueh.kltn.restaurant.TestFixtures.menu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;

class OrderItemTest {
    @ParameterizedTest
    @EnumSource(OrderItemStatus.class)
    void billingIncludesPendingCookingReadyAndServedButExcludesTerminalFailures(
            OrderItemStatus status) {
        OrderItem item = item();
        ReflectionTestUtils.setField(item, "status", status);
        BigDecimal expected =
                switch (status) {
                    case CANCELLED, UNAVAILABLE -> BigDecimal.ZERO;
                    case PENDING, COOKING, READY, SERVED ->
                            item.getUnitPriceAtOrder()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()));
                };
        assertEquals(expected, item.calculateSubtotal());
    }

    @Test
    void snapshotPriceAndSubtotalIgnoreLaterMenuPrice() {
        MenuItem menu = menu();
        var item = new OrderItem(menu, 2, null);
        menu.changePrice(new BigDecimal("75000"));
        assertEquals(new BigDecimal("50000"), item.getUnitPriceAtOrder());
        assertEquals(new BigDecimal("100000"), item.calculateSubtotal());
        item.cancel();
        assertEquals(BigDecimal.ZERO, item.calculateSubtotal());
    }

    @Test
    void unavailableRequiresReasonAndIsExcludedFromTotal() {
        OrderItem item = item();
        assertThrows(BusinessException.class, () -> item.markUnavailable("   "));
        assertThrows(BusinessException.class, () -> item.markUnavailable(null));
        item.markUnavailable("  Hết nguyên liệu  ");
        assertEquals("Hết nguyên liệu", item.getUnavailableReason());
        assertEquals(BigDecimal.ZERO, item.calculateSubtotal());
    }

    @Test
    void nonpositiveQuantityIsRejected() {
        assertThrows(BusinessException.class, () -> new OrderItem(menu(), 0, null));
        assertThrows(BusinessException.class, () -> new OrderItem(menu(), -1, null));
    }

    @Test
    void happyPathAndRepeatedTargetsAreIdempotent() {
        OrderItem item = item();
        item.startCooking();
        item.startCooking();
        item.markReady();
        item.markReady();
        item.markServed();
        item.markServed();
        item.markReady();
        assertEquals(OrderItemStatus.SERVED, item.getStatus());
    }

    @Test
    void repeatedTerminalActionsDoNotAlterReasonOrState() {
        OrderItem cancelled = item();
        cancelled.cancel();
        cancelled.cancel();
        OrderItem unavailable = item();
        unavailable.markUnavailable("Hết món");
        unavailable.markUnavailable("Lý do khác");
        assertEquals(OrderItemStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Hết món", unavailable.getUnavailableReason());
    }

    static Stream<Arguments> invalidTransitions() {
        Consumer<OrderItem> start = OrderItem::startCooking;
        Consumer<OrderItem> ready = OrderItem::markReady;
        Consumer<OrderItem> served = OrderItem::markServed;
        Consumer<OrderItem> cancel = OrderItem::cancel;
        Consumer<OrderItem> unavailable = item -> item.markUnavailable("Hết món");
        return Stream.of(
                Arguments.of(OrderItemStatus.PENDING, ready),
                Arguments.of(OrderItemStatus.PENDING, served),
                Arguments.of(OrderItemStatus.COOKING, served),
                Arguments.of(OrderItemStatus.COOKING, cancel),
                Arguments.of(OrderItemStatus.READY, start),
                Arguments.of(OrderItemStatus.READY, cancel),
                Arguments.of(OrderItemStatus.READY, unavailable),
                Arguments.of(OrderItemStatus.SERVED, start),
                Arguments.of(OrderItemStatus.SERVED, cancel),
                Arguments.of(OrderItemStatus.SERVED, unavailable),
                Arguments.of(OrderItemStatus.UNAVAILABLE, start),
                Arguments.of(OrderItemStatus.UNAVAILABLE, ready),
                Arguments.of(OrderItemStatus.UNAVAILABLE, served),
                Arguments.of(OrderItemStatus.UNAVAILABLE, cancel),
                Arguments.of(OrderItemStatus.CANCELLED, start),
                Arguments.of(OrderItemStatus.CANCELLED, ready),
                Arguments.of(OrderItemStatus.CANCELLED, served),
                Arguments.of(OrderItemStatus.CANCELLED, unavailable));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void guardsRejectInvalidTransitions(OrderItemStatus status, Consumer<OrderItem> transition) {
        OrderItem item = item();
        ReflectionTestUtils.setField(item, "status", status);
        assertThrows(BusinessException.class, () -> transition.accept(item));
        assertEquals(status, item.getStatus());
    }
}
