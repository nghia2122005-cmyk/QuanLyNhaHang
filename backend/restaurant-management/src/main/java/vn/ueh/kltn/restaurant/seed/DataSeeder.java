package vn.ueh.kltn.restaurant.seed;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningAreaRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;

import java.util.List;

@Component
@Order(0)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final AppProperties properties;
    private final AccountRepository accounts;
    private final DiningAreaRepository areas;
    private final DiningTableRepository tables;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.seed() || accounts.count() != 0) return;
        var demoAccounts =
                List.of(
                        new DemoAccount(Role.OWNER, "Nguyễn Minh Chủ", "0900000001"),
                        new DemoAccount(Role.MANAGER, "Trần Minh Quản", "0900000002"),
                        new DemoAccount(Role.CASHIER, "Lê Thu Ngân", "0900000003"),
                        new DemoAccount(Role.WAITER, "Phạm Phục Vụ", "0900000004"),
                        new DemoAccount(Role.KITCHEN_STAFF, "Hoàng Nhân Bếp", "0900000005"),
                        new DemoAccount(Role.HEAD_CHEF, "Vũ Bếp Trưởng", "0900000006"));
        for (var demo : demoAccounts) {
            accounts.save(
                    new Account(
                            demo.fullName(),
                            demo.phone(),
                            encoder.encode("Demo@123"),
                            demo.role()));
        }
        var diningAreas =
                areas.saveAll(
                        List.of(
                                new DiningArea("Tầng 1"),
                                new DiningArea("Tầng 2"),
                                new DiningArea("Sân vườn")));
        int[] capacities = {2, 4, 6, 8};
        for (int index = 0; index < 12; index++) {
            tables.save(
                    new DiningTable(
                            diningAreas.get(index / 4),
                            "T%02d".formatted(index + 1),
                            diningAreas.get(index / 4).getCode()
                                    + "-B%02d".formatted(index % 4 + 1),
                            capacities[index % 4]));
        }
    }

    private record DemoAccount(Role role, String fullName, String phone) {}
}
