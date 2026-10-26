package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.account;
import static vn.ueh.kltn.restaurant.TestFixtures.id;
import static vn.ueh.kltn.restaurant.TestFixtures.ingredient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.IngredientLot;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.entity.StockReceipt;
import vn.ueh.kltn.restaurant.entity.StockReceiptItem;
import vn.ueh.kltn.restaurant.entity.Stocktake;
import vn.ueh.kltn.restaurant.entity.StocktakeItem;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InventoryServiceImplTest {

    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private final IngredientLotRepository ingredientLots = mock(IngredientLotRepository.class);
    private final StockMovementRepository stockMovements = mock(StockMovementRepository.class);
    private final StockReceiptRepository stockReceipts = mock(StockReceiptRepository.class);
    private final StockReceiptItemRepository stockReceiptItems =
            mock(StockReceiptItemRepository.class);
    private final StocktakeRepository stocktakes = mock(StocktakeRepository.class);
    private final StocktakeItemRepository stocktakeItems = mock(StocktakeItemRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);

    private final AppProperties properties =
            new AppProperties(
                    "Asia/Ho_Chi_Minh",
                    false,
                    new AppProperties.Jwt("test-secret-for-phase-three-only-32", 12),
                    new AppProperties.Alert(7),
                    new AppProperties.Reservation(60));

    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final CookingInventory cookingInventory = mock(CookingInventory.class);
    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        when(orderItems.reservedQuantity(any(), any())).thenReturn(BigDecimal.ZERO);
        service =
                new InventoryServiceImpl(
                        ingredients,
                        ingredientLots,
                        stockMovements,
                        stockReceipts,
                        stockReceiptItems,
                        stocktakes,
                        stocktakeItems,
                        accounts,
                        currentUser,
                        CLOCK,
                        properties,
                        orderItems,
                        cookingInventory);
    }

    @Test
    void listIngredientsCalculatesStockAndFiltersBelowMin() {
        Ingredient ing1 =
                id(new Ingredient("Thịt bò", "kg", new BigDecimal("5"), false, false), 1L);
        Ingredient ing2 =
                id(new Ingredient("Rau thơm", "kg", new BigDecimal("1"), false, false), 2L);

        when(ingredients.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ing1, ing2));

        // Grouped query returns: ing1 has 10 kg, ing2 has 0.5 kg (below min)
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {1L, new BigDecimal("10.000")});
        rows.add(new Object[] {2L, new BigDecimal("0.500")});
        when(stockMovements.sumQuantityGroupedByIngredient()).thenReturn(rows);

        List<IngredientResponse> all = service.listIngredients(null, null, false);
        assertEquals(2, all.size());
        assertEquals(new BigDecimal("10.000"), all.get(0).currentStock());
        assertFalse(all.get(0).belowMinStock());
        assertEquals(new BigDecimal("0.500"), all.get(1).currentStock());
        assertTrue(all.get(1).belowMinStock());

        List<IngredientResponse> belowMinOnly = service.listIngredients(null, null, true);
        assertEquals(1, belowMinOnly.size());
        assertEquals("Rau thơm", belowMinOnly.get(0).name());
    }

    @Test
    void ingredientMetadataIsGroupedAndDoesNotChangeStockOrReservations() {
        Ingredient ing = ingredient(1L);
        when(ingredients.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ing));
        when(ingredientLots.summarizeByIngredientIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, 3L, LocalDate.of(2020, 1, 1)}));
        when(stockMovements.sumQuantityGroupedByIngredient())
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("10")}));
        when(orderItems.reservedQuantities())
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("2")}));
        IngredientResponse response = service.listIngredients(null, null, false).getFirst();
        assertEquals(LocalDate.of(2020, 1, 1), response.nearestExpiryDate());
        assertEquals(3, response.lotCount());
        assertEquals(new BigDecimal("10"), response.currentStock());
        assertEquals(new BigDecimal("2"), response.reservedStock());
        assertEquals(new BigDecimal("8"), response.availableStock());
        verify(ingredientLots).summarizeByIngredientIds(List.of(1L));
    }

    @Test
    void allLotsIncludeMissingExpiryAndDatesOutsideTheWarningWindow() {
        Ingredient ing = ingredient(1L);
        LocalDate today = LocalDate.now(CLOCK);
        List<IngredientLot> lots =
                List.of(
                        id(new IngredientLot(ing, "PAST", today.minusDays(1)), 11L),
                        id(new IngredientLot(ing, "TODAY", today), 12L),
                        id(new IngredientLot(ing, "SOON", today.plusDays(7)), 13L),
                        id(new IngredientLot(ing, "LATER", today.plusDays(120)), 14L),
                        id(new IngredientLot(ing, null, null), 15L));
        when(ingredients.existsById(1L)).thenReturn(true);
        when(ingredientLots.findByIngredientIdOrderByExpiryDateAscIdAsc(1L)).thenReturn(lots);
        when(stockReceiptItems.findInitialQuantitiesByLotIds(List.of(11L, 12L, 13L, 14L, 15L)))
                .thenReturn(List.<Object[]>of(new Object[] {11L, new BigDecimal("6.500")}));
        List<IngredientLotResponse> result = service.listIngredientLots(1L, 7);
        assertEquals(
                List.of("EXPIRED", "EXPIRING_SOON", "EXPIRING_SOON", "VALID", "NO_EXPIRY"),
                result.stream().map(IngredientLotResponse::status).toList());
        assertEquals(new BigDecimal("6.500"), result.getFirst().initialQuantity());
        assertEquals(0L, result.get(1).daysRemaining());
        assertEquals(null, result.getLast().daysRemaining());
        verify(stockReceiptItems).findInitialQuantitiesByLotIds(List.of(11L, 12L, 13L, 14L, 15L));
    }

    @Test
    void lotListingDistinguishesMissingIngredientAndEmptyHistory() {
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.listIngredientLots(1L, 7))
                        .getErrorCode());
        when(ingredients.existsById(1L)).thenReturn(true);
        assertTrue(service.listIngredientLots(1L, 7).isEmpty());
        verifyNoInteractions(stockReceiptItems);
    }

    @Test
    void warningDaysAreValidatedInTheServiceAndRespectConfigurationWhenOmitted() {
        for (int days : List.of(-1, 91, Integer.MAX_VALUE)) {
            assertEquals(
                    ErrorCode.INVALID_INPUT,
                    assertThrows(BusinessException.class, () -> service.listAttentionLots(days))
                            .getErrorCode());
            assertThrows(BusinessException.class, () -> service.listIngredientLots(1L, days));
        }
        LocalDate today = LocalDate.now(CLOCK);
        service.listAttentionLots(0);
        service.listAttentionLots(90);
        service.listAttentionLots(null);
        verify(ingredientLots).findAttentionLots(today);
        verify(ingredientLots).findAttentionLots(today.plusDays(90));
        verify(ingredientLots).findAttentionLots(today.plusDays(properties.alert().expiryDays()));
    }

    @Test
    void expiryThresholdChangesAtConfiguredVietnamMidnight() {
        for (String instant : List.of("2026-10-25T16:59:59Z", "2026-10-25T17:00:00Z")) {
            Clock testClock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
            InventoryServiceImpl atTime =
                    new InventoryServiceImpl(
                            ingredients,
                            ingredientLots,
                            stockMovements,
                            stockReceipts,
                            stockReceiptItems,
                            stocktakes,
                            stocktakeItems,
                            accounts,
                            currentUser,
                            testClock,
                            properties,
                            orderItems,
                            cookingInventory);
            atTime.listAttentionLots(7);
        }
        verify(ingredientLots).findAttentionLots(LocalDate.of(2026, 11, 1));
        verify(ingredientLots).findAttentionLots(LocalDate.of(2026, 11, 2));
    }

    @Test
    void receiptRejectsMissingBlankAndOversizedLotNumbersEvenOutsideControllerValidation() {
        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));
        for (String lotNumber : new String[] {null, "", " \t\n ", "X".repeat(51)}) {
            assertEquals(
                    ErrorCode.INVALID_INPUT,
                    assertThrows(
                                    BusinessException.class,
                                    () ->
                                            service.createReceipt(
                                                    new CreateStockReceiptRequest(
                                                            List.of(
                                                                    new StockReceiptItemRequest(
                                                                            1L,
                                                                            BigDecimal.ONE,
                                                                            lotNumber,
                                                                            null)))))
                            .getErrorCode());
        }
        verifyNoInteractions(ingredientLots, stockReceiptItems, stockMovements);
    }

    @Test
    void receiptAlwaysCreatesDistinctLotsAndTrimsNumbersWhenBothTrackingFlagsAreFalse() {
        Ingredient ing = id(new Ingredient("Gạo", "kg", BigDecimal.ZERO, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));
        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockReceipts.save(any(StockReceipt.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(ingredientLots.save(any(IngredientLot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockReceiptItems.save(any(StockReceiptItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        StockReceiptResponse receipt =
                service.createReceipt(
                        new CreateStockReceiptRequest(
                                List.of(
                                        new StockReceiptItemRequest(
                                                1L, BigDecimal.ONE, "  SAME  ", null),
                                        new StockReceiptItemRequest(
                                                1L, BigDecimal.TEN, "SAME", null))));
        assertEquals(2, receipt.items().size());
        assertTrue(receipt.items().stream().allMatch(item -> "SAME".equals(item.lotNumber())));
        assertEquals(null, receipt.items().getFirst().expiryDate());
        verify(ingredientLots, times(2)).save(any(IngredientLot.class));
    }

    @Test
    void createIngredientRejectsDuplicateName() {
        when(ingredients.existsByNameIgnoreCase("Thịt bò")).thenReturn(true);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createIngredient(
                                        new CreateIngredientRequest(
                                                "Thịt bò", "kg", BigDecimal.ONE, false, false)));

        assertEquals(ErrorCode.DATA_CONFLICT, ex.getErrorCode());
    }

    @Test
    void updateIngredientEnforcesD12UnitCannotChangeWhenMovementExists() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));
        when(ingredients.existsByNameIgnoreCaseAndIdNot("Thịt bò", 1L)).thenReturn(false);

        // Movements exist for this ingredient!
        when(stockMovements.existsByIngredientId(1L)).thenReturn(true);

        // Attempting to change unit from "kg" to "g"
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.updateIngredient(
                                        1L,
                                        new UpdateIngredientRequest(
                                                "Thịt bò", "g", BigDecimal.ONE, false, false)));

        assertEquals(ErrorCode.UNIT_CHANGE_BLOCKED, ex.getErrorCode());
    }

    @Test
    void updateIngredientAllowsUnitChangeWhenNoMovementExists() {
        Ingredient ing = id(new Ingredient("Gia vị mới", "kg", BigDecimal.ONE, false, false), 2L);
        when(ingredients.findLockedById(2L)).thenReturn(Optional.of(ing));
        when(ingredients.existsByNameIgnoreCaseAndIdNot("Gia vị mới", 2L)).thenReturn(false);

        // No movements exist
        when(stockMovements.existsByIngredientId(2L)).thenReturn(false);
        when(stockMovements.sumQuantityByIngredientId(2L)).thenReturn(BigDecimal.ZERO);

        IngredientResponse resp =
                service.updateIngredient(
                        2L,
                        new UpdateIngredientRequest(
                                "Gia vị mới", "g", BigDecimal.TEN, false, false));

        assertEquals("g", resp.unit());
        assertEquals(new BigDecimal("10"), resp.minStock());
    }

    @Test
    void updateIngredientAllowsNameAndMinStockChangeEvenWhenMovementExists() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));
        when(ingredients.existsByNameIgnoreCaseAndIdNot("Thịt bò Úc", 1L)).thenReturn(false);

        // Movements exist
        when(stockMovements.existsByIngredientId(1L)).thenReturn(true);
        when(stockMovements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("15"));

        // Unit stays "kg"
        IngredientResponse resp =
                service.updateIngredient(
                        1L,
                        new UpdateIngredientRequest(
                                "Thịt bò Úc", "kg", new BigDecimal("3"), false, false));

        assertEquals("Thịt bò Úc", resp.name());
        assertEquals("kg", resp.unit());
        assertEquals(new BigDecimal("3"), resp.minStock());
    }

    @Test
    void createReceiptAtomicallyCreatesLotItemAndMovement() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, true, true), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));

        when(stockReceipts.save(any(StockReceipt.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(ingredientLots.save(any(IngredientLot.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 20L));
        when(stockReceiptItems.save(any(StockReceiptItem.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 30L));
        when(stockMovements.save(any(StockMovement.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 40L));

        LocalDate exp = LocalDate.of(2026, 11, 15);
        StockReceiptResponse resp =
                service.createReceipt(
                        new CreateStockReceiptRequest(
                                List.of(
                                        new StockReceiptItemRequest(
                                                1L, new BigDecimal("10.500"), "LOT-001", exp))));

        assertNotNull(resp);
        assertEquals(1, resp.items().size());
        assertEquals(new BigDecimal("10.500"), resp.items().get(0).quantity());
        assertEquals("LOT-001", resp.items().get(0).lotNumber());
        assertEquals(exp, resp.items().get(0).expiryDate());
        verify(stockMovements).save(any(StockMovement.class));
    }

    @Test
    void createReceiptRejectsInactiveIngredient() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, false, false), 1L);
        ing.deactivate();
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockReceipts.save(any(StockReceipt.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 10L));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createReceipt(
                                        new CreateStockReceiptRequest(
                                                List.of(
                                                        new StockReceiptItemRequest(
                                                                1L,
                                                                BigDecimal.ONE,
                                                                "INACTIVE",
                                                                null)))));

        assertEquals(ErrorCode.INGREDIENT_INACTIVE, ex.getErrorCode());
    }

    @Test
    void createReceiptRejectsMissingExpiryDateWhenTrackExpiryIsTrue() {
        Ingredient ing = id(new Ingredient("Sữa tươi", "hộp", BigDecimal.ONE, true, true), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockReceipts.save(any(StockReceipt.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 10L));

        // expiryDate is null
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createReceipt(
                                        new CreateStockReceiptRequest(
                                                List.of(
                                                        new StockReceiptItemRequest(
                                                                1L,
                                                                BigDecimal.ONE,
                                                                "LOT-1",
                                                                null)))));

        assertEquals(ErrorCode.EXPIRY_DATE_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createStocktakeCalculatesDifferenceUsingD7LedgerSumAndCreatesMovement() {
        Ingredient ing = id(new Ingredient("Trứng", "quả", BigDecimal.TEN, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));

        when(stocktakes.save(any(Stocktake.class))).thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(stocktakeItems.save(any(StocktakeItem.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 20L));

        // D7: System stock recomputed at confirmation time is 50.000
        when(stockMovements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("50.000"));

        // Actual count is 48.000 (diff = -2.000)
        StocktakeResponse resp =
                service.createStocktake(
                        new CreateStocktakeRequest(
                                List.of(
                                        new StocktakeItemRequest(
                                                1L, new BigDecimal("48.000"), "Vỡ 2 quả"))));

        assertNotNull(resp);
        assertEquals(1, resp.items().size());
        assertEquals(new BigDecimal("50.000"), resp.items().get(0).systemQuantity());
        assertEquals(new BigDecimal("48.000"), resp.items().get(0).actualQuantity());
        assertEquals(new BigDecimal("-2.000"), resp.items().get(0).difference());

        // Verifies adjustment movement is saved with negative difference
        verify(stockMovements).save(any(StockMovement.class));
    }

    @Test
    void createStocktakeCreatesNoMovementWhenDifferenceIsZero() {
        Ingredient ing = id(new Ingredient("Gạo", "kg", BigDecimal.TEN, false, false), 2L);
        when(ingredients.findLockedById(2L)).thenReturn(Optional.of(ing));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));

        when(stocktakes.save(any(Stocktake.class))).thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(stocktakeItems.save(any(StocktakeItem.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 21L));

        // System stock is 20.000, actual is 20.000 -> diff = 0
        when(stockMovements.sumQuantityByIngredientId(2L)).thenReturn(new BigDecimal("20.000"));

        StocktakeResponse resp =
                service.createStocktake(
                        new CreateStocktakeRequest(
                                List.of(
                                        new StocktakeItemRequest(
                                                2L, new BigDecimal("20.000"), null))));

        assertNotNull(resp);
        assertEquals(0, resp.items().get(0).difference().compareTo(BigDecimal.ZERO));

        // NO movement should be created when difference == 0
        verify(stockMovements, never()).save(any(StockMovement.class));
    }

    @Test
    void createDisposalLocksIngredientAndEnforcesAvailableStock() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));

        // Total stock is 5.000 kg
        when(stockMovements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("5.000"));

        // Attempting to dispose 8.000 kg (> 5.000 kg)
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createDisposal(
                                        new CreateStockDisposalRequest(
                                                1L,
                                                null,
                                                new BigDecimal("8.000"),
                                                "Hỏng do mất điện")));

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    void createDisposalSavesNegativeDisposalMovementWhenValid() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, false, false), 1L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(ing));
        when(stockMovements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("10.000"));

        Account actor = account();
        when(currentUser.accountId()).thenReturn(actor.getId());
        when(accounts.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockMovements.save(any(StockMovement.class)))
                .thenAnswer(inv -> id(inv.getArgument(0), 50L));

        StockMovementResponse resp =
                service.createDisposal(
                        new CreateStockDisposalRequest(
                                1L, null, new BigDecimal("2.500"), "Ôi thiu"));

        assertNotNull(resp);
        assertEquals(new BigDecimal("-2.500"), resp.quantityChange());
        assertEquals(MovementType.DISPOSAL, resp.movementType());
        assertEquals("Ôi thiu", resp.reason());
        assertEquals("Xuất hủy", resp.source());
    }

    @Test
    void stocktakeLocksAllIngredientsInAscendingOrderBeforeReadingLedger() {
        Ingredient first = id(new Ingredient("Gạo", "kg", BigDecimal.ONE, false, false), 1L);
        Ingredient second = id(new Ingredient("Trứng", "quả", BigDecimal.TEN, false, false), 2L);
        when(ingredients.findLockedById(1L)).thenReturn(Optional.of(first));
        when(ingredients.findLockedById(2L)).thenReturn(Optional.of(second));
        when(stockMovements.sumQuantityByIngredientId(any())).thenReturn(BigDecimal.TEN);
        when(stocktakes.save(any())).thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(stocktakeItems.save(any())).thenAnswer(inv -> inv.getArgument(0));
        StocktakeResponse response =
                service.createStocktakeForSeed(
                        new CreateStocktakeRequest(
                                List.of(
                                        new StocktakeItemRequest(2L, BigDecimal.TEN, null),
                                        new StocktakeItemRequest(1L, BigDecimal.TEN, null))),
                        account());
        InOrder ordered = inOrder(ingredients, stockMovements);
        ordered.verify(ingredients).findLockedById(1L);
        ordered.verify(ingredients).findLockedById(2L);
        ordered.verify(stockMovements).sumQuantityByIngredientId(2L);
        ordered.verify(stockMovements).sumQuantityByIngredientId(1L);
        assertEquals(
                List.of(2L, 1L),
                response.items().stream().map(item -> item.ingredientId()).toList());
        verifyNoInteractions(currentUser);
    }

    @Test
    void stocktakeRejectsDuplicateIngredientBeforeLocksOrWrites() {
        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createStocktakeForSeed(
                                        new CreateStocktakeRequest(
                                                List.of(
                                                        new StocktakeItemRequest(
                                                                1L, BigDecimal.TEN, null),
                                                        new StocktakeItemRequest(
                                                                1L, BigDecimal.ONE, null))),
                                        account()));
        assertEquals(ErrorCode.INVALID_STOCKTAKE, error.getErrorCode());
        assertEquals("Nguyên vật liệu bị trùng trong phiếu kiểm kê", error.getMessage());
        verifyNoInteractions(ingredients, stocktakes, stocktakeItems, stockMovements);
    }

    @Test
    void stocktakeRejectsInactiveIngredientBeforeLedgerOrWrites() {
        Ingredient ingredient = ingredient(1L);
        ingredient.deactivate();
        when(ingredients.findLockedById(ingredient.getId())).thenReturn(Optional.of(ingredient));
        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.createStocktakeForSeed(
                                        new CreateStocktakeRequest(
                                                List.of(
                                                        new StocktakeItemRequest(
                                                                ingredient.getId(),
                                                                BigDecimal.ONE,
                                                                null))),
                                        account()));
        assertEquals(ErrorCode.INGREDIENT_INACTIVE, error.getErrorCode());
        verifyNoInteractions(stocktakes, stocktakeItems, stockMovements);
    }

    @Test
    void seedStocktakeAcceptsPositiveActualAgainstNegativeLedgerWithoutJwt() {
        Ingredient ingredient = ingredient(1L);
        when(ingredients.findLockedById(ingredient.getId())).thenReturn(Optional.of(ingredient));
        when(stockMovements.sumQuantityByIngredientId(ingredient.getId()))
                .thenReturn(new BigDecimal("-3.000"));
        when(stocktakes.save(any())).thenAnswer(inv -> id(inv.getArgument(0), 10L));
        when(stocktakeItems.save(any())).thenAnswer(inv -> id(inv.getArgument(0), 20L));
        StocktakeResponse result =
                service.createStocktakeForSeed(
                        new CreateStocktakeRequest(
                                List.of(
                                        new StocktakeItemRequest(
                                                ingredient.getId(),
                                                new BigDecimal("0.500"),
                                                null))),
                        account());
        assertEquals(new BigDecimal("-3.000"), result.items().get(0).systemQuantity());
        assertEquals(new BigDecimal("0.500"), result.items().get(0).actualQuantity());
        assertEquals(new BigDecimal("3.500"), result.items().get(0).difference());
        verifyNoInteractions(currentUser, accounts);
        verify(stockMovements).save(any(StockMovement.class));
    }

    @Test
    void receiptHistoryFetchesOnlyPageDetailsAndPreservesPageOrder() {
        StockReceipt first = id(new StockReceipt(account(), NOW), 20L);
        StockReceipt second = id(new StockReceipt(account(), NOW), 10L);
        when(stockReceipts.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 7));
        when(stockReceipts.findDetailsByIdIn(List.of(20L, 10L))).thenReturn(List.of(second, first));
        PageResponse<StockReceiptResponse> result = service.listReceipts(null, null, 0, 2);
        assertEquals(7, result.total());
        assertEquals(
                List.of(20L, 10L), result.items().stream().map(StockReceiptResponse::id).toList());
    }

    @Test
    void stocktakeHistoryFetchesOnlyPageDetailsAndPreservesPageOrder() {
        Stocktake first = id(new Stocktake(account(), NOW), 20L);
        Stocktake second = id(new Stocktake(account(), NOW), 10L);
        when(stocktakes.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 7));
        when(stocktakes.findDetailsByIdIn(List.of(20L, 10L))).thenReturn(List.of(second, first));
        PageResponse<StocktakeResponse> result = service.listStocktakes(null, null, 0, 2);
        assertEquals(7, result.total());
        assertEquals(
                List.of(20L, 10L), result.items().stream().map(StocktakeResponse::id).toList());
    }

    @Test
    void listAttentionLotsCategorizesExpiredAndExpiringSoon() {
        Ingredient ing = id(new Ingredient("Thịt bò", "kg", BigDecimal.ONE, true, true), 1L);

        // Today in Asia/Ho_Chi_Minh for NOW (2026-10-22) is 2026-10-22
        LocalDate today = LocalDate.of(2026, 10, 22);

        // Lot 1: Expired 2 days ago (2026-10-20)
        IngredientLot expiredLot = id(new IngredientLot(ing, "LOT-EXP", today.minusDays(2)), 11L);

        // Lot 2: Expiring in 3 days (2026-10-25)
        IngredientLot soonLot = id(new IngredientLot(ing, "LOT-SOON", today.plusDays(3)), 12L);

        when(ingredientLots.findAttentionLots(any(LocalDate.class)))
                .thenReturn(List.of(expiredLot, soonLot));

        List<Object[]> initialRows = new ArrayList<>();
        initialRows.add(new Object[] {11L, new BigDecimal("5.000")});
        initialRows.add(new Object[] {12L, new BigDecimal("10.000")});
        when(stockReceiptItems.findInitialQuantitiesByLotIds(List.of(11L, 12L)))
                .thenReturn(initialRows);

        List<IngredientLotAttentionResponse> results = service.listAttentionLots(7);

        assertEquals(2, results.size());
        assertEquals("EXPIRED", results.get(0).status());
        assertEquals(-2, results.get(0).daysRemaining());
        assertEquals(new BigDecimal("5.000"), results.get(0).initialQuantity());

        assertEquals("EXPIRING_SOON", results.get(1).status());
        assertEquals(3, results.get(1).daysRemaining());
        assertEquals(new BigDecimal("10.000"), results.get(1).initialQuantity());
    }
}
