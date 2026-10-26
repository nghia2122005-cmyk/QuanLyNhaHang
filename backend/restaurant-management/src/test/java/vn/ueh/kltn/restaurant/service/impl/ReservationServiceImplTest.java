package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.reservation;
import static vn.ueh.kltn.restaurant.TestFixtures.table;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.dto.response.ReservationResponse;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class ReservationServiceImplTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final DiningTableRepository tables = mock(DiningTableRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private ReservationServiceImpl service;
    private DiningTable table;

    @BeforeEach
    void setup() {
        var properties =
                new AppProperties(
                        "Asia/Ho_Chi_Minh",
                        false,
                        new AppProperties.Jwt("x".repeat(40), 12),
                        new AppProperties.Alert(3),
                        new AppProperties.Reservation(60));
        service =
                new ReservationServiceImpl(
                        reservations, tables, accounts, user, CLOCK, properties, events, sessions);
        table = table(1L, 4);
        when(tables.findLockedById(1L)).thenReturn(Optional.of(table));
        when(user.accountId()).thenReturn(2L);
        when(accounts.getReferenceById(2L)).thenReturn(account());
        when(reservations.save(any())).thenAnswer(a -> id(a.getArgument(0), 9L));
    }

    private CreateReservationRequest request(int guests) {
        return new CreateReservationRequest(
                1L, "Khách", "0910000001", guests, NOW, NOW.plusSeconds(7200), "Ghi chú");
    }

    @Test
    void updatePreservesCreationAndExcludesSelfAfterTableLocks() {
        DiningTable target = table(1L, 4);
        Reservation existing = reservation(target);
        when(reservations.findTableIdById(1L)).thenReturn(Optional.of(1L));
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(existing));
        ReservationResponse result = service.update(1L, request(3));
        assertEquals(3, result.guestCount());
        assertEquals(existing.getCreatedAt(), result.createdAt());
        verify(reservations).existsOverlapExcluding(eq(1L), any(), any(), any(), eq(1L));
        InOrder locks = Mockito.inOrder(tables, reservations);
        locks.verify(reservations).findTableIdById(1L);
        locks.verify(tables).findLockedById(1L);
        locks.verify(reservations).findLockedById(1L);
    }

    @Test
    void updateRejectsSeatedCancelledAndConcurrentMove() {
        DiningTable target = table(1L, 4);
        Reservation existing = reservation(target);
        when(reservations.findTableIdById(1L)).thenReturn(Optional.of(1L));
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(existing));
        existing.confirmSeated();
        assertThrows(BusinessException.class, () -> service.update(1L, request(2)));
        Reservation cancelled = reservation(target);
        cancelled.cancel();
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(cancelled));
        assertThrows(BusinessException.class, () -> service.update(1L, request(2)));
        when(reservations.findTableIdById(1L)).thenReturn(Optional.of(2L));
        when(tables.findLockedById(2L)).thenReturn(Optional.of(table(2L, 4)));
        assertThrows(BusinessException.class, () -> service.update(1L, request(2)));
    }

    @Test
    void rejectsOverlap() {
        when(reservations.existsOverlap(eq(1L), any(), any(), any())).thenReturn(true);
        assertEquals(
                ErrorCode.RESERVATION_OVERLAP,
                assertThrows(BusinessException.class, () -> service.create(request(2)))
                        .getErrorCode());
        verify(reservations, never()).save(any());
    }

    @Test
    void touchingBoundaryIsAcceptedAndUsesCurrentCreator() {
        ReservationResponse r =
                service.create(
                        new CreateReservationRequest(
                                1L,
                                "Khách",
                                "0910000001",
                                2,
                                NOW.plusSeconds(7200),
                                NOW.plusSeconds(14400),
                                null));
        assertEquals(ReservationStatus.CONFIRMED, r.status());
        assertEquals(2L, r.createdById());
        verify(events).publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
        verify(reservations)
                .existsOverlap(
                        1L,
                        NOW.plusSeconds(7200),
                        NOW.plusSeconds(14400),
                        Set.of(ReservationStatus.CONFIRMED, ReservationStatus.SEATED));
    }

    @Test
    void rejectsCapacity() {
        assertEquals(
                ErrorCode.TABLE_CAPACITY_EXCEEDED,
                assertThrows(BusinessException.class, () -> service.create(request(5)))
                        .getErrorCode());
    }

    @Test
    void rejectsInactiveTable() {
        ReflectionTestUtils.setField(table, "status", TableStatus.INACTIVE);
        assertEquals(
                ErrorCode.TABLE_INACTIVE,
                assertThrows(BusinessException.class, () -> service.create(request(2)))
                        .getErrorCode());
    }

    @Test
    void rejectsInvalidIntervalButDoesNotBanPast() {
        assertEquals(
                ErrorCode.INVALID_INPUT,
                assertThrows(BusinessException.class, () -> service.available(NOW, NOW, 2))
                        .getErrorCode());
        when(tables.findAvailable(any(), any(), anyInt(), any(), any())).thenReturn(List.of(table));
        assertEquals(
                1, service.available(NOW.minusSeconds(7200), NOW.minusSeconds(3600), 2).size());
    }

    @Test
    void cancelConfirmedAndCancelledIsIdempotent() {
        Reservation r = reservation(table);
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(r));
        assertEquals(ReservationStatus.CANCELLED, service.cancel(1L).status());
        assertEquals(ReservationStatus.CANCELLED, service.cancel(1L).status());
        verify(events, times(1))
                .publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
    }

    @Test
    void cannotCancelSeated() {
        Reservation r = reservation(table);
        r.confirmSeated();
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(r));
        assertEquals(
                ErrorCode.RESERVATION_NOT_CONFIRMED,
                assertThrows(BusinessException.class, () -> service.cancel(1L)).getErrorCode());
    }

    @Test
    void missingReservationIs404() {
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.cancel(99L)).getErrorCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sessionPickerFiltersByTableAndStrictlyFutureEndTime() {
        Root<Reservation> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        when(reservations.findAll(
                        ArgumentMatchers.<Specification<Reservation>>any(), any(Pageable.class)))
                .thenAnswer(
                        invocation -> {
                            Specification<Reservation> filter = invocation.getArgument(0);
                            filter.toPredicate(root, query, builder);
                            return Page.empty();
                        });
        service.list(null, ReservationStatus.CONFIRMED, null, 7L, true, 0, 100);
        verify(builder).equal(root.get("table").get("id"), 7L);
        verify(builder).greaterThan(root.<Instant>get("endTime"), NOW);
        verify(builder).equal(root.get("status"), ReservationStatus.CONFIRMED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void regularListKeepsHistoricalReservationsWithoutPickerFilters() {
        Root<Reservation> root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        when(reservations.findAll(
                        ArgumentMatchers.<Specification<Reservation>>any(), any(Pageable.class)))
                .thenAnswer(
                        invocation -> {
                            Specification<Reservation> filter = invocation.getArgument(0);
                            filter.toPredicate(root, mock(CriteriaQuery.class), builder);
                            return Page.empty();
                        });
        service.list(null, null, null, null, false, 0, 20);
        verify(root, never()).get("table");
        verify(root, never()).get("endTime");
    }
}
