package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
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
import static vn.ueh.kltn.restaurant.TestFixtures.ingredient;
import static vn.ueh.kltn.restaurant.TestFixtures.item;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.enums.SessionStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.InventoryAvailabilityException;
import vn.ueh.kltn.restaurant.realtime.RealtimeEvent;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.repository.StockMovementRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class KitchenServiceImplTest {
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final RecipeItemRepository recipes = mock(RecipeItemRepository.class);
    private final StockMovementRepository movements = mock(StockMovementRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ServiceSessionRepository sessions = mock(ServiceSessionRepository.class);
    private final MenuItemRepository menu = mock(MenuItemRepository.class);
    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private final CookingInventory inventory =
            new CookingInventory(mock(EntityManager.class), ingredients, items, movements);
    private final KitchenServiceImpl service =
            new KitchenServiceImpl(
                    items, recipes, movements, accounts, user, CLOCK, events, sessions, menu,
                    inventory);
    private OrderItem item;

    @BeforeEach
    void setup() {
        item = item();
        when(menu.findLockedById(1L)).thenReturn(Optional.of(item.getMenuItem()));
        when(ingredients.findLockedById(any()))
                .thenAnswer(call -> Optional.of(ingredient(call.getArgument(0))));
        when(movements.sumQuantityByIngredientId(any())).thenReturn(new BigDecimal("1000"));
        when(items.reservedQuantity(any(), any())).thenReturn(BigDecimal.ZERO);
        when(items.findSessionIdById(9L)).thenReturn(Optional.of(7L));
        when(sessions.findLockedById(7L)).thenReturn(Optional.of(item.getOrder().getSession()));
        when(items.findLockedById(9L)).thenReturn(Optional.of(item));
    }

    @Test
    @SuppressWarnings("unchecked")
    void legacyCompleteCapturesCurrentRecipeIncludingRetainedInactiveAndChecksStock() {
        item.startCooking();
        Ingredient inactive = ingredient(2L);
        inactive.deactivate();
        List<RecipeItem> recipe =
                List.of(
                        new RecipeItem(item.getMenuItem(), ingredient(1L), new BigDecimal("0.125")),
                        new RecipeItem(item.getMenuItem(), inactive, new BigDecimal("0.300")));
        when(recipes.findByMenuItemId(1L)).thenReturn(recipe);
        Account performer = account();
        when(user.accountId()).thenReturn(2L);
        when(accounts.getReferenceById(2L)).thenReturn(performer);
        assertEquals(OrderItemStatus.READY, service.complete(9L).status());
        ArgumentCaptor<List<StockMovement>> saved = ArgumentCaptor.forClass(List.class);
        verify(movements).saveAll(saved.capture());
        List<StockMovement> ledger = saved.getValue();
        assertEquals(2, ledger.size());
        assertEquals(new BigDecimal("-0.375"), ledger.get(0).getQuantityChange());
        assertEquals(new BigDecimal("-0.900"), ledger.get(1).getQuantityChange());
        for (StockMovement movement : ledger) {
            assertSame(item, movement.getOrderItem());
            assertSame(performer, movement.getPerformedBy());
            assertEquals(MovementType.CONSUMPTION, movement.getMovementType());
            assertEquals(NOW, movement.getOccurredAt());
            assertNull(movement.getLot());
            assertNull(movement.getReceiptItem());
            assertNull(movement.getStocktakeItem());
        }
        InOrder order = inOrder(items, sessions, recipes, movements, events);
        order.verify(items).findSessionIdById(9L);
        order.verify(sessions).findLockedById(7L);
        order.verify(items).findLockedById(9L);
        order.verify(recipes).findByMenuItemId(1L);
        order.verify(movements).saveAll(any());
        order.verify(events).publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
    }

    @Test
    void emptyRecipeStillCompletesAndReadyOrServedRetryDoesNotReadRecipeAgain() {
        item.startCooking();
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of());
        service.complete(9L);
        service.complete(9L);
        item.markServed();
        service.complete(9L);
        verify(recipes, times(1)).findByMenuItemId(1L);
        verify(movements, times(1)).saveAll(List.of());
        verify(events, times(1)).publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
        assertEquals(OrderItemStatus.SERVED, item.getStatus());
    }

    @Test
    void completeRejectsPendingWithoutMovementOrEvent() {
        assertThrows(BusinessException.class, () -> service.complete(9L));
        verifyNoInteractions(recipes, movements, events);
    }

    @Test
    void failedMovementWriteDoesNotMarkReadyOrPublish() {
        item.startCooking();
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("constraint")).when(movements).saveAll(any());
        assertThrows(DataIntegrityViolationException.class, () -> service.complete(9L));
        assertEquals(OrderItemStatus.COOKING, item.getStatus());
        verifyNoInteractions(events);
    }

    @Test
    void startAndUnavailableUseLockAndPublishOnlyRealChangesWithoutConsumption() {
        service.start(9L, false);
        service.start(9L, false);
        assertThrows(BusinessException.class, () -> service.unavailable(9L, " "));
        service.unavailable(9L, "Hết nguyên liệu");
        service.unavailable(9L, "Lý do khác");
        assertEquals("Hết nguyên liệu", item.getUnavailableReason());
        verify(items, times(5)).findLockedById(9L);
        verify(events, times(2)).publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
        verify(recipes, times(1)).findByMenuItemId(1L);
        verifyNoInteractions(movements);
    }

    @Test
    void queueOnlyReadsActivePendingAndCookingAndMapsContext() {
        when(items.findQueue(
                        SessionStatus.ACTIVE,
                        Set.of(OrderItemStatus.PENDING, OrderItemStatus.COOKING)))
                .thenReturn(List.of(item));
        assertEquals(7L, service.queue().getFirst().sessionId());
        assertEquals("T1", service.queue().getFirst().tableName());
    }

    @Test
    void missingItemIsRejected() {
        assertThrows(BusinessException.class, () -> service.start(99L, false));
        assertThrows(BusinessException.class, () -> service.unavailable(99L, "Hết món"));
        assertThrows(BusinessException.class, () -> service.complete(99L));
        verifyNoInteractions(events, movements);
    }

    @ParameterizedTest
    @CsvSource({
        "start,PENDING",
        "start,COOKING",
        "unavailable,PENDING",
        "unavailable,UNAVAILABLE",
        "complete,COOKING",
        "complete,READY",
        "complete,SERVED"
    })
    void closedSessionRejectsTransitionsIncludingRepeatedTargets(
            String action, OrderItemStatus status) {
        ReflectionTestUtils.setField(item, "status", status);
        item.getOrder().getSession().close(NOW);
        assertThrows(BusinessException.class, () -> mutate(action));
        assertEquals(status, item.getStatus());
        verifyNoInteractions(recipes, movements, events);
    }

    @ParameterizedTest
    @CsvSource({"start,PENDING", "unavailable,PENDING", "complete,COOKING"})
    void mutationsLockSessionBeforeItem(String action, OrderItemStatus status) {
        ReflectionTestUtils.setField(item, "status", status);
        mutate(action);
        InOrder locks = inOrder(items, sessions);
        locks.verify(items).findSessionIdById(9L);
        locks.verify(sessions).findLockedById(7L);
        locks.verify(items).findLockedById(9L);
        verify(items, never()).findById(9L);
    }

    @Test
    void lowStockWarningHasNoSideEffectsAndConfirmationRechecks() {
        when(recipes.findByMenuItemId(1L))
                .thenReturn(
                        List.of(
                                new RecipeItem(
                                        item.getMenuItem(),
                                        ingredient(1L),
                                        new BigDecimal("0.8"))));
        when(movements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("3"));
        assertEquals(false, service.start(9L, false).started());
        assertEquals(OrderItemStatus.PENDING, item.getStatus());
        assertNull(item.getIngredientRequirements());
        verifyNoInteractions(events);
        when(movements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("2"));
        assertThrows(InventoryAvailabilityException.class, () -> service.start(9L, true));
        assertEquals(OrderItemStatus.PENDING, item.getStatus());
        assertNull(item.getIngredientRequirements());
    }

    @Test
    void startCapturesQuantityAndCompleteNeverRereadsChangedRecipe() {
        when(recipes.findByMenuItemId(1L))
                .thenReturn(
                        List.of(
                                new RecipeItem(
                                        item.getMenuItem(),
                                        ingredient(1L),
                                        new BigDecimal("0.8"))));
        assertEquals(true, service.start(9L, false).started());
        assertEquals(new BigDecimal("2.4"), item.getIngredientRequirements().getFirst().quantity());
        service.start(9L, false);
        when(recipes.findByMenuItemId(1L)).thenReturn(List.of());
        service.complete(9L);
        service.complete(9L);
        verify(recipes, times(1)).findByMenuItemId(1L);
        verify(movements, times(1)).saveAll(any());
    }

    @Test
    void startAccountsForOtherReservationsAndExactZeroCanBeConfirmed() {
        when(recipes.findByMenuItemId(1L))
                .thenReturn(
                        List.of(
                                new RecipeItem(
                                        item.getMenuItem(),
                                        ingredient(1L),
                                        new BigDecimal("0.8"))));
        when(movements.sumQuantityByIngredientId(1L)).thenReturn(new BigDecimal("3.4"));
        when(items.reservedQuantity(1L, 9L)).thenReturn(BigDecimal.ONE);
        assertEquals(false, service.start(9L, false).started());
        assertEquals(true, service.start(9L, true).started());
        assertEquals(OrderItemStatus.COOKING, item.getStatus());
    }

    private void mutate(String action) {
        switch (action) {
            case "start" -> service.start(9L, false);
            case "unavailable" -> service.unavailable(9L, "Hết nguyên liệu");
            case "complete" -> service.complete(9L);
            default -> throw new IllegalArgumentException(action);
        }
    }
}
