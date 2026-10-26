package vn.ueh.kltn.restaurant.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.id;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateStockReceiptRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStocktakeRequest;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptRepository;
import vn.ueh.kltn.restaurant.repository.StocktakeRepository;
import vn.ueh.kltn.restaurant.service.InventoryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

class PhaseThreeSeederTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private final StockReceiptRepository receipts = mock(StockReceiptRepository.class);
    private final StocktakeRepository stocktakes = mock(StocktakeRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final Account chef =
            id(new Account("Bếp trưởng", "0900000006", "hash", Role.HEAD_CHEF), 6L);

    private PhaseThreeSeeder seeder(boolean enabled) {
        AppProperties properties =
                new AppProperties(
                        "Asia/Ho_Chi_Minh",
                        enabled,
                        new AppProperties.Jwt("test-secret-for-phase-three-only-32", 12),
                        new AppProperties.Alert(3),
                        new AppProperties.Reservation(60));
        return new PhaseThreeSeeder(
                properties, accounts, ingredients, receipts, stocktakes, inventory, CLOCK);
    }

    private void completeCatalog() {
        List<String> names =
                List.of(
                        "Thịt bò",
                        "Thịt heo",
                        "Sữa đặc",
                        "Gạo",
                        "Bánh phở",
                        "Bún",
                        "Trứng",
                        "Cà phê",
                        "Rau thơm",
                        "Trà");
        List<Ingredient> catalog =
                IntStream.range(0, names.size())
                        .mapToObj(
                                i ->
                                        id(
                                                new Ingredient(
                                                        names.get(i),
                                                        "kg",
                                                        BigDecimal.ONE,
                                                        i < 3,
                                                        i < 3),
                                                (long) i + 1))
                        .toList();
        when(ingredients.findAll()).thenReturn(catalog);
        when(accounts.findAll()).thenReturn(List.of(chef));
    }

    @Test
    void seedsConfiguredNonNegativeOpeningBeforeReceiptAndDoesNotDuplicate() {
        completeCatalog();
        when(stocktakes.count()).thenReturn(0L, 1L);
        when(receipts.count()).thenReturn(0L, 1L);
        PhaseThreeSeeder seeder = seeder(true);
        seeder.run();
        seeder.run();
        ArgumentCaptor<CreateStocktakeRequest> opening =
                ArgumentCaptor.forClass(CreateStocktakeRequest.class);
        ArgumentCaptor<CreateStockReceiptRequest> receipt =
                ArgumentCaptor.forClass(CreateStockReceiptRequest.class);
        InOrder ordered = inOrder(inventory);
        ordered.verify(inventory).createStocktakeForSeed(opening.capture(), eq(chef));
        ordered.verify(inventory).createReceiptForSeed(receipt.capture(), eq(chef));
        ordered.verifyNoMoreInteractions();
        assertEquals(10, opening.getValue().items().size());
        assertTrue(
                opening.getValue().items().stream()
                        .allMatch(item -> item.actualQuantity().signum() >= 0));
        assertEquals(
                new BigDecimal("0.500"),
                opening.getValue().items().stream()
                        .filter(item -> item.ingredientId().equals(9L))
                        .findFirst()
                        .orElseThrow()
                        .actualQuantity());
        assertEquals(4, receipt.getValue().items().size());
        assertTrue(receipt.getValue().items().stream().allMatch(item -> item.expiryDate() != null));
    }

    @Test
    void existingReceiptDoesNotBlockMissingOpeningStocktake() {
        completeCatalog();
        when(receipts.count()).thenReturn(1L);
        seeder(true).run();
        verify(inventory).createStocktakeForSeed(any(), eq(chef));
        verify(inventory, never()).createReceiptForSeed(any(), any());
    }

    @Test
    void existingStocktakeDoesNotBlockMissingReceipt() {
        completeCatalog();
        when(stocktakes.count()).thenReturn(1L);
        seeder(true).run();
        verify(inventory).createReceiptForSeed(any(), eq(chef));
        verify(inventory, never()).createStocktakeForSeed(any(), any());
    }

    @Test
    void incompleteCatalogSkipsWholeBlocksWithoutWritingPartialSeed() {
        when(accounts.findAll()).thenReturn(List.of(chef));
        when(ingredients.findAll())
                .thenReturn(
                        List.of(
                                id(
                                        new Ingredient(
                                                "Trứng", "quả", BigDecimal.ONE, false, false),
                                        1L)));
        seeder(true).run();
        verifyNoInteractions(inventory);
    }

    @Test
    void inactiveCatalogDoesNotCreateStocktakeOrReceipt() {
        completeCatalog();
        ingredients.findAll().get(0).deactivate();
        seeder(true).run();
        verifyNoInteractions(inventory);
    }

    @Test
    void noActiveHeadChefSkipsSeed() {
        chef.lock();
        when(accounts.findAll()).thenReturn(List.of(chef));
        seeder(true).run();
        verifyNoInteractions(inventory, ingredients);
    }

    @Test
    void disabledSeedDoesNotReadOrWrite() {
        seeder(false).run();
        verifyNoInteractions(accounts, ingredients, receipts, stocktakes, inventory);
    }
}
