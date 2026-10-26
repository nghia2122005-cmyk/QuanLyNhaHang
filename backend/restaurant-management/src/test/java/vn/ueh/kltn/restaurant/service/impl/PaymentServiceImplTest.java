package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.appProperties;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.menu;
import static vn.ueh.kltn.restaurant.TestFixtures.session;
import static vn.ueh.kltn.restaurant.TestFixtures.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import vn.ueh.kltn.restaurant.dto.request.PaySessionRequest;
import vn.ueh.kltn.restaurant.dto.response.InvoiceResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionPaymentResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentPreviewResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.Invoice;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.PaymentMethod;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.FoodOrderRepository;
import vn.ueh.kltn.restaurant.repository.InvoiceRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class PaymentServiceImplTest {

    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final FoodOrderRepository foodOrders = mock(FoodOrderRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new PaymentServiceImpl(
                        sessions,
                        foodOrders,
                        invoices,
                        accounts,
                        currentUser,
                        CLOCK,
                        events,
                        mock(OrderItemRepository.class),
                        appProperties("Asia/Ho_Chi_Minh"));
    }

    @Test
    void listOpenSessionsReturnsActiveSessionsWithProvisionalTotals() {
        DiningTable table = table(1L, 4);
        ServiceSession activeSession = id(new ServiceSession(table, null, account(), NOW), 10L);

        when(sessions.findByStatusWithTableAndArea(SessionStatus.ACTIVE))
                .thenReturn(List.of(activeSession));

        FoodOrder order = id(new FoodOrder(activeSession, account(), NOW), 20L);
        OrderItem item = id(new OrderItem(menu(), 2, "ít ớt"), 30L);
        item.startCooking();
        item.markReady();
        order.addItem(item);

        when(foodOrders.findDetailsBySessionIdIn(List.of(10L))).thenReturn(List.of(order));

        List<OpenSessionPaymentResponse> result = service.listOpenSessions();

        assertEquals(1, result.size());
        OpenSessionPaymentResponse resp = result.get(0);
        assertEquals(10L, resp.sessionId());
        assertEquals("T1", resp.tableName());
        assertEquals(new BigDecimal("100000"), resp.provisionalTotal());
        assertTrue(resp.hasBlockingItems());
        assertEquals(1, resp.totalItems());
    }

    @Test
    void previewSessionCalculatesSubtotalAndDetectsBlockingItems() {
        ServiceSession activeSession = session();
        when(sessions.findDetailById(activeSession.getId())).thenReturn(Optional.of(activeSession));

        FoodOrder order = id(new FoodOrder(activeSession, account(), NOW), 20L);

        // Item 1: PENDING (blocks payment)
        OrderItem pendingItem = id(new OrderItem(menu(), 1, null), 31L);
        order.addItem(pendingItem);

        // Item 2: READY (billable)
        MenuItem item2Menu = id(new MenuItem("Cơm tấm", new BigDecimal("40000")), 2L);
        OrderItem readyItem = id(new OrderItem(item2Menu, 2, null), 32L);
        readyItem.startCooking();
        readyItem.markReady();
        order.addItem(readyItem);

        // Item 3: CANCELLED (0đ)
        OrderItem cancelledItem = id(new OrderItem(menu(), 1, null), 33L);
        cancelledItem.cancel();
        order.addItem(cancelledItem);

        when(foodOrders.findDetailsBySessionId(activeSession.getId())).thenReturn(List.of(order));

        PaymentPreviewResponse preview = service.previewSession(activeSession.getId());

        assertEquals(activeSession.getId(), preview.sessionId());
        assertTrue(preview.hasBlockingItems());
        assertEquals(2, preview.blockingItems().size());
        assertEquals(pendingItem.getId(), preview.blockingItems().get(0).id());
        // Provisional total = 1 * 50000 (pending) + 2 * 40000 (ready) + 0 (cancelled) = 130000
        assertEquals(new BigDecimal("130000"), preview.provisionalTotal());
    }

    @Test
    void payLocksSessionBeforeLoadingItemsAndEnforcesLockOrder() {
        ServiceSession activeSession = session();
        activeSession.getTable().occupy();

        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(false);
        when(foodOrders.findDetailsBySessionId(activeSession.getId()))
                .thenReturn(Collections.emptyList());

        Account cashier = account();
        when(currentUser.accountId()).thenReturn(cashier.getId());
        when(accounts.findById(cashier.getId())).thenReturn(Optional.of(cashier));
        when(invoices.save(any(Invoice.class)))
                .thenAnswer(invocation -> id(invocation.getArgument(0), 100L));

        service.pay(new PaySessionRequest(activeSession.getId(), PaymentMethod.CASH));

        InOrder inOrder = inOrder(sessions, foodOrders, invoices, events);
        inOrder.verify(sessions).findLockedById(activeSession.getId());
        inOrder.verify(foodOrders).findDetailsBySessionId(activeSession.getId());
        inOrder.verify(invoices).save(any(Invoice.class));
        inOrder.verify(events).publishEvent(any(RealtimeEvent.class));
    }

    @Test
    void payRejectsAlreadyClosedSessionWith409() {
        ServiceSession closedSession = session();
        closedSession.close(NOW);

        when(sessions.findLockedById(closedSession.getId())).thenReturn(Optional.of(closedSession));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.pay(
                                        new PaySessionRequest(
                                                closedSession.getId(), PaymentMethod.CASH)));

        assertEquals(ErrorCode.SESSION_ALREADY_PAID, ex.getErrorCode());
    }

    @Test
    void payRejectsSessionWithExistingInvoiceWith409() {
        ServiceSession activeSession = session();

        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(true);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.pay(
                                        new PaySessionRequest(
                                                activeSession.getId(), PaymentMethod.CASH)));

        assertEquals(ErrorCode.SESSION_ALREADY_PAID, ex.getErrorCode());
    }

    @Test
    void payBlocksPaymentWhenPendingItemsExist() {
        ServiceSession activeSession = session();
        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(false);

        FoodOrder order = id(new FoodOrder(activeSession, account(), NOW), 20L);
        OrderItem pendingItem = id(new OrderItem(menu(), 1, null), 31L);
        order.addItem(pendingItem);

        when(foodOrders.findDetailsBySessionId(activeSession.getId())).thenReturn(List.of(order));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.pay(
                                        new PaySessionRequest(
                                                activeSession.getId(), PaymentMethod.CASH)));

        assertEquals(ErrorCode.PAYMENT_BLOCKED_PENDING_ITEMS, ex.getErrorCode());
    }

    @Test
    void payBlocksPaymentWhenCookingItemsExist() {
        ServiceSession activeSession = session();
        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(false);

        FoodOrder order = id(new FoodOrder(activeSession, account(), NOW), 20L);
        OrderItem cookingItem = id(new OrderItem(menu(), 1, null), 31L);
        cookingItem.startCooking();
        order.addItem(cookingItem);

        when(foodOrders.findDetailsBySessionId(activeSession.getId())).thenReturn(List.of(order));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.pay(
                                        new PaySessionRequest(
                                                activeSession.getId(), PaymentMethod.CASH)));

        assertEquals(ErrorCode.PAYMENT_BLOCKED_PENDING_ITEMS, ex.getErrorCode());
    }

    @Test
    void payAllowsServedItemsAndCalculatesTotalServerSide() {
        ServiceSession activeSession = session();
        DiningTable table = activeSession.getTable();
        table.occupy();

        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(false);

        FoodOrder order = id(new FoodOrder(activeSession, account(), NOW), 20L);
        OrderItem readyItem = id(new OrderItem(menu(), 3, null), 31L);
        readyItem.startCooking();
        readyItem.markReady();
        readyItem.markServed();
        order.addItem(readyItem);

        when(foodOrders.findDetailsBySessionId(activeSession.getId())).thenReturn(List.of(order));

        Account cashier = account();
        when(currentUser.accountId()).thenReturn(cashier.getId());
        when(accounts.findById(cashier.getId())).thenReturn(Optional.of(cashier));
        when(invoices.save(any(Invoice.class)))
                .thenAnswer(invocation -> id(invocation.getArgument(0), 101L));

        InvoiceResponse response =
                service.pay(new PaySessionRequest(activeSession.getId(), PaymentMethod.CASH));

        assertNotNull(response);
        assertEquals(new BigDecimal("150000"), response.amount());
        assertEquals(SessionStatus.CLOSED, activeSession.getStatus());
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        verify(events)
                .publishEvent(
                        new RealtimeEvent(RealtimeEvent.Type.FLOOR_PLAN, activeSession.getId()));
    }

    @Test
    void payRejectsReadyEvenThroughDirectServiceCall() {
        ServiceSession session = session();
        OrderItem item = id(new OrderItem(menu(), 2, null), 51L);
        item.startCooking();
        item.markReady();
        FoodOrder order = id(new FoodOrder(session, account(), NOW), 50L);
        order.addItem(item);
        when(sessions.findLockedById(session.getId())).thenReturn(Optional.of(session));
        when(foodOrders.findDetailsBySessionId(session.getId())).thenReturn(List.of(order));
        assertEquals(
                ErrorCode.PAYMENT_BLOCKED_PENDING_ITEMS,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.pay(
                                                new PaySessionRequest(
                                                        session.getId(), PaymentMethod.CASH)))
                        .getErrorCode());
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
    }

    @Test
    void payAllowsZeroAmountSessionPerD8() {
        ServiceSession activeSession = session();
        DiningTable table = activeSession.getTable();
        table.occupy();

        when(sessions.findLockedById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(invoices.existsBySessionId(activeSession.getId())).thenReturn(false);
        when(foodOrders.findDetailsBySessionId(activeSession.getId()))
                .thenReturn(Collections.emptyList());

        Account cashier = account();
        when(currentUser.accountId()).thenReturn(cashier.getId());
        when(accounts.findById(cashier.getId())).thenReturn(Optional.of(cashier));
        when(invoices.save(any(Invoice.class)))
                .thenAnswer(invocation -> id(invocation.getArgument(0), 102L));

        InvoiceResponse response =
                service.pay(
                        new PaySessionRequest(activeSession.getId(), PaymentMethod.BANK_TRANSFER));

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.amount());
        assertEquals(PaymentMethod.BANK_TRANSFER, response.paymentMethod());
        assertEquals(SessionStatus.CLOSED, activeSession.getStatus());
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
    }

    @Test
    void listInvoicesPaginatesAndMapsResponse() {
        ServiceSession s = session();
        Invoice invoice =
                id(new Invoice(s, account(), PaymentMethod.CASH, new BigDecimal("50000"), NOW), 1L);

        when(invoices.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(invoice), PageRequest.of(0, 20), 1));

        PageResponse<InvoiceResponse> page = service.listInvoices(null, null, null, 0, 20);

        assertEquals(1, page.total());
        assertEquals(1, page.items().size());
        assertEquals(new BigDecimal("50000"), page.items().get(0).amount());
    }
}
