package vn.ueh.kltn.restaurant.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateStockReceiptRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStocktakeRequest;
import vn.ueh.kltn.restaurant.dto.request.StockReceiptItemRequest;
import vn.ueh.kltn.restaurant.dto.request.StocktakeItemRequest;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.enums.AccountStatus;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptRepository;
import vn.ueh.kltn.restaurant.repository.StocktakeRepository;
import vn.ueh.kltn.restaurant.service.InventoryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PhaseThreeSeeder implements CommandLineRunner {

    // These are counted opening quantities, independent of any existing ledger balance.
    private static final Map<String, BigDecimal> OPENING_QUANTITIES =
            Map.ofEntries(
                    Map.entry("Thịt bò", new BigDecimal("15.000")),
                    Map.entry("Thịt heo", new BigDecimal("15.000")),
                    Map.entry("Sữa đặc", new BigDecimal("20.000")),
                    Map.entry("Gạo", new BigDecimal("30.000")),
                    Map.entry("Bánh phở", new BigDecimal("10.000")),
                    Map.entry("Bún", new BigDecimal("10.000")),
                    Map.entry("Trứng", new BigDecimal("50.000")),
                    Map.entry("Cà phê", new BigDecimal("1.000")),
                    Map.entry("Rau thơm", new BigDecimal("0.500")),
                    Map.entry("Trà", new BigDecimal("3.000")));
    private static final List<String> RECEIPT_INGREDIENTS =
            List.of("Thịt bò", "Thịt heo", "Sữa đặc");

    private final AppProperties properties;
    private final AccountRepository accounts;
    private final IngredientRepository ingredients;
    private final StockReceiptRepository stockReceipts;
    private final StocktakeRepository stocktakes;
    private final InventoryService inventory;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.seed()) return;

        boolean seedStocktake = stocktakes.count() == 0;
        boolean seedReceipt = stockReceipts.count() == 0;
        if (!seedStocktake) log.info("Bỏ qua seed kiểm kê kho: bảng stocktake đã có dữ liệu");
        if (!seedReceipt) log.info("Bỏ qua seed phiếu nhập kho: bảng stock_receipt đã có dữ liệu");
        if (!seedStocktake && !seedReceipt) return;

        Account headChef =
                accounts.findAll().stream()
                        .filter(
                                a ->
                                        a.getRole() == Role.HEAD_CHEF
                                                && a.getStatus() == AccountStatus.ACTIVE)
                        .findFirst()
                        .orElse(null);
        if (headChef == null) {
            log.warn("Bỏ qua seed kho Phase 3: chưa có tài khoản HEAD_CHEF đang hoạt động");
            return;
        }

        Map<String, Ingredient> ingredientMap = new HashMap<>();
        ingredients
                .findAll()
                .forEach(ingredient -> ingredientMap.put(ingredient.getName(), ingredient));

        // Opening stock must precede demo receipts. Existing movements are retained and D7
        // computes the real system quantity, including any negative Phase 2 consumption.
        if (seedStocktake) {
            if (ready(ingredientMap, OPENING_QUANTITIES.keySet().stream().toList())) {
                List<StocktakeItemRequest> items =
                        OPENING_QUANTITIES.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                        entry ->
                                                new StocktakeItemRequest(
                                                        ingredientMap.get(entry.getKey()).getId(),
                                                        entry.getValue(),
                                                        "Kiểm kê tồn đầu cho demo"))
                                .toList();
                inventory.createStocktakeForSeed(new CreateStocktakeRequest(items), headChef);
                log.info("Đã seed phiếu kiểm kê tồn đầu qua InventoryService");
            } else {
                log.warn(
                        "Bỏ qua seed tồn đầu: danh mục mẫu thiếu hoặc có nguyên vật liệu ngừng"
                                + " dùng");
            }
        }

        if (seedReceipt) {
            if (!ready(ingredientMap, RECEIPT_INGREDIENTS)) {
                log.warn(
                        "Bỏ qua seed phiếu nhập: danh mục mẫu thiếu hoặc có nguyên vật liệu ngừng"
                                + " dùng");
                return;
            }
            LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(properties.timezone())));
            List<StockReceiptItemRequest> items = new ArrayList<>();
            items.add(
                    new StockReceiptItemRequest(
                            ingredientMap.get("Thịt bò").getId(),
                            new BigDecimal("5.000"),
                            "LOT-TB-001",
                            today.minusDays(3)));
            items.add(
                    new StockReceiptItemRequest(
                            ingredientMap.get("Thịt bò").getId(),
                            new BigDecimal("10.000"),
                            "LOT-TB-002",
                            today.plusDays(2)));
            items.add(
                    new StockReceiptItemRequest(
                            ingredientMap.get("Thịt heo").getId(),
                            new BigDecimal("15.000"),
                            "LOT-TH-001",
                            today.plusDays(15)));
            items.add(
                    new StockReceiptItemRequest(
                            ingredientMap.get("Sữa đặc").getId(),
                            new BigDecimal("20.000"),
                            "LOT-SD-001",
                            today.plusDays(30)));
            inventory.createReceiptForSeed(new CreateStockReceiptRequest(items), headChef);
            log.info("Đã seed phiếu nhập có lô và hạn sử dụng qua InventoryService");
        }
    }

    private boolean ready(Map<String, Ingredient> ingredientMap, List<String> names) {
        return names.stream()
                .allMatch(
                        name ->
                                ingredientMap.containsKey(name)
                                        && ingredientMap.get(name).getStatus()
                                                == IngredientStatus.ACTIVE);
    }
}
