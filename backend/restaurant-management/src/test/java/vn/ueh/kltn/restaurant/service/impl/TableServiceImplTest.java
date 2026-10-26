package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.reservation;
import static vn.ueh.kltn.restaurant.TestFixtures.table;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanAreaResponse;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.repository.DiningAreaRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;

import java.util.List;

class TableServiceImplTest {
    private final DiningTableRepository tables = mock(DiningTableRepository.class);
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final AppProperties properties =
            new AppProperties(
                    "Asia/Ho_Chi_Minh",
                    false,
                    new AppProperties.Jwt("x".repeat(40), 12),
                    new AppProperties.Alert(3),
                    new AppProperties.Reservation(60));
    private final TableServiceImpl service =
            new TableServiceImpl(
                    tables,
                    sessions,
                    reservations,
                    properties,
                    CLOCK,
                    mock(DiningAreaRepository.class),
                    mock(ApplicationEventPublisher.class));

    @Test
    void groupsTablesAndUsesNearestReservationInExactlyThreeRepositoryCalls() {
        DiningTable table = table(1L, 4);
        DiningTable inactive = table(2L, 2);
        ReflectionTestUtils.setField(inactive, "status", TableStatus.INACTIVE);
        ServiceSession session = id(new ServiceSession(table, null, account(), NOW), 5L);
        Reservation nearest = reservation(table);
        Reservation later = id(reservation(table), 2L);
        ReflectionTestUtils.setField(later, "startTime", NOW.plusSeconds(1800));
        when(tables.findFloorPlanTables()).thenReturn(List.of(table, inactive));
        when(sessions.findByTableIdInAndStatus(List.of(1L, 2L), SessionStatus.ACTIVE))
                .thenReturn(List.of(session));
        when(reservations.findUpcoming(List.of(1L, 2L), NOW, NOW.plusSeconds(3600)))
                .thenReturn(List.of(nearest, later));
        List<FloorPlanAreaResponse> result = service.floorPlan();
        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().tables().size());
        assertEquals(5L, result.getFirst().tables().getFirst().activeSessionId());
        assertEquals(1L, result.getFirst().tables().getFirst().upcomingReservation().id());
        assertEquals(TableStatus.INACTIVE, result.getFirst().tables().get(1).status());
        verify(tables).findFloorPlanTables();
        verify(sessions).findByTableIdInAndStatus(List.of(1L, 2L), SessionStatus.ACTIVE);
        verify(reservations).findUpcoming(List.of(1L, 2L), NOW, NOW.plusSeconds(3600));
        verifyNoMoreInteractions(tables, sessions, reservations);
    }

    @Test
    void emptyFloorDoesNotQuerySessionsOrReservations() {
        when(tables.findFloorPlanTables()).thenReturn(List.of());
        assertTrue(service.floorPlan().isEmpty());
        verifyNoInteractions(sessions, reservations);
    }
}
