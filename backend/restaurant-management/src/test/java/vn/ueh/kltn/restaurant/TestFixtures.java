package vn.ueh.kltn.restaurant;

import org.springframework.test.util.ReflectionTestUtils;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.FoodOrder;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.OrderItem;
import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.Role;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TestFixtures {
    private TestFixtures() {}

    public static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    public static AppProperties appProperties(String timezone) {
        return new AppProperties(
                timezone,
                false,
                new AppProperties.Jwt("test-only-secret-key-at-least-32-characters", 12),
                new AppProperties.Alert(3),
                new AppProperties.Reservation(60));
    }

    public static <T> T id(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static Account account() {
        return id(new Account("Quản lý", "0900000002", "hash", Role.MANAGER), 2L);
    }

    public static DiningTable table(Long id, int capacity) {
        return id(new DiningTable(id(new DiningArea("Tầng 1"), 1L), "T" + id, capacity), id);
    }

    public static Reservation reservation(DiningTable table) {
        return id(
                new Reservation(
                        table,
                        account(),
                        "Khách",
                        "0910000001",
                        2,
                        NOW,
                        NOW.plusSeconds(7200),
                        null,
                        NOW),
                1L);
    }

    public static Ingredient ingredient(Long id) {
        return id(new Ingredient("NVL " + id, "kg", BigDecimal.ONE, false, false), id);
    }

    public static MenuItem menu() {
        return id(new MenuItem("Phở bò", new BigDecimal("50000")), 1L);
    }

    public static ServiceSession session() {
        return id(new ServiceSession(table(1L, 4), null, account(), NOW), 7L);
    }

    public static OrderItem item() {
        var order = id(new FoodOrder(session(), account(), NOW), 8L);
        var item = id(new OrderItem(menu(), 3, "[QA] Không hành"), 9L);
        order.addItem(item);
        return item;
    }
}
