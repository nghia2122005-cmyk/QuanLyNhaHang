package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.item;
import static vn.ueh.kltn.restaurant.TestFixtures.menu;
import static vn.ueh.kltn.restaurant.TestFixtures.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.OrderLineRequest;
import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.dto.response.SessionDetailResponse;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.FoodOrderRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

class SessionOrderServiceImplTest {
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final FoodOrderRepository orders = mock(FoodOrderRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final MenuItemRepository menu = mock(MenuItemRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ServiceSessionServiceImpl service =
            new ServiceSessionServiceImpl(
                    mock(DiningTableRepository.class),
                    mock(ReservationRepository.class),
                    sessions,
                    accounts,
                    user,
                    CLOCK,
                    orders,
                    items,
                    menu,
                    events);
    private ServiceSession session;
    private MenuItem dish;

    @BeforeEach
    void setup() {
        session = session();
        dish = menu();
        when(sessions.findLockedById(7L)).thenReturn(Optional.of(session));
        when(items.findSessionIdById(9L)).thenReturn(Optional.of(7L));
        when(menu.findAllById(List.of(1L))).thenReturn(List.of(dish));
        when(user.accountId()).thenReturn(2L);
        when(accounts.getReferenceById(2L)).thenReturn(account());
        var ids = new AtomicLong(10);
        when(orders.saveAndFlush(any()))
                .thenAnswer(
                        invocation -> {
                            FoodOrder order = invocation.getArgument(0);
                            id(order, ids.getAndIncrement());
                            order.getItems().forEach(item -> id(item, ids.getAndIncrement()));
                            return order;
                        });
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(List.of(new OrderLineRequest(1L, 2, "[QA] Không hành")));
    }

    @Test
    void additionalOrderIsNewBatchSameSessionAndSnapshotsNewPrice() {
        FoodOrderResponse first = service.order(7L, request());
        dish.changePrice(new BigDecimal("60000"));
        FoodOrderResponse second = service.order(7L, request());
        assertNotEquals(first.id(), second.id());
        assertEquals(new BigDecimal("100000"), first.total());
        assertEquals(new BigDecimal("120000"), second.total());
        assertEquals(2L, first.createdById());
        assertEquals(NOW, first.createdAt());
        assertEquals(OrderItemStatus.PENDING, first.items().getFirst().status());
        verify(sessions, times(2)).findLockedById(7L);
        verify(events, times(2)).publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
    }

    @Test
    void closedSessionRejectsBeforeMenuOrSave() {
        session.close(NOW);
        assertThrows(BusinessException.class, () -> service.order(7L, request()));
        verifyNoInteractions(menu, orders, events);
    }

    @Test
    void discontinuedOrMissingMenuRejectsWholeBatch() {
        dish.discontinue();
        assertThrows(BusinessException.class, () -> service.order(7L, request()));
        when(menu.findAllById(List.of(1L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.order(7L, request()));
        verifyNoInteractions(orders, events);
    }

    @Test
    void emptyOrNonpositiveOrderIsRejected() {
        assertThrows(
                BusinessException.class,
                () -> service.order(7L, new CreateOrderRequest(List.of())));
        assertThrows(
                BusinessException.class,
                () ->
                        service.order(
                                7L,
                                new CreateOrderRequest(
                                        List.of(new OrderLineRequest(1L, 0, null)))));
        verifyNoInteractions(orders, events);
    }

    @Test
    void detailTotalsAllBatchesFromSnapshotAndExcludesCancelledUnavailable() {
        OrderItem pending = item();
        OrderItem cancelled = id(new OrderItem(dish, 2, null), 11L);
        cancelled.cancel();
        OrderItem unavailable = id(new OrderItem(dish, 4, null), 12L);
        unavailable.markUnavailable("Hết món");
        pending.getOrder().addItem(cancelled);
        pending.getOrder().addItem(unavailable);
        var second = id(new FoodOrder(session, account(), NOW), 20L);
        second.addItem(id(new OrderItem(dish, 1, null), 21L));
        dish.changePrice(new BigDecimal("90000"));
        when(sessions.findDetailById(7L)).thenReturn(Optional.of(session));
        when(orders.findDetailsBySessionId(7L)).thenReturn(List.of(pending.getOrder(), second));
        SessionDetailResponse detail = service.detail(7L);
        assertEquals(new BigDecimal("200000"), detail.total());
        assertEquals(2, detail.orders().size());
        assertEquals(BigDecimal.ZERO, detail.orders().getFirst().items().get(1).subtotal());
    }

    @Test
    void cancelAndServeLockAndDoNotPublishForRepeatedTargetOrInvalidTransition() {
        OrderItem item = item();
        when(items.findLockedById(9L)).thenReturn(Optional.of(item));
        assertThrows(BusinessException.class, () -> service.serve(9L));
        service.cancel(9L);
        service.cancel(9L);
        OrderItem ready = item();
        ready.startCooking();
        assertThrows(
                BusinessException.class,
                () -> {
                    when(items.findLockedById(9L)).thenReturn(Optional.of(ready));
                    service.cancel(9L);
                });
        ready.markReady();
        service.serve(9L);
        service.serve(9L);
        verify(events, times(2)).publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
    }

    @Test
    void awaitingOnlyReadsReadyAndUnavailableOfActiveSessions() {
        OrderItem item = item();
        item.markUnavailable("Hết món");
        when(items.findQueue(
                        SessionStatus.ACTIVE,
                        Set.of(OrderItemStatus.READY, OrderItemStatus.UNAVAILABLE)))
                .thenReturn(List.of(item));
        assertEquals("Hết món", service.awaitingService().getFirst().unavailableReason());
    }

    @Test
    void missingSessionAndItemsReturnBusinessErrors() {
        assertThrows(BusinessException.class, () -> service.detail(99L));
        assertThrows(BusinessException.class, () -> service.order(99L, request()));
        assertThrows(BusinessException.class, () -> service.cancel(99L));
        assertThrows(BusinessException.class, () -> service.serve(99L));
        verifyNoInteractions(events);
    }

    @Test
    void rejectsOversizedBatchesAndQuantitiesBeforeReadingMenuOrSaving() {
        assertThrows(
                BusinessException.class,
                () ->
                        service.order(
                                7L,
                                new CreateOrderRequest(
                                        Collections.nCopies(
                                                51, new OrderLineRequest(1L, 1, null)))));
        for (int quantity : new int[] {100, Integer.MAX_VALUE}) {
            assertThrows(
                    BusinessException.class,
                    () ->
                            service.order(
                                    7L,
                                    new CreateOrderRequest(
                                            List.of(new OrderLineRequest(1L, quantity, null)))));
        }
        verifyNoInteractions(menu, orders, events);
    }

    @Test
    void acceptsFiftyLinesOfNinetyNinePortions() {
        FoodOrderResponse result =
                service.order(
                        7L,
                        new CreateOrderRequest(
                                Collections.nCopies(50, new OrderLineRequest(1L, 99, null))));
        assertEquals(50, result.items().size());
        assertEquals(99, result.items().getFirst().quantity());
    }

    @ParameterizedTest
    @CsvSource({"cancel,PENDING", "cancel,CANCELLED", "serve,READY", "serve,SERVED"})
    void closedSessionRejectsTransitionsIncludingNoOps(String action, OrderItemStatus status) {
        OrderItem item = item();
        ReflectionTestUtils.setField(item, "status", status);
        when(items.findLockedById(9L)).thenReturn(Optional.of(item));
        session.close(NOW);
        assertThrows(BusinessException.class, () -> mutate(action));
        assertEquals(status, item.getStatus());
        verifyNoInteractions(events);
    }

    @ParameterizedTest
    @CsvSource({"cancel,PENDING", "serve,READY"})
    void mutationsLockSessionBeforeItem(String action, OrderItemStatus status) {
        OrderItem item = item();
        ReflectionTestUtils.setField(item, "status", status);
        when(items.findLockedById(9L)).thenReturn(Optional.of(item));
        mutate(action);
        InOrder locks = inOrder(items, sessions);
        locks.verify(items).findSessionIdById(9L);
        locks.verify(sessions).findLockedById(7L);
        locks.verify(items).findLockedById(9L);
        verify(items, never()).findById(9L);
    }

    private void mutate(String action) {
        if (action.equals("cancel")) service.cancel(9L);
        else service.serve(9L);
    }
}
