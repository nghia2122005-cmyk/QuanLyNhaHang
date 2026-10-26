package vn.ueh.kltn.restaurant.service.impl;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateIngredientRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockDisposalRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockReceiptRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStocktakeRequest;
import vn.ueh.kltn.restaurant.dto.request.StockReceiptItemRequest;
import vn.ueh.kltn.restaurant.dto.request.StocktakeItemRequest;
import vn.ueh.kltn.restaurant.dto.request.UpdateIngredientRequest;
import vn.ueh.kltn.restaurant.dto.response.IngredientLotAttentionResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientLotResponse;
import vn.ueh.kltn.restaurant.dto.response.IngredientResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.StockMovementResponse;
import vn.ueh.kltn.restaurant.dto.response.StockReceiptResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeResponse;
import vn.ueh.kltn.restaurant.dto.response.StocktakeTemplateItemResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.IngredientLot;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.entity.StockReceipt;
import vn.ueh.kltn.restaurant.entity.StockReceiptItem;
import vn.ueh.kltn.restaurant.entity.Stocktake;
import vn.ueh.kltn.restaurant.entity.StocktakeItem;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.InventoryMapper;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.IngredientLotRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.StockMovementRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptItemRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptRepository;
import vn.ueh.kltn.restaurant.repository.StocktakeItemRepository;
import vn.ueh.kltn.restaurant.repository.StocktakeRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.InventoryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final IngredientRepository ingredients;
    private final IngredientLotRepository ingredientLots;
    private final StockMovementRepository stockMovements;
    private final StockReceiptRepository stockReceipts;
    private final StockReceiptItemRepository stockReceiptItems;
    private final StocktakeRepository stocktakes;
    private final StocktakeItemRepository stocktakeItems;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final AppProperties properties;
    private final OrderItemRepository orderItems;
    private final CookingInventory cookingInventory;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<IngredientResponse> listIngredients(
            String q, IngredientStatus status, Boolean belowMinStockOnly) {
        Specification<Ingredient> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (status != null) {
                        predicates.add(cb.equal(root.get("status"), status));
                    }
                    if (q != null && !q.isBlank()) {
                        String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                        predicates.add(cb.like(cb.lower(root.get("name")), term));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                };

        List<Ingredient> list = ingredients.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));
        Map<Long, LotMetadata> lotMetadata =
                lotMetadata(list.stream().map(Ingredient::getId).toList());
        Map<Long, BigDecimal> stockMap = getStockMapGrouped();
        Map<Long, BigDecimal> reservedMap = new HashMap<>();
        for (Object[] row : orderItems.reservedQuantities())
            reservedMap.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);

        List<IngredientResponse> results = new ArrayList<>();
        for (Ingredient ing : list) {
            BigDecimal currentStock = stockMap.getOrDefault(ing.getId(), BigDecimal.ZERO);
            BigDecimal reserved = reservedMap.getOrDefault(ing.getId(), BigDecimal.ZERO);
            boolean isBelow = ing.isBelowMinStock(currentStock.subtract(reserved));
            if (Boolean.TRUE.equals(belowMinStockOnly) && !isBelow) {
                continue;
            }
            LotMetadata metadata = lotMetadata.getOrDefault(ing.getId(), new LotMetadata(null, 0));
            results.add(
                    InventoryMapper.toIngredientResponse(
                            ing,
                            currentStock,
                            reserved,
                            metadata.nearestExpiryDate(),
                            metadata.count()));
        }

        return results;
    }

    private record LotMetadata(LocalDate nearestExpiryDate, long count) {}

    private Map<Long, LotMetadata> lotMetadata(List<Long> ids) {
        Map<Long, LotMetadata> result = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : ingredientLots.summarizeByIngredientIds(ids)) {
                result.put(
                        ((Number) row[0]).longValue(),
                        new LotMetadata((LocalDate) row[2], ((Number) row[1]).longValue()));
            }
        }
        return result;
    }

    private IngredientResponse ingredientResponse(
            Ingredient ingredient, BigDecimal stock, BigDecimal reserved) {
        LotMetadata metadata =
                lotMetadata(List.of(ingredient.getId()))
                        .getOrDefault(ingredient.getId(), new LotMetadata(null, 0));
        return InventoryMapper.toIngredientResponse(
                ingredient, stock, reserved, metadata.nearestExpiryDate(), metadata.count());
    }

    @Override
    @Transactional
    public IngredientResponse createIngredient(CreateIngredientRequest request) {
        String name = request.name().trim();
        if (ingredients.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Tên nguyên vật liệu đã tồn tại");
        }

        Ingredient ingredient =
                new Ingredient(
                        name,
                        request.unit().trim(),
                        request.minStock(),
                        request.trackLot(),
                        request.trackExpiry());
        Ingredient saved = ingredients.save(ingredient);
        return InventoryMapper.toIngredientResponse(saved, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request) {
        Ingredient ingredient =
                ingredients
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy nguyên vật liệu"));

        String newName = request.name().trim();
        if (ingredients.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Tên nguyên vật liệu đã tồn tại");
        }

        String newUnit = request.unit().trim();
        if (!ingredient.getUnit().equalsIgnoreCase(newUnit)) {
            if (stockMovements.existsByIngredientId(id)
                    || orderItems.reservedQuantity(id, null).signum() > 0) {
                throw new BusinessException(
                        ErrorCode.UNIT_CHANGE_BLOCKED,
                        "Không thể thay đổi đơn vị tính của nguyên vật liệu đã có biến"
                                + " động kho");
            }
        }

        ingredient.updateInfo(
                newName, newUnit, request.minStock(), request.trackLot(), request.trackExpiry());

        BigDecimal currentStock = stockMovements.sumQuantityByIngredientId(id);
        return ingredientResponse(ingredient, currentStock, orderItems.reservedQuantity(id, null));
    }

    @Override
    @Transactional
    public IngredientResponse deactivateIngredient(Long id) {
        Ingredient ingredient =
                ingredients
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy nguyên vật liệu"));
        ingredient.deactivate();
        BigDecimal currentStock = stockMovements.sumQuantityByIngredientId(id);
        return ingredientResponse(ingredient, currentStock, orderItems.reservedQuantity(id, null));
    }

    @Override
    @Transactional
    public IngredientResponse activateIngredient(Long id) {
        Ingredient ingredient =
                ingredients
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy nguyên vật liệu"));
        ingredient.activate();
        BigDecimal currentStock = stockMovements.sumQuantityByIngredientId(id);
        return ingredientResponse(ingredient, currentStock, orderItems.reservedQuantity(id, null));
    }

    @Override
    public List<IngredientLotAttentionResponse> listAttentionLots(Integer days) {
        int alertDays = alertDays(days);
        ZoneId zone = ZoneId.of(properties.timezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate thresholdDate = today.plusDays(alertDays);

        List<IngredientLot> lots = ingredientLots.findAttentionLots(thresholdDate);
        if (lots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> lotIds = lots.stream().map(IngredientLot::getId).toList();
        List<Object[]> initialRows = stockReceiptItems.findInitialQuantitiesByLotIds(lotIds);
        Map<Long, BigDecimal> initialMap = new HashMap<>();
        for (Object[] row : initialRows) {
            initialMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<IngredientLotAttentionResponse> results = new ArrayList<>();
        for (IngredientLot lot : lots) {
            BigDecimal initialQty = initialMap.getOrDefault(lot.getId(), BigDecimal.ZERO);
            results.add(InventoryMapper.toLotAttentionResponse(lot, initialQty, today));
        }

        return results;
    }

    private int alertDays(Integer days) {
        if (days != null && (days < 0 || days > 90)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "Số ngày cảnh báo phải từ 0 đến 90");
        }
        return days != null ? days : properties.alert().expiryDays();
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<IngredientLotResponse> listIngredientLots(Long ingredientId, Integer days) {
        int alertDays = alertDays(days);
        if (!ingredients.existsById(ingredientId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên vật liệu");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(properties.timezone())));
        List<IngredientLot> lots =
                ingredientLots.findByIngredientIdOrderByExpiryDateAscIdAsc(ingredientId);
        if (lots.isEmpty()) return List.of();
        Map<Long, BigDecimal> quantities = new HashMap<>();
        for (Object[] row :
                stockReceiptItems.findInitialQuantitiesByLotIds(
                        lots.stream().map(IngredientLot::getId).toList())) {
            quantities.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }
        return lots.stream()
                .map(
                        lot ->
                                InventoryMapper.toLotResponse(
                                        lot, quantities.get(lot.getId()), today, alertDays))
                .toList();
    }

    @Override
    @Transactional
    public StockReceiptResponse createReceipt(CreateStockReceiptRequest request) {
        Account actor =
                accounts.findById(currentUser.accountId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return createReceiptWithActor(request, actor);
    }

    @Override
    @Transactional
    public StockReceiptResponse createReceiptForSeed(
            CreateStockReceiptRequest request, Account actor) {
        return createReceiptWithActor(request, actor);
    }

    private StockReceiptResponse createReceiptWithActor(
            CreateStockReceiptRequest request, Account actor) {
        cookingInventory.lock(
                request.items().stream().map(StockReceiptItemRequest::ingredientId).toList());
        Instant now = clock.instant();

        StockReceipt receipt = new StockReceipt(actor, now);
        StockReceipt savedReceipt = stockReceipts.save(receipt);

        for (StockReceiptItemRequest itemReq : request.items()) {
            String lotNumber = itemReq.lotNumber() != null ? itemReq.lotNumber().strip() : "";
            if (lotNumber.isBlank() || itemReq.lotNumber().length() > 50) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Số lô không được để trống và không vượt quá 50 ký tự");
            }
            Ingredient ingredient =
                    ingredients
                            .findLockedById(itemReq.ingredientId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND,
                                                    "Không tìm thấy nguyên vật liệu"));

            if (ingredient.getStatus() != IngredientStatus.ACTIVE) {
                throw new BusinessException(
                        ErrorCode.INGREDIENT_INACTIVE, "Nguyên vật liệu đã ngừng sử dụng");
            }

            if (itemReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_QUANTITY, "Số lượng nhập phải lớn hơn 0");
            }

            if (ingredient.isTrackExpiry() && itemReq.expiryDate() == null) {
                throw new BusinessException(
                        ErrorCode.EXPIRY_DATE_REQUIRED,
                        "Nguyên vật liệu có theo dõi hạn sử dụng bắt buộc phải nhập hạn sử"
                                + " dụng");
            }

            // Each receipt line is a distinct intake lot; supplier numbers may repeat.
            IngredientLot lot =
                    ingredientLots.save(
                            new IngredientLot(ingredient, lotNumber, itemReq.expiryDate()));

            StockReceiptItem item = new StockReceiptItem(ingredient, lot, itemReq.quantity());
            savedReceipt.addItem(item);
            StockReceiptItem savedItem = stockReceiptItems.save(item);

            StockMovement movement =
                    new StockMovement(
                            ingredient,
                            lot,
                            savedItem,
                            null,
                            null,
                            actor,
                            MovementType.RECEIPT,
                            itemReq.quantity(),
                            null,
                            now);
            stockMovements.save(movement);
        }

        return InventoryMapper.toReceiptResponse(savedReceipt);
    }

    @Override
    public PageResponse<StockReceiptResponse> listReceipts(
            Instant from, Instant to, int page, int size) {
        Specification<StockReceipt> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (from != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                    }
                    if (to != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                };

        PageRequest pageRequest =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<StockReceipt> receiptPage = stockReceipts.findAll(spec, pageRequest);
        if (receiptPage.isEmpty())
            return PageResponse.from(receiptPage.map(InventoryMapper::toReceiptResponse));
        List<Long> ids = receiptPage.getContent().stream().map(StockReceipt::getId).toList();
        Map<Long, StockReceipt> details = new HashMap<>();
        stockReceipts
                .findDetailsByIdIn(ids)
                .forEach(receipt -> details.put(receipt.getId(), receipt));
        return PageResponse.from(
                receiptPage.map(
                        receipt ->
                                InventoryMapper.toReceiptResponse(details.get(receipt.getId()))));
    }

    @Override
    public List<StocktakeTemplateItemResponse> getStocktakeTemplate() {
        List<Ingredient> activeIngredients =
                ingredients.findByStatusOrderByNameAsc(IngredientStatus.ACTIVE);
        Map<Long, BigDecimal> stockMap = getStockMapGrouped();

        List<StocktakeTemplateItemResponse> results = new ArrayList<>();
        for (Ingredient ing : activeIngredients) {
            BigDecimal currentStock = stockMap.getOrDefault(ing.getId(), BigDecimal.ZERO);
            results.add(
                    new StocktakeTemplateItemResponse(
                            ing.getId(), ing.getName(), ing.getUnit(), currentStock));
        }

        return results;
    }

    @Override
    @Transactional
    public StocktakeResponse createStocktake(CreateStocktakeRequest request) {
        Account actor =
                accounts.findById(currentUser.accountId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return createStocktakeWithActor(request, actor);
    }

    @Override
    @Transactional
    public StocktakeResponse createStocktakeForSeed(CreateStocktakeRequest request, Account actor) {
        return createStocktakeWithActor(request, actor);
    }

    private StocktakeResponse createStocktakeWithActor(
            CreateStocktakeRequest request, Account actor) {
        Set<Long> ingredientIds = new TreeSet<>();
        for (StocktakeItemRequest item : request.items()) {
            if (!ingredientIds.add(item.ingredientId())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STOCKTAKE,
                        "Nguyên vật liệu bị trùng trong phiếu kiểm kê");
            }
        }
        // Acquire every parent lock in a stable order before reading any ledger sum.
        Map<Long, Ingredient> lockedIngredients = new HashMap<>();
        for (Long ingredientId : ingredientIds) {
            Ingredient ingredient =
                    ingredients
                            .findLockedById(ingredientId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND,
                                                    "Không tìm thấy nguyên vật liệu"));
            if (ingredient.getStatus() != IngredientStatus.ACTIVE) {
                throw new BusinessException(
                        ErrorCode.INGREDIENT_INACTIVE, "Nguyên vật liệu đã ngừng sử dụng");
            }
            lockedIngredients.put(ingredientId, ingredient);
        }
        Instant now = clock.instant();

        Stocktake stocktake = new Stocktake(actor, now);
        Stocktake savedStocktake = stocktakes.save(stocktake);

        for (StocktakeItemRequest itemReq : request.items()) {
            Ingredient ingredient = lockedIngredients.get(itemReq.ingredientId());

            if (itemReq.actualQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_STOCKTAKE, "Số lượng thực tế không được âm");
            }

            if (itemReq.actualQuantity()
                            .compareTo(orderItems.reservedQuantity(ingredient.getId(), null))
                    < 0)
                throw new BusinessException(
                        ErrorCode.INVALID_STOCKTAKE,
                        "Số thực đếm không được thấp hơn lượng đang giữ cho món chế biến");

            String note =
                    itemReq.note() != null && !itemReq.note().isBlank()
                            ? itemReq.note().trim()
                            : null;

            // D7: Recompute ledger sum at confirmation time
            BigDecimal systemQuantity =
                    stockMovements.sumQuantityByIngredientId(ingredient.getId());

            StocktakeItem item =
                    new StocktakeItem(ingredient, systemQuantity, itemReq.actualQuantity(), note);
            savedStocktake.addItem(item);
            StocktakeItem savedItem = stocktakeItems.save(item);

            BigDecimal difference = item.calculateDifference();
            if (difference.compareTo(BigDecimal.ZERO) != 0) {
                StockMovement movement =
                        new StockMovement(
                                ingredient,
                                null,
                                null,
                                null,
                                savedItem,
                                actor,
                                MovementType.STOCKTAKE_ADJUSTMENT,
                                difference,
                                note,
                                now);
                stockMovements.save(movement);
            }
        }

        return InventoryMapper.toStocktakeResponse(savedStocktake);
    }

    @Override
    public PageResponse<StocktakeResponse> listStocktakes(
            Instant from, Instant to, int page, int size) {
        Specification<Stocktake> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (from != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                    }
                    if (to != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                };

        PageRequest pageRequest =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Stocktake> stocktakePage = stocktakes.findAll(spec, pageRequest);
        if (stocktakePage.isEmpty())
            return PageResponse.from(stocktakePage.map(InventoryMapper::toStocktakeResponse));
        List<Long> ids = stocktakePage.getContent().stream().map(Stocktake::getId).toList();
        Map<Long, Stocktake> details = new HashMap<>();
        stocktakes
                .findDetailsByIdIn(ids)
                .forEach(stocktake -> details.put(stocktake.getId(), stocktake));
        return PageResponse.from(
                stocktakePage.map(
                        stocktake ->
                                InventoryMapper.toStocktakeResponse(
                                        details.get(stocktake.getId()))));
    }

    @Override
    @Transactional
    public StockMovementResponse createDisposal(CreateStockDisposalRequest request) {
        Ingredient ingredient =
                ingredients
                        .findLockedById(request.ingredientId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Không tìm thấy nguyên vật liệu"));

        if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_DISPOSAL, "Số lượng xuất hủy phải lớn hơn 0");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vui lòng nhập lý do xuất hủy");
        }

        IngredientLot lot = null;
        if (request.lotId() != null) {
            lot =
                    ingredientLots
                            .findById(request.lotId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.LOT_NOT_FOUND,
                                                    "Không tìm thấy lô nguyên vật liệu"));
            if (!lot.getIngredient().getId().equals(ingredient.getId())) {
                throw new BusinessException(
                        ErrorCode.LOT_INGREDIENT_MISMATCH,
                        "Lô nguyên vật liệu không thuộc về nguyên vật liệu đã chọn");
            }
        }

        BigDecimal totalStock = stockMovements.sumQuantityByIngredientId(ingredient.getId());
        if (request.quantity()
                        .compareTo(
                                totalStock.subtract(
                                        orderItems.reservedQuantity(ingredient.getId(), null)))
                > 0) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK, "Số lượng tồn kho không đủ để xuất hủy");
        }

        Account actor =
                accounts.findById(currentUser.accountId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Instant now = clock.instant();

        StockMovement movement =
                new StockMovement(
                        ingredient,
                        lot,
                        null,
                        null,
                        null,
                        actor,
                        MovementType.DISPOSAL,
                        request.quantity().negate(),
                        request.reason().trim(),
                        now);
        StockMovement savedMovement = stockMovements.save(movement);

        return InventoryMapper.toMovementResponse(savedMovement);
    }

    @Override
    public PageResponse<StockMovementResponse> listMovements(
            Long ingredientId,
            MovementType movementType,
            Instant from,
            Instant to,
            int page,
            int size) {
        Specification<StockMovement> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (ingredientId != null) {
                        predicates.add(cb.equal(root.get("ingredient").get("id"), ingredientId));
                    }
                    if (movementType != null) {
                        predicates.add(cb.equal(root.get("movementType"), movementType));
                    }
                    if (from != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
                    }
                    if (to != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                };

        PageRequest pageRequest =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        Page<StockMovement> movementPage = stockMovements.findAll(spec, pageRequest);

        return PageResponse.from(movementPage.map(InventoryMapper::toMovementResponse));
    }

    private Map<Long, BigDecimal> getStockMapGrouped() {
        List<Object[]> rows = stockMovements.sumQuantityGroupedByIngredient();
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }
}
