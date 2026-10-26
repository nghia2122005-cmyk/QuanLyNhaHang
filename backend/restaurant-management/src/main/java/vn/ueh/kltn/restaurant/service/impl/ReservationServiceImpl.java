package vn.ueh.kltn.restaurant.service.impl;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.dto.response.AvailableTableResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.ReservationResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.ReservationMapper;
import vn.ueh.kltn.restaurant.mapper.TableMapper;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.ReservationService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {
    private static final Set<ReservationStatus> BLOCKING =
            Set.of(ReservationStatus.CONFIRMED, ReservationStatus.SEATED);
    private final ReservationRepository reservations;
    private final DiningTableRepository tables;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final AppProperties properties;
    private final ApplicationEventPublisher events;
    private final ServiceSessionRepository sessions;

    public PageResponse<ReservationResponse> list(
            LocalDate date,
            ReservationStatus status,
            String q,
            Long tableId,
            boolean unexpired,
            int page,
            int size) {
        ZoneId zone = ZoneId.of(properties.timezone());
        Instant from = date == null ? null : date.atStartOfDay(zone).toInstant();
        Instant to = date == null ? null : date.plusDays(1).atStartOfDay(zone).toInstant();
        Instant now = clock.instant();
        Specification<Reservation> filter =
                (root, query, cb) -> {
                    var conditions = new ArrayList<Predicate>();
                    if (from != null) {
                        conditions.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
                        conditions.add(cb.lessThan(root.get("startTime"), to));
                    }
                    if (status != null) conditions.add(cb.equal(root.get("status"), status));
                    if (tableId != null)
                        conditions.add(cb.equal(root.get("table").get("id"), tableId));
                    if (unexpired) conditions.add(cb.greaterThan(root.get("endTime"), now));
                    if (q != null && !q.isBlank()) {
                        String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                        conditions.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("customerName")), term),
                                        cb.like(root.get("customerPhone"), term)));
                    }
                    return cb.and(conditions.toArray(Predicate[]::new));
                };
        return PageResponse.from(
                reservations
                        .findAll(
                                filter,
                                PageRequest.of(page, size, Sort.by("startTime").and(Sort.by("id"))))
                        .map(r -> ReservationMapper.toResponse(r, now)));
    }

    public List<AvailableTableResponse> available(Instant start, Instant end, int guestCount) {
        return available(start, end, guestCount, null);
    }

    public List<AvailableTableResponse> available(
            Instant start, Instant end, int guestCount, Long excludedId) {
        validateInterval(start, end);
        if (guestCount <= 0)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Số khách phải lớn hơn 0");
        return tables.findAvailable(start, end, guestCount, BLOCKING, excludedId).stream()
                .map(TableMapper::available)
                .toList();
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        return createWithCreator(request, accounts.getReferenceById(currentUser.accountId()));
    }

    @Transactional
    public ReservationResponse createForSeed(CreateReservationRequest request, Account creator) {
        return createWithCreator(request, creator);
    }

    private ReservationResponse createWithCreator(CreateReservationRequest r, Account creator) {
        DiningTable table =
                tables.findLockedById(r.tableId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "Không tìm thấy bàn"));
        if (table.getStatus() == TableStatus.INACTIVE
                || table.getArea().getStatus() != AreaStatus.ACTIVE)
            throw new BusinessException(ErrorCode.TABLE_INACTIVE);
        if (!table.hasEnoughCapacity(r.guestCount()))
            throw new BusinessException(ErrorCode.TABLE_CAPACITY_EXCEEDED);
        validateInterval(r.startTime(), r.endTime());
        checkOverlap(table.getId(), r.startTime(), r.endTime());
        Instant now = clock.instant();
        Reservation saved =
                reservations.save(
                        new Reservation(
                                table,
                                creator,
                                r.customerName(),
                                r.customerPhone(),
                                r.guestCount(),
                                r.startTime(),
                                r.endTime(),
                                r.note(),
                                now));
        events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
        return ReservationMapper.toResponse(saved, now);
    }

    @Transactional
    public ReservationResponse update(Long id, CreateReservationRequest request) {
        Long sourceId =
                reservations
                        .findTableIdById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Map<Long, DiningTable> locked = new HashMap<>();
        for (Long tableId : Stream.of(sourceId, request.tableId()).distinct().sorted().toList())
            locked.put(
                    tableId,
                    tables.findLockedById(tableId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND, "Không tìm thấy bàn")));
        Reservation reservation =
                reservations
                        .findLockedById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!reservation.getTable().getId().equals(sourceId))
            throw new BusinessException(
                    ErrorCode.DATA_CONFLICT, "Đặt bàn vừa được thay đổi. Vui lòng tải lại");
        if (reservation.getStatus() != ReservationStatus.CONFIRMED
                || sessions.existsByReservationId(id))
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CONFIRMED);
        DiningTable target = locked.get(request.tableId());
        if (target.getStatus() == TableStatus.INACTIVE
                || target.getArea().getStatus() != AreaStatus.ACTIVE)
            throw new BusinessException(ErrorCode.TABLE_INACTIVE);
        if (!target.hasEnoughCapacity(request.guestCount()))
            throw new BusinessException(ErrorCode.TABLE_CAPACITY_EXCEEDED);
        validateInterval(request.startTime(), request.endTime());
        if (reservations.existsOverlapExcluding(
                target.getId(), request.startTime(), request.endTime(), BLOCKING, id))
            throw new BusinessException(ErrorCode.RESERVATION_OVERLAP);
        reservation.updateInfo(
                target,
                request.customerName(),
                request.customerPhone(),
                request.guestCount(),
                request.startTime(),
                request.endTime(),
                request.note());
        events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
        return ReservationMapper.toResponse(reservation, clock.instant());
    }

    public void checkOverlap(Long tableId, Instant start, Instant end) {
        if (reservations.existsOverlap(tableId, start, end, BLOCKING))
            throw new BusinessException(ErrorCode.RESERVATION_OVERLAP);
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation =
                reservations
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "Không tìm thấy đặt bàn"));
        if (reservation.getStatus() == ReservationStatus.SEATED)
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CONFIRMED);
        if (reservation.getStatus() != ReservationStatus.CANCELLED) {
            reservation.cancel();
            events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
        }
        return ReservationMapper.toResponse(reservation, clock.instant());
    }

    private void validateInterval(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end))
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "Giờ kết thúc phải sau giờ bắt đầu");
    }
}
