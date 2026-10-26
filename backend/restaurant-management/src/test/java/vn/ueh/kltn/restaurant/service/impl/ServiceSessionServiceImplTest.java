package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.reservation;
import static vn.ueh.kltn.restaurant.TestFixtures.table;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.dto.request.OpenSessionRequest;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionResponse;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.FoodOrderRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class ServiceSessionServiceImplTest {
    private final DiningTableRepository tables = mock(DiningTableRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private ServiceSessionServiceImpl service;
    private DiningTable table;

    @BeforeEach
    void setup() {
        service =
                new ServiceSessionServiceImpl(
                        tables,
                        reservations,
                        sessions,
                        accounts,
                        user,
                        CLOCK,
                        mock(FoodOrderRepository.class),
                        mock(OrderItemRepository.class),
                        mock(MenuItemRepository.class),
                        events);
        table = table(1L, 4);
        when(tables.findLockedById(1L)).thenReturn(Optional.of(table));
        when(user.accountId()).thenReturn(2L);
        when(accounts.getReferenceById(2L)).thenReturn(account());
        when(sessions.saveAndFlush(any())).thenAnswer(a -> id(a.getArgument(0), 7L));
    }

    @Test
    void walkInOccupiesTableWithoutConsultingReservations() {
        OpenSessionResponse result = service.open(new OpenSessionRequest(1L, null));
        assertEquals(7L, result.sessionId());
        assertNull(result.reservationId());
        assertEquals(SessionStatus.ACTIVE, result.status());
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
        verifyNoInteractions(reservations);
        verify(events).publishEvent(new RealtimeEvent(RealtimeEvent.Type.FLOOR_PLAN, 7L));
    }

    @Test
    void reservationSeatsAndLocksAfterTable() {
        Reservation r = reservation(table);
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(r));
        assertEquals(1L, service.open(new OpenSessionRequest(1L, 1L)).reservationId());
        assertEquals(ReservationStatus.SEATED, r.getStatus());
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
        InOrder order = inOrder(tables, reservations);
        order.verify(tables).findLockedById(1L);
        order.verify(reservations).findLockedById(1L);
        verify(events).publishEvent(new RealtimeEvent(RealtimeEvent.Type.FLOOR_PLAN, 7L));
        verify(events).publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
    }

    private ErrorCode failure(Long reservationId) {
        return assertThrows(
                        BusinessException.class,
                        () -> service.open(new OpenSessionRequest(1L, reservationId)))
                .getErrorCode();
    }

    @Test
    void inactiveIsRejected() {
        ReflectionTestUtils.setField(table, "status", TableStatus.INACTIVE);
        assertEquals(ErrorCode.TABLE_INACTIVE, failure(null));
    }

    @Test
    void existingActiveSessionIsRejectedBeforeTableStatus() {
        table.occupy();
        when(sessions.existsByTableIdAndStatus(1L, SessionStatus.ACTIVE)).thenReturn(true);
        assertEquals(ErrorCode.SESSION_ALREADY_ACTIVE, failure(null));
        verifyNoInteractions(events);
    }

    @Test
    void occupiedWithoutSessionIsNotAvailable() {
        table.occupy();
        assertEquals(ErrorCode.TABLE_NOT_AVAILABLE, failure(null));
    }

    @Test
    void reservationTableMustMatch() {
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(reservation(table(2L, 4))));
        assertEquals(ErrorCode.RESERVATION_TABLE_MISMATCH, failure(1L));
    }

    @Test
    void cancelledReservationIsRejected() {
        Reservation r = reservation(table);
        r.cancel();
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(r));
        assertEquals(ErrorCode.RESERVATION_NOT_CONFIRMED, failure(1L));
    }

    @Test
    void expiredReservationIsRejected() {
        Reservation r = reservation(table);
        ReflectionTestUtils.setField(r, "endTime", NOW);
        when(reservations.findLockedById(1L)).thenReturn(Optional.of(r));
        assertEquals(ErrorCode.RESERVATION_EXPIRED, failure(1L));
    }

    @Test
    void databaseRaceIsClean409Code() {
        doThrow(constraintFailure("uq_active_session_table")).when(sessions).saveAndFlush(any());
        assertEquals(ErrorCode.SESSION_ALREADY_ACTIVE, failure(null));
    }

    @Test
    void unrelatedConstraintsRemainGenericDataConflicts() {
        for (String constraint : List.of("uq_session_reservation", "fk_session_table", "unknown")) {
            table = table(1L, 4);
            when(tables.findLockedById(1L)).thenReturn(Optional.of(table));
            DataIntegrityViolationException original = constraintFailure(constraint);
            doThrow(original).when(sessions).saveAndFlush(any());
            assertSame(
                    original,
                    assertThrows(
                            DataIntegrityViolationException.class,
                            () -> service.open(new OpenSessionRequest(1L, null))));
        }
    }

    @Test
    void unclassifiedIntegrityFailureIsNotReportedAsAnActiveSession() {
        DataIntegrityViolationException original = new DataIntegrityViolationException("not null");
        doThrow(original).when(sessions).saveAndFlush(any());
        assertSame(
                original,
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> service.open(new OpenSessionRequest(1L, null))));
    }

    private DataIntegrityViolationException constraintFailure(String name) {
        return new DataIntegrityViolationException(
                "constraint",
                new ConstraintViolationException(
                        "constraint", new SQLException("constraint"), name));
    }

    @Test
    void missingReservationIs404() {
        assertEquals(ErrorCode.NOT_FOUND, failure(99L));
    }
}
