package vn.ueh.kltn.restaurant.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningAreaRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningTableRequest;
import vn.ueh.kltn.restaurant.dto.response.DiningAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.DiningTableResponse;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanAreaResponse;
import vn.ueh.kltn.restaurant.dto.response.FloorPlanTableResponse;
import vn.ueh.kltn.restaurant.dto.response.UpcomingReservationResponse;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.ReservationMapper;
import vn.ueh.kltn.restaurant.mapper.TableMapper;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.DiningAreaRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.service.TableService;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {
    private final DiningTableRepository tables;
    private final ServiceSessionRepository sessions;
    private final ReservationRepository reservations;
    private final AppProperties properties;
    private final Clock clock;
    private final DiningAreaRepository areas;
    private final ApplicationEventPublisher events;

    public List<DiningAreaResponse> listAreas() {
        return areas.findAll(Sort.by("id")).stream().map(TableMapper::area).toList();
    }

    public List<DiningTableResponse> listTables() {
        List<DiningTable> all = tables.findAllWithArea();
        if (all.isEmpty()) return List.of();
        List<Long> ids = all.stream().map(DiningTable::getId).toList();
        Map<Long, Long> active = new HashMap<>();
        sessions.findByTableIdInAndStatus(ids, SessionStatus.ACTIVE)
                .forEach(session -> active.put(session.getTable().getId(), session.getId()));
        Set<Long> live =
                new HashSet<>(reservations.findLiveConfirmedTableIds(ids, clock.instant()));
        return all.stream()
                .map(
                        table ->
                                TableMapper.managed(
                                        table,
                                        active.get(table.getId()),
                                        live.contains(table.getId())))
                .toList();
    }

    @Transactional
    public DiningAreaResponse createArea(SaveDiningAreaRequest request) {
        String code = code(request.code(), 40);
        uniqueAreaCode(code, 0L);
        DiningArea area = areas.save(new DiningArea(name(request.name(), 100), code));
        changed();
        return TableMapper.area(area);
    }

    @Transactional
    public DiningAreaResponse updateArea(Long id, SaveDiningAreaRequest request) {
        DiningArea area = lockedArea(id);
        String code = code(request.code(), 40);
        uniqueAreaCode(code, id);
        area.updateInfo(name(request.name(), 100), code);
        changed();
        return TableMapper.area(area);
    }

    @Transactional
    public DiningAreaResponse deactivateArea(Long id) {
        // Holding the parent prevents concurrent table creation/moves during membership checks.
        DiningArea area = lockedArea(id);
        List<DiningTable> children =
                tables.findIdsByAreaId(id).stream().map(this::lockedTable).toList();
        requireNoOrders(children);
        area.deactivate();
        changed();
        return TableMapper.area(area);
    }

    @Transactional
    public DiningAreaResponse reactivateArea(Long id) {
        DiningArea area = lockedArea(id);
        area.activate();
        changed();
        return TableMapper.area(area);
    }

    @Transactional
    public DiningTableResponse createTable(SaveDiningTableRequest request) {
        DiningArea area = lockedArea(request.areaId());
        requireActiveArea(area);
        String code = code(request.code(), 64);
        uniqueTableCode(code, 0L);
        if (request.capacity() <= 0)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Sức chứa phải lớn hơn 0");
        DiningTable table =
                tables.save(
                        new DiningTable(area, name(request.name(), 50), code, request.capacity()));
        changed();
        return response(table);
    }

    @Transactional
    public DiningTableResponse updateTable(Long id, SaveDiningTableRequest request) {
        Long source = sourceArea(id);
        Map<Long, DiningArea> locked = new HashMap<>();
        if (request.areaId() == null)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vui lòng chọn khu vực");
        for (Long areaId :
                List.of(source, request.areaId()).stream().distinct().sorted().toList()) {
            locked.put(areaId, lockedArea(areaId));
        }
        DiningTable table = lockedTable(id);
        requireSameArea(table, source);
        DiningArea target = locked.get(request.areaId());
        if (!source.equals(request.areaId())) {
            requireActiveArea(target);
            requireNoOrders(List.of(table));
        }
        if (request.capacity() < table.getCapacity()
                && (sessions.existsByTableIdAndStatus(id, SessionStatus.ACTIVE)
                        || table.getStatus() == TableStatus.OCCUPIED
                        || request.capacity()
                                < reservations.maxLiveGuestCount(id, clock.instant()))) {
            throw new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "Không thể giảm sức chứa khi đang phục vụ hoặc vượt quá số khách đã đặt bàn");
        }
        String code = code(request.code(), 64);
        uniqueTableCode(code, id);
        table.updateInfo(target, name(request.name(), 50), code, request.capacity());
        changed();
        return response(table);
    }

    @Transactional
    public DiningTableResponse deactivateTable(Long id) {
        DiningTable table = managedTable(id);
        requireNoOrders(List.of(table));
        table.deactivate();
        changed();
        return response(table);
    }

    @Transactional
    public DiningTableResponse reactivateTable(Long id) {
        DiningTable table = managedTable(id);
        requireActiveArea(table.getArea());
        table.activate();
        changed();
        return response(table);
    }

    private DiningTable managedTable(Long id) {
        Long source = sourceArea(id);
        lockedArea(source);
        DiningTable table = lockedTable(id);
        requireSameArea(table, source);
        return table;
    }

    private Long sourceArea(Long id) {
        return tables.findAreaIdById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bàn"));
    }

    private void requireSameArea(DiningTable table, Long source) {
        if (!table.getArea().getId().equals(source))
            throw new BusinessException(
                    ErrorCode.DATA_CONFLICT, "Bàn vừa được chuyển khu vực. Vui lòng tải lại");
    }

    private DiningArea lockedArea(Long id) {
        if (id == null)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vui lòng chọn khu vực");
        return areas.findLockedById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy khu vực"));
    }

    private DiningTable lockedTable(Long id) {
        return tables.findLockedById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bàn"));
    }

    private void requireActiveArea(DiningArea area) {
        if (area.getStatus() != AreaStatus.ACTIVE)
            throw new BusinessException(ErrorCode.INVALID_STATE, "Khu vực đã ngừng sử dụng");
    }

    private void requireNoOrders(List<DiningTable> rows) {
        if (rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(DiningTable::getId).toList();
        if (rows.stream().anyMatch(table -> table.getStatus() == TableStatus.OCCUPIED)
                || !sessions.findByTableIdInAndStatus(ids, SessionStatus.ACTIVE).isEmpty()
                || !reservations.findLiveConfirmedTableIds(ids, clock.instant()).isEmpty()) {
            throw new BusinessException(
                    ErrorCode.DATA_CONFLICT,
                    "Không thể ngừng sử dụng hoặc chuyển khu vực khi bàn có phiên đang mở hoặc đặt"
                            + " bàn còn hiệu lực");
        }
    }

    private DiningTableResponse response(DiningTable table) {
        List<Long> ids = List.of(table.getId());
        Long sessionId =
                sessions.findByTableIdInAndStatus(ids, SessionStatus.ACTIVE).stream()
                        .map(session -> session.getId())
                        .findFirst()
                        .orElse(null);
        return TableMapper.managed(
                table,
                sessionId,
                !reservations.findLiveConfirmedTableIds(ids, clock.instant()).isEmpty());
    }

    private String name(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "Tên không được trống và tối đa " + max + " ký tự");
        return value.strip();
    }

    private String code(String value, int max) {
        String code = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (code.length() > max || !code.matches("[A-Z0-9][A-Z0-9_-]*"))
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Mã chỉ gồm chữ không dấu, số, dấu gạch ngang hoặc gạch dưới; tối đa "
                            + max
                            + " ký tự");
        return code;
    }

    private void uniqueAreaCode(String code, Long excluded) {
        if (areas.existsByCodeIgnoreCaseAndIdNot(code, excluded))
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Mã khu vực đã tồn tại");
    }

    private void uniqueTableCode(String code, Long excluded) {
        if (tables.existsByCodeIgnoreCaseAndIdNot(code, excluded))
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Mã bàn đã tồn tại");
    }

    private void changed() {
        events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.FLOOR_PLAN, null));
    }

    public List<FloorPlanAreaResponse> floorPlan() {
        List<DiningTable> all = tables.findFloorPlanTables();
        if (all.isEmpty()) return List.of();
        List<Long> ids = all.stream().map(t -> t.getId()).toList();
        var active = new HashMap<Long, Long>();
        sessions.findByTableIdInAndStatus(ids, SessionStatus.ACTIVE)
                .forEach(s -> active.put(s.getTable().getId(), s.getId()));
        Instant now = clock.instant();
        var upcoming = new HashMap<Long, UpcomingReservationResponse>();
        reservations
                .findUpcoming(
                        ids,
                        now,
                        now.plus(
                                properties.reservation().upcomingWarningMinutes(),
                                ChronoUnit.MINUTES))
                .forEach(
                        r ->
                                upcoming.putIfAbsent(
                                        r.getTable().getId(), ReservationMapper.upcoming(r)));
        var areas = new LinkedHashMap<Long, List<FloorPlanTableResponse>>();
        var names = new HashMap<Long, String>();
        for (DiningTable table : all) {
            DiningArea area = table.getArea();
            names.put(area.getId(), area.getName());
            areas.computeIfAbsent(area.getId(), ignored -> new ArrayList<>())
                    .add(
                            TableMapper.floor(
                                    table, active.get(table.getId()), upcoming.get(table.getId())));
        }
        return areas.entrySet().stream()
                .map(
                        a ->
                                new FloorPlanAreaResponse(
                                        a.getKey(),
                                        names.get(a.getKey()),
                                        a.getValue(),
                                        all.stream()
                                                .filter(t -> t.getArea().getId().equals(a.getKey()))
                                                .findFirst()
                                                .orElseThrow()
                                                .getArea()
                                                .getCode()))
                .toList();
    }
}
