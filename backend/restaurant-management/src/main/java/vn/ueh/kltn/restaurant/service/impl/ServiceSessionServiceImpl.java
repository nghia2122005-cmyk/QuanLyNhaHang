package vn.ueh.kltn.restaurant.service.impl;

import lombok.RequiredArgsConstructor;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.OpenSessionRequest;
import vn.ueh.kltn.restaurant.dto.request.OrderLineRequest;
import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.dto.response.SessionDetailResponse;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.MenuItemStatus;
import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.FoodOrderMapper;
import vn.ueh.kltn.restaurant.mapper.ReservationMapper;
import vn.ueh.kltn.restaurant.mapper.ServiceSessionMapper;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.FoodOrderRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.ServiceSessionService;
import vn.ueh.kltn.restaurant.validation.OrderLimits;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceSessionServiceImpl implements ServiceSessionService {
    private final DiningTableRepository tables;
    private final ReservationRepository reservations;
    private final ServiceSessionRepository sessions;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final FoodOrderRepository orders;
    private final OrderItemRepository items;
    private final MenuItemRepository menu;
    private final ApplicationEventPublisher events;

    @Transactional
    public OpenSessionResponse open(OpenSessionRequest request) {
        // Fixed lock order: DiningTable, then Reservation. Cancel never locks a table.
        DiningTable table =
                tables.findLockedById(request.tableId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "Không tìm thấy bàn"));
        if (table.getStatus() == TableStatus.INACTIVE
                || table.getArea().getStatus() != AreaStatus.ACTIVE)
            throw new BusinessException(ErrorCode.TABLE_INACTIVE);
        if (sessions.existsByTableIdAndStatus(table.getId(), SessionStatus.ACTIVE))
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ACTIVE);
        if (table.getStatus() != TableStatus.AVAILABLE)
            throw new BusinessException(ErrorCode.TABLE_NOT_AVAILABLE);
        Instant now = clock.instant();
        Reservation reservation = null;
        if (request.reservationId() != null) {
            reservation =
                    reservations
                            .findLockedById(request.reservationId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND, "Không tìm thấy đặt bàn"));
            if (reservation.getStatus() != ReservationStatus.CONFIRMED)
                throw new BusinessException(ErrorCode.RESERVATION_NOT_CONFIRMED);
            if (!reservation.getTable().getId().equals(table.getId()))
                throw new BusinessException(ErrorCode.RESERVATION_TABLE_MISMATCH);
            if (!reservation.getEndTime().isAfter(now))
                throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
            reservation.confirmSeated();
        }
        var session =
                new ServiceSession(
                        table,
                        reservation,
                        accounts.getReferenceById(currentUser.accountId()),
                        now);
        table.occupy();
        try {
            sessions.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_active_session_table".equals(violation.getConstraintName())) {
                    throw new BusinessException(ErrorCode.SESSION_ALREADY_ACTIVE);
                }
            }
            throw exception;
        }
        events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.FLOOR_PLAN, session.getId()));
        if (reservation != null) {
            events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
        }
        return ServiceSessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse detail(Long id) {
        ServiceSession session =
                sessions.findDetailById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy phiên phục vụ"));
        List<FoodOrder> foodOrders = orders.findDetailsBySessionId(id);
        List<FoodOrderResponse> responses =
                foodOrders.stream().map(FoodOrderMapper::order).toList();
        BigDecimal total =
                foodOrders.stream()
                        .map(FoodOrder::calculateTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SessionDetailResponse(
                session.getId(),
                session.getTable().getId(),
                session.getTable().getName(),
                session.getTable().getArea().getName(),
                session.getReservation() == null
                        ? null
                        : ReservationMapper.toResponse(session.getReservation(), clock.instant()),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getStatus(),
                responses,
                total,
                session.getTable().getCode());
    }

    @Transactional
    public FoodOrderResponse order(Long id, CreateOrderRequest request) {
        // Serialize with the future payment workflow's session lock; no closing workflow is added
        // here.
        ServiceSession session =
                sessions.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy phiên phục vụ"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE, "Chỉ gọi món trong phiên đang hoạt động");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vui lòng chọn ít nhất một món");
        }
        if (request.items().size() > OrderLimits.MAX_LINES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Mỗi đợt gọi món tối đa 50 dòng");
        }
        for (OrderLineRequest line : request.items()) {
            if (line == null
                    || line.menuItemId() == null
                    || line.quantity() == null
                    || line.quantity() < 1
                    || line.quantity() > OrderLimits.MAX_QUANTITY_PER_LINE) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Mỗi dòng cần chọn món và số lượng từ 1 đến 99 phần");
            }
        }
        List<Long> menuIds =
                request.items().stream().map(OrderLineRequest::menuItemId).distinct().toList();
        Map<Long, MenuItem> choices =
                menu.findAllById(menuIds).stream()
                        .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        var order =
                new FoodOrder(
                        session,
                        accounts.getReferenceById(currentUser.accountId()),
                        clock.instant());
        for (OrderLineRequest line : request.items()) {
            MenuItem item = choices.get(line.menuItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy món đã chọn");
            }
            if (item.getStatus() != MenuItemStatus.ON_SALE) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE, "Món " + item.getName() + " đã ngừng kinh doanh");
            }
            order.addItem(new OrderItem(item, line.quantity(), line.note()));
        }
        orders.saveAndFlush(order);
        events.publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, session.getId()));
        return FoodOrderMapper.order(order);
    }

    @Transactional
    public OrderItemResponse cancel(Long id) {
        OrderItem item = lockedItem(id);
        OrderItemStatus before = item.getStatus();
        item.cancel();
        publishIfChanged(item, before);
        return FoodOrderMapper.item(item);
    }

    @Transactional
    public OrderItemResponse serve(Long id) {
        OrderItem item = lockedItem(id);
        OrderItemStatus before = item.getStatus();
        item.markServed();
        publishIfChanged(item, before);
        return FoodOrderMapper.item(item);
    }

    @Transactional(readOnly = true)
    public List<OrderQueueItemResponse> awaitingService() {
        return items
                .findQueue(
                        SessionStatus.ACTIVE,
                        Set.of(OrderItemStatus.READY, OrderItemStatus.UNAVAILABLE))
                .stream()
                .map(FoodOrderMapper::queueItem)
                .toList();
    }

    @Transactional
    public OrderItemResponse acknowledgeUnavailable(Long id) {
        OrderItem item = lockedItem(id);
        if (item.acknowledgeUnavailable(
                accounts.getReferenceById(currentUser.accountId()), clock.instant()))
            events.publishEvent(
                    new RealtimeEvent(
                            RealtimeEvent.Type.ORDER, item.getOrder().getSession().getId()));
        return FoodOrderMapper.item(item);
    }

    private OrderItem lockedItem(Long id) {
        Long sessionId =
                items.findSessionIdById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy món trong đơn"));
        // Keep this order consistent with kitchen mutations and the future payment workflow.
        ServiceSession session =
                sessions.findLockedById(sessionId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy phiên phục vụ"));
        OrderItem item =
                items.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy món trong đơn"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE, "Không thể cập nhật món của phiên đã kết thúc");
        }
        return item;
    }

    private void publishIfChanged(OrderItem item, OrderItemStatus before) {
        if (before != item.getStatus()) {
            events.publishEvent(
                    new RealtimeEvent(
                            RealtimeEvent.Type.ORDER, item.getOrder().getSession().getId()));
        }
    }
}
