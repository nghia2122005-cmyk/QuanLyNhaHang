package vn.ueh.kltn.restaurant.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.response.CookingStartResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientAvailabilityResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderItemResponse;
import vn.ueh.kltn.restaurant.dto.response.OrderQueueItemResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.IngredientRequirement;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.exception.InventoryAvailabilityException;
import vn.ueh.kltn.restaurant.mapper.FoodOrderMapper;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.repository.StockMovementRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.KitchenService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KitchenServiceImpl implements KitchenService {
    private final OrderItemRepository items;
    private final RecipeItemRepository recipes;
    private final StockMovementRepository movements;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final ServiceSessionRepository sessions;
    private final MenuItemRepository menu;
    private final CookingInventory inventory;

    public List<OrderQueueItemResponse> queue() {
        return items
                .findQueue(
                        SessionStatus.ACTIVE,
                        Set.of(OrderItemStatus.PENDING, OrderItemStatus.COOKING))
                .stream()
                .map(FoodOrderMapper::queueItem)
                .toList();
    }

    public List<OrderQueueItemResponse> batch(Long orderId) {
        List<OrderItem> batch = items.findKitchenBatch(orderId);
        if (batch.isEmpty())
            throw new BusinessException(
                    ErrorCode.NOT_FOUND, "Không tìm thấy đợt gọi món đang hoạt động");
        return batch.stream().map(FoodOrderMapper::queueItem).toList();
    }

    @Transactional
    public CookingStartResponse start(Long id, boolean confirmLowStock) {
        OrderItem item = locked(id);
        if (item.getStatus() == OrderItemStatus.COOKING && item.getIngredientRequirements() != null)
            return new CookingStartResponse(true, FoodOrderMapper.item(item), List.of());
        if (item.getStatus() != OrderItemStatus.PENDING
                && item.getStatus() != OrderItemStatus.COOKING)
            throw new BusinessException(ErrorCode.INVALID_STATE);
        List<IngredientRequirement> requirements = requirements(item);
        Map<Long, Ingredient> locked =
                inventory.lock(
                        requirements.stream().map(IngredientRequirement::ingredientId).toList());
        List<IngredientAvailabilityResponse> check = inventory.inspect(item, requirements, locked);
        requireEnough(check);
        if (!confirmLowStock
                && check.stream()
                        .anyMatch(line -> line.projectedStock().compareTo(line.minimumStock()) < 0))
            return new CookingStartResponse(false, FoodOrderMapper.item(item), check);
        item.captureRequirements(requirements);
        item.startCooking();
        events.publishEvent(
                new RealtimeEvent(RealtimeEvent.Type.ORDER, item.getOrder().getSession().getId()));
        return new CookingStartResponse(true, FoodOrderMapper.item(item), check);
    }

    @Transactional
    public OrderItemResponse unavailable(Long id, String reason) {
        OrderItem item = locked(id);
        OrderItemStatus before = item.getStatus();
        if (before == OrderItemStatus.COOKING && item.getIngredientRequirements() != null)
            inventory.lock(
                    item.getIngredientRequirements().stream()
                            .map(IngredientRequirement::ingredientId)
                            .toList());
        item.markUnavailable(reason);
        publishIfChanged(item, before);
        return FoodOrderMapper.item(item);
    }

    @Transactional
    public OrderItemResponse complete(Long id) {
        OrderItem item = locked(id);
        if (item.getStatus() == OrderItemStatus.READY || item.getStatus() == OrderItemStatus.SERVED)
            return FoodOrderMapper.item(item);
        if (item.getStatus() != OrderItemStatus.COOKING)
            throw new BusinessException(
                    ErrorCode.INVALID_STATE, "Chỉ hoàn thành món đang chế biến");
        // Legacy COOKING rows have no snapshot. Validate/capture on first touch, never invent
        // history.
        List<IngredientRequirement> requirements = requirements(item);
        Map<Long, Ingredient> locked =
                inventory.lock(
                        requirements.stream().map(IngredientRequirement::ingredientId).toList());
        requireEnough(inventory.inspect(item, requirements, locked));
        item.captureRequirements(requirements);
        Account performer = accounts.getReferenceById(currentUser.accountId());
        Instant now = clock.instant();
        List<StockMovement> consumption =
                requirements.stream()
                        .map(
                                line ->
                                        new StockMovement(
                                                locked.get(line.ingredientId()),
                                                null,
                                                null,
                                                item,
                                                null,
                                                performer,
                                                MovementType.CONSUMPTION,
                                                line.quantity().negate(),
                                                null,
                                                now))
                        .toList();
        movements.saveAll(consumption);
        item.markReady();
        events.publishEvent(
                new RealtimeEvent(RealtimeEvent.Type.ORDER, item.getOrder().getSession().getId()));
        return FoodOrderMapper.item(item);
    }

    private List<IngredientRequirement> requirements(OrderItem item) {
        if (item.getIngredientRequirements() != null) return item.getIngredientRequirements();
        menu.findLockedById(item.getMenuItem().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<RecipeItem> recipe = recipes.findByMenuItemId(item.getMenuItem().getId());
        List<IngredientRequirement> result =
                recipe.stream()
                        .map(
                                line ->
                                        new IngredientRequirement(
                                                line.getIngredient().getId(),
                                                line.getIngredient().getUnit(),
                                                line.calculateConsumption(item.getQuantity())))
                        .toList();
        if (result.stream()
                .anyMatch(
                        line ->
                                line.quantity().signum() <= 0
                                        || line.quantity()
                                                        .compareTo(new BigDecimal("999999999.999"))
                                                > 0))
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "Lượng nguyên liệu chế biến vượt giới hạn lưu trữ");
        return result;
    }

    private void requireEnough(List<IngredientAvailabilityResponse> check) {
        if (check.stream().anyMatch(line -> line.projectedStock().signum() < 0))
            throw new InventoryAvailabilityException(check);
    }

    private OrderItem locked(Long id) {
        Long sessionId =
                items.findSessionIdById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy món trong đơn"));
        // All item mutations and payment must use the same Session -> OrderItem lock order.
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
