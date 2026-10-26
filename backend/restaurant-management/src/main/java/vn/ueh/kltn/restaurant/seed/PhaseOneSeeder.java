package vn.ueh.kltn.restaurant.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.service.ReservationService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PhaseOneSeeder implements CommandLineRunner {
    private final AppProperties properties;
    private final MenuItemRepository menuItems;
    private final IngredientRepository ingredients;
    private final RecipeItemRepository recipes;
    private final ReservationRepository reservations;
    private final DiningTableRepository tables;
    private final AccountRepository accounts;
    private final ReservationService reservationService;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.seed()) return;
        if (menuItems.count() == 0) {
            String[] names = {
                "Phở bò",
                "Bún chả",
                "Cơm tấm",
                "Gỏi cuốn",
                "Cà phê sữa đá",
                "Trà đá",
                "Cà phê đen",
                "Cơm chiên",
                "Bún thịt nướng",
                "Trà đào"
            };
            int[] prices = {50000, 45000, 45000, 30000, 25000, 5000, 20000, 45000, 45000, 25000};
            for (int i = 0; i < names.length; i++)
                menuItems.save(new MenuItem(names[i], BigDecimal.valueOf(prices[i])));
        }
        if (ingredients.count() == 0) {
            ingredients.saveAll(
                    List.of(
                            ingredient("Thịt bò", "kg", "2", true, true),
                                    ingredient("Thịt heo", "kg", "3", true, true),
                            ingredient("Bánh phở", "kg", "4", false, false),
                                    ingredient("Bún", "kg", "4", false, false),
                            ingredient("Gạo", "kg", "10", false, false),
                                    ingredient("Rau thơm", "kg", "1", false, false),
                            ingredient("Cà phê", "kg", "2", false, false),
                                    ingredient("Sữa đặc", "hộp", "5", true, true),
                            ingredient("Trà", "kg", "1", false, false),
                                    ingredient("Trứng", "quả", "10", false, false)));
        }
        if (recipes.count() == 0) seedRecipes();
        if (reservations.count() == 0) seedReservations();
    }

    private Ingredient ingredient(
            String name, String unit, String min, boolean lot, boolean expiry) {
        return new Ingredient(name, unit, new BigDecimal(min), lot, expiry);
    }

    private void seedRecipes() {
        var menuByName = new HashMap<String, MenuItem>();
        menuItems.findAll().forEach(m -> menuByName.putIfAbsent(m.getName(), m));
        var ingredientByName = new HashMap<String, Ingredient>();
        ingredients.findAll().stream()
                .filter(i -> i.getStatus() == IngredientStatus.ACTIVE)
                .forEach(i -> ingredientByName.putIfAbsent(i.getName(), i));
        String[][] lines = {
            {"Phở bò", "Thịt bò", "0.150"},
            {"Phở bò", "Bánh phở", "0.200"},
            {"Phở bò", "Rau thơm", "0.020"},
            {"Bún chả", "Thịt heo", "0.150"},
            {"Bún chả", "Bún", "0.200"},
            {"Cơm tấm", "Gạo", "0.150"},
            {"Cơm tấm", "Thịt heo", "0.150"},
            {"Cơm tấm", "Trứng", "1.000"},
            {"Gỏi cuốn", "Bún", "0.100"},
            {"Gỏi cuốn", "Rau thơm", "0.030"},
            {"Cà phê sữa đá", "Cà phê", "0.020"},
            {"Cà phê sữa đá", "Sữa đặc", "0.100"},
            {"Trà đá", "Trà", "0.005"},
            {"Cà phê đen", "Cà phê", "0.025"}
        };
        if (Arrays.stream(lines)
                .anyMatch(
                        l ->
                                !menuByName.containsKey(l[0])
                                        || !ingredientByName.containsKey(l[1]))) {
            log.warn(
                    "Bỏ qua seed định lượng: danh mục hiện có chưa đủ món/NVL demo; không ghi đè dữ"
                        + " liệu");
            return;
        }
        recipes.saveAll(
                Arrays.stream(lines)
                        .map(
                                l ->
                                        new RecipeItem(
                                                menuByName.get(l[0]),
                                                ingredientByName.get(l[1]),
                                                new BigDecimal(l[2])))
                        .toList());
    }

    private void seedReservations() {
        Optional<Account> manager =
                accounts.findByPhone("0900000002").filter(a -> a.getRole() == Role.MANAGER);
        Optional<DiningTable> first = tables.findByName("T01");
        Optional<DiningTable> second = tables.findByName("T05");
        Optional<DiningTable> third = tables.findByName("T09");
        if (manager.isEmpty() || first.isEmpty() || second.isEmpty() || third.isEmpty()) {
            log.warn(
                    "Bỏ qua seed đặt bàn: thiếu tài khoản quản lý hoặc bàn demo; không thay dữ liệu"
                        + " hiện có");
            return;
        }
        Instant soon = clock.instant().plus(45, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        Instant tomorrow =
                LocalDate.now(clock.withZone(ZoneId.of(properties.timezone())))
                        .plusDays(1)
                        .atTime(18, 0)
                        .atZone(ZoneId.of(properties.timezone()))
                        .toInstant();
        List<DiningTable> selected = List.of(first.get(), second.get(), third.get());
        if (selected.stream()
                .anyMatch(t -> t.getStatus() == TableStatus.INACTIVE || !t.hasEnoughCapacity(2))) {
            log.warn("Bỏ qua seed đặt bàn: bàn demo ngừng sử dụng hoặc không đủ sức chứa");
            return;
        }
        List<Instant> starts = List.of(soon, tomorrow, tomorrow.plus(2, ChronoUnit.HOURS));
        List<String> names = List.of("Nguyễn Minh Anh", "Trần Thanh Bình", "Lê Ngọc Lan");
        for (int i = 0; i < 3; i++) {
            reservationService.createForSeed(
                    new CreateReservationRequest(
                            selected.get(i).getId(),
                            names.get(i),
                            "091000000" + (i + 1),
                            2,
                            starts.get(i),
                            starts.get(i).plus(2, ChronoUnit.HOURS),
                            "Khách đặt bàn dùng bữa"),
                    manager.get());
        }
    }
}
