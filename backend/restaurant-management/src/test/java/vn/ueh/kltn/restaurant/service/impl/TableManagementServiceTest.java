package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningAreaRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningTableRequest;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.repository.DiningAreaRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;

import java.util.List;
import java.util.Optional;

class TableManagementServiceTest {
    private final DiningTableRepository tables = mock(DiningTableRepository.class);
    private final DiningAreaRepository areas = mock(DiningAreaRepository.class);
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AppProperties properties =
            new AppProperties(
                    "Asia/Ho_Chi_Minh",
                    false,
                    new AppProperties.Jwt("x".repeat(40), 12),
                    new AppProperties.Alert(3),
                    new AppProperties.Reservation(60));
    private final TableServiceImpl service =
            new TableServiceImpl(tables, sessions, reservations, properties, CLOCK, areas, events);
    private final DiningArea area = id(new DiningArea("Tầng một", "T01"), 1L);
    private final DiningTable table = id(new DiningTable(area, "Bàn một", "T01-B01", 8), 10L);

    @BeforeEach
    void setup() {
        when(areas.findLockedById(1L)).thenReturn(Optional.of(area));
        when(tables.findLockedById(10L)).thenReturn(Optional.of(table));
        when(tables.findAreaIdById(10L)).thenReturn(Optional.of(1L));
    }

    @Test
    void createsNormalizedAreaAndRejectsDuplicateCodeAcrossInactiveAreas() {
        when(areas.save(any(DiningArea.class))).thenAnswer(inv -> id(inv.getArgument(0), 2L));
        assertEquals(
                "SV", service.createArea(new SaveDiningAreaRequest(" Sân vườn ", " sv ")).code());
        when(areas.existsByCodeIgnoreCaseAndIdNot("T01", 0L)).thenReturn(true);
        assertEquals(
                ErrorCode.DATA_CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.createArea(new SaveDiningAreaRequest("Khác", "t01")))
                        .getErrorCode());
    }

    @Test
    void newTableRequiresActiveAreaPositiveCapacityAndUniqueCode() {
        when(tables.save(any(DiningTable.class))).thenAnswer(inv -> id(inv.getArgument(0), 11L));
        assertEquals(
                "T01-B02",
                service.createTable(new SaveDiningTableRequest(1L, " Bàn hai ", "t01-b02", 4))
                        .code());
        assertThrows(
                BusinessException.class,
                () -> service.createTable(new SaveDiningTableRequest(1L, "A", "A", 0)));
        when(tables.existsByCodeIgnoreCaseAndIdNot("T01-B01", 0L)).thenReturn(true);
        assertThrows(
                BusinessException.class,
                () -> service.createTable(new SaveDiningTableRequest(1L, "A", "T01-B01", 4)));
        area.deactivate();
        assertThrows(
                BusinessException.class,
                () -> service.createTable(new SaveDiningTableRequest(1L, "A", "X", 4)));
    }

    @Test
    void activeSessionBlocksDeactivationEvenWhenTableStatusIsStaleAvailable() {
        ServiceSession session = id(new ServiceSession(table, null, account(), NOW), 3L);
        when(sessions.findByTableIdInAndStatus(List.of(10L), SessionStatus.ACTIVE))
                .thenReturn(List.of(session));
        assertThrows(BusinessException.class, () -> service.deactivateTable(10L));
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
    }

    @Test
    void unexpiredConfirmedReservationBlocksTableAndWholeAreaAtomically() {
        when(tables.findIdsByAreaId(1L)).thenReturn(List.of(10L));
        when(reservations.findLiveConfirmedTableIds(List.of(10L), NOW)).thenReturn(List.of(10L));
        assertThrows(BusinessException.class, () -> service.deactivateTable(10L));
        assertThrows(BusinessException.class, () -> service.deactivateArea(1L));
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        assertEquals(AreaStatus.ACTIVE, area.getStatus());
    }

    @Test
    void quietTableCanStopAndResumeWithoutDeletingHistoryOrChangingItsId() {
        assertEquals(TableStatus.INACTIVE, service.deactivateTable(10L).status());
        assertEquals(TableStatus.AVAILABLE, service.reactivateTable(10L).status());
        assertEquals(10L, table.getId());
    }

    @Test
    void areaStopPreservesIndividualTableStatesAndLocksParentBeforeChildren() {
        DiningTable other = id(new DiningTable(area, "Hai", "T01-B02", 4), 20L);
        other.deactivate();
        when(tables.findLockedById(20L)).thenReturn(Optional.of(other));
        when(tables.findAreaIdById(20L)).thenReturn(Optional.of(1L));
        when(tables.findIdsByAreaId(1L)).thenReturn(List.of(10L, 20L));
        service.deactivateArea(1L);
        assertEquals(AreaStatus.INACTIVE, area.getStatus());
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        assertEquals(TableStatus.INACTIVE, other.getStatus());
        assertThrows(BusinessException.class, () -> service.reactivateTable(20L));
        service.reactivateArea(1L);
        assertEquals(TableStatus.INACTIVE, other.getStatus());
        InOrder locks = inOrder(areas, tables);
        locks.verify(areas).findLockedById(1L);
        locks.verify(tables).findIdsByAreaId(1L);
        locks.verify(tables).findLockedById(10L);
        locks.verify(tables).findLockedById(20L);
    }

    @Test
    void tableMoveLocksAreasAscendingAndDoesNotRegenerateCode() {
        DiningArea target = id(new DiningArea("Hai", "T02"), 2L);
        when(areas.findLockedById(2L)).thenReturn(Optional.of(target));
        service.updateTable(10L, new SaveDiningTableRequest(2L, "Mới", "T01-B01", 8));
        assertEquals(2L, table.getArea().getId());
        assertEquals("T01-B01", table.getCode());
        InOrder locks = inOrder(areas, tables);
        locks.verify(tables).findAreaIdById(10L);
        locks.verify(areas).findLockedById(1L);
        locks.verify(areas).findLockedById(2L);
        locks.verify(tables).findLockedById(10L);
    }

    @Test
    void shrinkingCapacityCannotInvalidateLiveGuestCount() {
        when(reservations.maxLiveGuestCount(10L, NOW)).thenReturn(6);
        assertThrows(
                BusinessException.class,
                () ->
                        service.updateTable(
                                10L, new SaveDiningTableRequest(1L, "Một", "T01-B01", 4)));
        assertEquals(8, table.getCapacity());
        assertEquals(
                8,
                service.updateTable(10L, new SaveDiningTableRequest(1L, "Một", "T01-B01", 8))
                        .capacity());
    }
}
