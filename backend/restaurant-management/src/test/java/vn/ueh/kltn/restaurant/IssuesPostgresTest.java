package vn.ueh.kltn.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import vn.ueh.kltn.restaurant.dto.request.CreateOrderRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateReservationRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockDisposalRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStockReceiptRequest;
import vn.ueh.kltn.restaurant.dto.request.CreateStocktakeRequest;
import vn.ueh.kltn.restaurant.dto.request.OpenSessionRequest;
import vn.ueh.kltn.restaurant.dto.request.OrderLineRequest;
import vn.ueh.kltn.restaurant.dto.request.RecipeLineRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningAreaRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveDiningTableRequest;
import vn.ueh.kltn.restaurant.dto.request.SaveRecipeRequest;
import vn.ueh.kltn.restaurant.dto.request.StockReceiptItemRequest;
import vn.ueh.kltn.restaurant.dto.request.StocktakeItemRequest;
import vn.ueh.kltn.restaurant.dto.response.FoodOrderResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.DiningArea;
import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.entity.MenuItem;
import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.entity.StockMovement;
import vn.ueh.kltn.restaurant.enums.AreaStatus;
import vn.ueh.kltn.restaurant.enums.MovementType;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.InventoryAvailabilityException;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.DiningAreaRepository;
import vn.ueh.kltn.restaurant.repository.DiningTableRepository;
import vn.ueh.kltn.restaurant.repository.IngredientLotRepository;
import vn.ueh.kltn.restaurant.repository.IngredientRepository;
import vn.ueh.kltn.restaurant.repository.InvoiceRepository;
import vn.ueh.kltn.restaurant.repository.MenuItemRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.RecipeItemRepository;
import vn.ueh.kltn.restaurant.repository.ReservationRepository;
import vn.ueh.kltn.restaurant.repository.StockMovementRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptItemRepository;
import vn.ueh.kltn.restaurant.repository.StockReceiptRepository;
import vn.ueh.kltn.restaurant.security.JwtService;
import vn.ueh.kltn.restaurant.service.InventoryService;
import vn.ueh.kltn.restaurant.service.KitchenService;
import vn.ueh.kltn.restaurant.service.MenuService;
import vn.ueh.kltn.restaurant.service.ReservationService;
import vn.ueh.kltn.restaurant.service.ServiceSessionService;
import vn.ueh.kltn.restaurant.service.TableService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Real PostgreSQL, opt-in and hard guarded against any production endpoint. No local profile. */
@EnabledIfEnvironmentVariable(named = "KLTN_TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IssuesPostgresTest {
    private ConfigurableApplicationContext context;
    private String url;
    private String schema;
    private String api;
    private final Map<Role, Account> actors = new EnumMap<>(Role.class);
    private final Map<Role, String> tokens = new EnumMap<>(Role.class);
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private TransactionTemplate tx;
    private KitchenService kitchen;
    private InventoryService inventory;
    private ServiceSessionService sessions;
    private ReservationService reservations;

    @BeforeAll
    void startIsolatedApplication() throws Exception {
        url = System.getenv("KLTN_TEST_DB_URL");
        assertTrue(
                url.matches(
                        "jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/kltn_issues_test"),
                "Only a loopback kltn_issues_test database is allowed");
        schema = "issues_" + UUID.randomUUID().toString().replace("-", "");
        context =
                new SpringApplicationBuilder(RestaurantApplication.class)
                        .run(
                                "--spring.datasource.url=" + url,
                                "--spring.datasource.username=kltn_test",
                                "--spring.datasource.password=",
                                "--spring.datasource.hikari.schema=" + schema,
                                "--spring.flyway.schemas=" + schema,
                                "--spring.flyway.default-schema=" + schema,
                                "--spring.jpa.properties.hibernate.default_schema=" + schema,
                                "--spring.profiles.active=issues-test",
                                "--app.seed=false",
                                "--app.jwt.secret=isolated-integration-test-secret-at-least-32-characters",
                                "--server.port=0",
                                "--logging.level.root=WARN");
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        kitchen = context.getBean(KitchenService.class);
        inventory = context.getBean(InventoryService.class);
        sessions = context.getBean(ServiceSessionService.class);
        reservations = context.getBean(ReservationService.class);
        api =
                "http://127.0.0.1:"
                        + ((WebServerApplicationContext) context).getWebServer().getPort()
                        + "/api";
        tx.executeWithoutResult(
                status -> {
                    for (Role role : Role.values()) {
                        Account actor =
                                context.getBean(AccountRepository.class)
                                        .save(
                                                new Account(
                                                        role.name(),
                                                        "090000000" + (role.ordinal() + 1),
                                                        context.getBean(PasswordEncoder.class)
                                                                .encode("IssueTest@123"),
                                                        role));
                        actors.put(role, actor);
                        tokens.put(
                                role, context.getBean(JwtService.class).issue(actor).accessToken());
                    }
                });
    }

    @AfterAll
    void stop() {
        SecurityContextHolder.clearContext();
        if (context != null) context.close();
    }

    private void actor(Role role) {
        Jwt jwt =
                Jwt.withTokenValue("integration-only")
                        .header("alg", "HS256")
                        .subject(actors.get(role).getId().toString())
                        .claim("role", role.name())
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private record Fixture(Long table, Long session, Long item, Long ingredient, Long menu) {}

    private Fixture fixture(String stock, String required, String minimum) {
        actor(Role.HEAD_CHEF);
        List<Long> ids =
                tx.execute(
                        status -> {
                            String tag = UUID.randomUUID().toString().substring(0, 8);
                            DiningArea area =
                                    context.getBean(DiningAreaRepository.class)
                                            .save(new DiningArea("Test " + tag));
                            DiningTable table =
                                    context.getBean(DiningTableRepository.class)
                                            .save(new DiningTable(area, "B" + tag, 4));
                            Ingredient ingredient =
                                    context.getBean(IngredientRepository.class)
                                            .save(
                                                    new Ingredient(
                                                            "NVL " + tag,
                                                            "kg",
                                                            new BigDecimal(minimum),
                                                            false,
                                                            false));
                            MenuItem menu =
                                    context.getBean(MenuItemRepository.class)
                                            .save(
                                                    new MenuItem(
                                                            "Món " + tag, new BigDecimal("30000")));
                            if (required != null)
                                context.getBean(RecipeItemRepository.class)
                                        .save(
                                                new RecipeItem(
                                                        menu,
                                                        ingredient,
                                                        new BigDecimal(required)));
                            if (new BigDecimal(stock).signum() < 0)
                                context.getBean(StockMovementRepository.class)
                                        .save(
                                                new StockMovement(
                                                        ingredient,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        actors.get(Role.HEAD_CHEF),
                                                        MovementType.DISPOSAL,
                                                        new BigDecimal(stock),
                                                        "Fixture ledger (test only)",
                                                        Instant.now()));
                            return List.of(table.getId(), ingredient.getId(), menu.getId());
                        });
        if (new BigDecimal(stock).signum() >= 0)
            inventory.createStocktake(
                    new CreateStocktakeRequest(
                            List.of(
                                    new StocktakeItemRequest(
                                            ids.get(1),
                                            new BigDecimal(stock),
                                            "Opening count in isolated test"))));
        // Positive fixture stock uses the normal stocktake workflow, negative values are historical
        // test data.
        actor(Role.WAITER);
        Long session = sessions.open(new OpenSessionRequest(ids.get(0), null)).sessionId();
        FoodOrderResponse order =
                sessions.order(
                        session,
                        new CreateOrderRequest(List.of(new OrderLineRequest(ids.get(2), 1, null))));
        return new Fixture(
                ids.get(0), session, order.items().getFirst().id(), ids.get(1), ids.get(2));
    }

    private BigDecimal stock(Long id) {
        return context.getBean(StockMovementRepository.class).sumQuantityByIngredientId(id);
    }

    private BigDecimal held(Long id) {
        return context.getBean(OrderItemRepository.class).reservedQuantity(id, null);
    }

    private void decimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private List<Boolean> race(Callable<?> left, Callable<?> right) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> jobs = new ArrayList<>();
            for (Callable<?> task : List.of(left, right))
                jobs.add(
                        pool.submit(
                                () -> {
                                    actor(Role.HEAD_CHEF);
                                    ready.countDown();
                                    assertTrue(go.await(5, TimeUnit.SECONDS));
                                    try {
                                        task.call();
                                        return true;
                                    } catch (BusinessException rejected) {
                                        return false;
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            return List.of(
                    jobs.get(0).get(15, TimeUnit.SECONDS), jobs.get(1).get(15, TimeUnit.SECONDS));
        }
    }

    private JsonNode call(Role role, String method, String path, String body, int expected)
            throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(api + path))
                        .header("Authorization", "Bearer " + tokens.get(role))
                        .header("Content-Type", "application/json");
        request.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response =
                http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), response.body());
        return response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
    }

    @Test
    void twoKitchensCannotReserveBeyondStockAndRetriesConsumeOnce() throws Exception {
        Fixture f = fixture("1", "0.8", "0");
        actor(Role.WAITER);
        Long tableId =
                tx.execute(
                        status -> {
                            DiningArea area =
                                    context.getBean(DiningAreaRepository.class)
                                            .save(
                                                    new DiningArea(
                                                            "Concurrent " + UUID.randomUUID()));
                            return context.getBean(DiningTableRepository.class)
                                    .save(new DiningTable(area, "Other", 4))
                                    .getId();
                        });
        Long otherSession = sessions.open(new OpenSessionRequest(tableId, null)).sessionId();
        Long other =
                sessions.order(
                                otherSession,
                                new CreateOrderRequest(
                                        List.of(new OrderLineRequest(f.menu(), 1, null))))
                        .items()
                        .getFirst()
                        .id();
        List<Boolean> results =
                race(() -> kitchen.start(f.item(), false), () -> kitchen.start(other, false));
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        decimal("0.8", held(f.ingredient()));
        decimal("1", stock(f.ingredient()));
        Long winner = results.getFirst() ? f.item() : other;
        actor(Role.KITCHEN_STAFF);
        assertTrue(kitchen.start(winner, false).started());
        race(() -> kitchen.complete(winner), () -> kitchen.complete(winner));
        decimal("0.2", stock(f.ingredient()));
        decimal("0", held(f.ingredient()));
    }

    @Test
    void warningHasNoSideEffectsAndConfirmRechecksThroughHttp() throws Exception {
        Fixture f = fixture("1", "0.8", "0.5");
        JsonNode warning =
                call(
                        Role.KITCHEN_STAFF,
                        "PATCH",
                        "/kitchen/items/" + f.item() + "/start",
                        "{}",
                        200);
        assertFalse(warning.path("started").asBoolean());
        decimal("0", held(f.ingredient()));
        actor(Role.HEAD_CHEF);
        inventory.createDisposal(
                new CreateStockDisposalRequest(
                        f.ingredient(), null, new BigDecimal("0.5"), "Test competing disposal"));
        JsonNode blocked =
                call(
                        Role.KITCHEN_STAFF,
                        "PATCH",
                        "/kitchen/items/" + f.item() + "/start",
                        "{\"confirmLowStock\":true}",
                        409);
        assertTrue(blocked.path("ingredients").isArray());
        decimal("0", held(f.ingredient()));
    }

    @Test
    void zeroBoundarySnapshotChangeAndUnavailableRelease() throws Exception {
        Fixture f = fixture("0.8", "0.8", "0");
        actor(Role.HEAD_CHEF);
        assertTrue(kitchen.start(f.item(), false).started());
        context.getBean(MenuService.class)
                .saveRecipe(
                        f.menu(),
                        new SaveRecipeRequest(
                                List.of(
                                        new RecipeLineRequest(
                                                f.ingredient(), new BigDecimal("10")))));
        kitchen.complete(f.item());
        decimal("0", stock(f.ingredient()));
        Fixture release = fixture("1", "0.8", "0");
        actor(Role.HEAD_CHEF);
        kitchen.start(release.item(), false);
        kitchen.unavailable(release.item(), "Hết nguyên liệu");
        decimal("0", held(release.ingredient()));
        decimal("1", stock(release.ingredient()));
        call(
                Role.WAITER,
                "PATCH",
                "/sessions/items/" + release.item() + "/acknowledge-unavailable",
                null,
                200);
        call(
                Role.WAITER,
                "PATCH",
                "/sessions/items/" + release.item() + "/acknowledge-unavailable",
                null,
                200);
        assertTrue(
                sessions.awaitingService().stream().noneMatch(i -> i.id().equals(release.item())));
    }

    @Test
    void startSerializesWithDisposalAndStocktake() throws Exception {
        Fixture disposal = fixture("1", "0.8", "0");
        List<Boolean> first =
                race(
                        () -> kitchen.start(disposal.item(), false),
                        () ->
                                inventory.createDisposal(
                                        new CreateStockDisposalRequest(
                                                disposal.ingredient(),
                                                null,
                                                new BigDecimal("0.8"),
                                                "Concurrent test")));
        assertEquals(1, first.stream().filter(Boolean::booleanValue).count());
        assertTrue(
                stock(disposal.ingredient()).subtract(held(disposal.ingredient())).signum() >= 0);
        Fixture count = fixture("1", "0.8", "0");
        List<Boolean> second =
                race(
                        () -> kitchen.start(count.item(), false),
                        () ->
                                inventory.createStocktake(
                                        new CreateStocktakeRequest(
                                                List.of(
                                                        new StocktakeItemRequest(
                                                                count.ingredient(),
                                                                new BigDecimal("0.1"),
                                                                "Count test")))));
        assertEquals(1, second.stream().filter(Boolean::booleanValue).count());
        assertTrue(stock(count.ingredient()).subtract(held(count.ingredient())).signum() >= 0);
    }

    @Test
    void fullApiFlowBatchNotificationsEligibilityAndRoles() throws Exception {
        Fixture f = fixture("5", "0.8", "0");
        actor(Role.WAITER);
        FoodOrderResponse batch =
                sessions.order(
                        f.session(),
                        new CreateOrderRequest(
                                List.of(
                                        new OrderLineRequest(f.menu(), 2, null),
                                        new OrderLineRequest(f.menu(), 1, "Unavailable"))));
        Long ready = batch.items().getFirst().id();
        Long unavailable = batch.items().getLast().id();
        call(Role.KITCHEN_STAFF, "GET", "/kitchen/orders/" + batch.id(), null, 200);
        call(Role.WAITER, "GET", "/kitchen/orders/" + batch.id(), null, 403);
        call(Role.KITCHEN_STAFF, "GET", "/sessions/" + f.session(), null, 403);
        call(Role.KITCHEN_STAFF, "PATCH", "/kitchen/items/" + ready + "/start", "{}", 200);
        JsonNode stockList = call(Role.HEAD_CHEF, "GET", "/inventory/ingredients", null, 200);
        JsonNode stockLine = null;
        for (int index = 0; index < stockList.size(); index++)
            if (stockList.get(index).path("id").asLong() == f.ingredient())
                stockLine = stockList.get(index);
        assertNotNull(stockLine);
        assertEquals(1.6, stockLine.path("reservedStock").asDouble());
        assertEquals(3.4, stockLine.path("availableStock").asDouble());
        call(Role.KITCHEN_STAFF, "PATCH", "/kitchen/items/" + ready + "/complete", null, 200);
        call(
                Role.HEAD_CHEF,
                "PATCH",
                "/kitchen/items/" + unavailable + "/unavailable",
                "{\"reason\":\"Hết nguyên liệu\"}",
                200);
        call(Role.WAITER, "PATCH", "/sessions/items/" + f.item() + "/cancel", null, 200);
        JsonNode notifications =
                call(Role.OWNER, "GET", "/sessions/items/awaiting-service", null, 200);
        assertTrue(notifications.size() >= 2);
        String pay = "{\"sessionId\":" + f.session() + ",\"paymentMethod\":\"CASH\"}";
        call(Role.CASHIER, "POST", "/payments", pay, 400);
        call(Role.MANAGER, "PATCH", "/sessions/items/" + ready + "/serve", null, 403);
        call(Role.WAITER, "PATCH", "/sessions/items/" + ready + "/serve", null, 200);
        call(
                Role.WAITER,
                "PATCH",
                "/sessions/items/" + unavailable + "/acknowledge-unavailable",
                null,
                200);
        JsonNode invoice = call(Role.CASHIER, "POST", "/payments", pay, 200);
        assertEquals(60000, invoice.path("amount").asInt());
        call(Role.CASHIER, "POST", "/payments", pay, 409);
        assertTrue(
                sessions.awaitingService().stream()
                        .noneMatch(i -> i.sessionId().equals(f.session())));
        assertEquals("CLOSED", sessions.detail(f.session()).status().name());
    }

    @Test
    void reservationUpdateAndHeadChefRecipeWorkThroughRealApi() throws Exception {
        Fixture f = fixture("5", "0.8", "0");
        String body =
                "{\"tableId\":"
                        + f.table()
                        + ",\"customerName\":\"Khách kiểm"
                        + " thử\",\"customerPhone\":\"0912345678\",\"guestCount\":2,\"startTime\":\"2026-11-01T10:00:00Z\",\"endTime\":\"2026-11-01T12:00:00Z\"}";
        JsonNode created = call(Role.MANAGER, "POST", "/reservations", body, 200);
        String path = "/reservations/" + created.path("id").asLong();
        Instant originalCreatedAt =
                context.getBean(ReservationRepository.class)
                        .findById(created.path("id").asLong())
                        .orElseThrow()
                        .getCreatedAt();
        JsonNode updated =
                call(
                        Role.MANAGER,
                        "PUT",
                        path,
                        body.replace("guestCount\":2", "guestCount\":3"),
                        200);
        assertEquals(3, updated.path("guestCount").asInt());
        assertEquals(originalCreatedAt, Instant.parse(updated.path("createdAt").asString()));
        call(Role.WAITER, "PUT", path, body, 403);
        call(Role.MANAGER, "POST", "/reservations", body, 409);
        call(Role.MANAGER, "PUT", path, body.replace("guestCount\":2", "guestCount\":9"), 409);
        String recipe =
                "{\"lines\":[{\"ingredientId\":"
                        + f.ingredient()
                        + ",\"quantityPerServing\":0.125}]}";
        call(Role.HEAD_CHEF, "PUT", "/menu-items/" + f.menu() + "/recipe", recipe, 200);
        JsonNode lines =
                call(Role.HEAD_CHEF, "GET", "/menu-items/" + f.menu() + "/recipe", null, 200);
        assertEquals(0.125, lines.get(0).path("quantityPerServing").asDouble());
        call(
                Role.HEAD_CHEF,
                "PUT",
                "/menu-items/" + f.menu(),
                "{\"name\":\"Forbidden\",\"price\":1}",
                403);
    }

    private void sql(String statement) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "kltn_test", "")) {
            connection.setSchema(schema);
            try (Statement query = connection.createStatement()) {
                query.execute(statement);
            }
        }
    }

    @Test
    void multiIngredientShortageDoesNotReserveAnythingAndEmptyRecipesStillWork() throws Exception {
        Fixture f = fixture("1", "0.8", "0");
        Long missing =
                tx.execute(
                        status -> {
                            Ingredient ingredient =
                                    context.getBean(IngredientRepository.class)
                                            .save(
                                                    new Ingredient(
                                                            "Missing " + UUID.randomUUID(),
                                                            "kg",
                                                            BigDecimal.ZERO,
                                                            false,
                                                            false));
                            context.getBean(RecipeItemRepository.class)
                                    .save(
                                            new RecipeItem(
                                                    context.getBean(MenuItemRepository.class)
                                                            .findById(f.menu())
                                                            .orElseThrow(),
                                                    ingredient,
                                                    BigDecimal.ONE));
                            return ingredient.getId();
                        });
        actor(Role.HEAD_CHEF);
        assertThrows(InventoryAvailabilityException.class, () -> kitchen.start(f.item(), true));
        decimal("0", held(f.ingredient()));
        decimal("0", held(missing));
        assertEquals(
                "PENDING",
                sessions.detail(f.session())
                        .orders()
                        .getFirst()
                        .items()
                        .getFirst()
                        .status()
                        .name());
        Fixture empty = fixture("0", null, "0");
        actor(Role.HEAD_CHEF);
        assertTrue(kitchen.start(empty.item(), false).started());
        kitchen.complete(empty.item());
        decimal("0", stock(empty.ingredient()));
    }

    @Test
    void historicalNegativeStockIsNotMaskedAndCannotStart() {
        Fixture f = fixture("-1", "0.1", "0");
        actor(Role.HEAD_CHEF);
        assertThrows(InventoryAvailabilityException.class, () -> kitchen.start(f.item(), true));
        decimal("-1", stock(f.ingredient()));
        decimal("0", held(f.ingredient()));
    }

    @Test
    void failedStartAndPartialConsumptionAreRolledBackByPostgres() throws Exception {
        Fixture f = fixture("1", "0.4", "0");
        sql(
                "create function reject_start_"
                        + f.item()
                        + "() returns trigger language plpgsql as $$ begin raise exception"
                        + " 'isolated rollback test'; end $$; create trigger reject_start_"
                        + f.item()
                        + " before update on order_item for each row when (new.id = "
                        + f.item()
                        + " and new.status = 'COOKING') execute function reject_start_"
                        + f.item()
                        + "()");
        actor(Role.HEAD_CHEF);
        assertThrows(Exception.class, () -> kitchen.start(f.item(), false));
        decimal("0", held(f.ingredient()));
        decimal("1", stock(f.ingredient()));
        Fixture complete = fixture("1", "0.4", "0");
        Long second =
                tx.execute(
                        status -> {
                            Ingredient ingredient =
                                    context.getBean(IngredientRepository.class)
                                            .save(
                                                    new Ingredient(
                                                            "Rollback " + UUID.randomUUID(),
                                                            "kg",
                                                            BigDecimal.ZERO,
                                                            false,
                                                            false));
                            context.getBean(RecipeItemRepository.class)
                                    .save(
                                            new RecipeItem(
                                                    context.getBean(MenuItemRepository.class)
                                                            .findById(complete.menu())
                                                            .orElseThrow(),
                                                    ingredient,
                                                    new BigDecimal("0.2")));
                            return ingredient.getId();
                        });
        inventory.createStocktake(
                new CreateStocktakeRequest(
                        List.of(
                                new StocktakeItemRequest(
                                        second, BigDecimal.ONE, "Rollback fixture"))));
        kitchen.start(complete.item(), false);
        sql(
                "create function reject_consumption_"
                        + second
                        + "() returns trigger language plpgsql as $$ begin raise exception"
                        + " 'isolated consumption rollback'; end $$; create trigger"
                        + " reject_consumption_"
                        + second
                        + " before insert on stock_movement for each row when (new.ingredient_id = "
                        + second
                        + " and new.movement_type = 'CONSUMPTION') execute function"
                        + " reject_consumption_"
                        + second
                        + "()");
        assertThrows(Exception.class, () -> kitchen.complete(complete.item()));
        decimal("1", stock(complete.ingredient()));
        decimal("1", stock(second));
        decimal("0.4", held(complete.ingredient()));
        decimal("0.2", held(second));
        assertEquals(
                "COOKING",
                sessions.detail(complete.session())
                        .orders()
                        .getFirst()
                        .items()
                        .getFirst()
                        .status()
                        .name());
    }

    @Test
    void reservationUpdatesCompeteForOneTargetWithoutOverlap() throws Exception {
        Fixture a = fixture("0", null, "0");
        Fixture b = fixture("0", null, "0");
        Fixture target = fixture("0", null, "0");
        actor(Role.MANAGER);
        Instant start = Instant.parse("2026-11-02T10:00:00Z");
        Instant end = start.plusSeconds(3600);
        Long first =
                reservations
                        .create(
                                new CreateReservationRequest(
                                        a.table(), "A", "0912345678", 2, start, end, null))
                        .id();
        Long second =
                reservations
                        .create(
                                new CreateReservationRequest(
                                        b.table(), "B", "0912345678", 2, start, end, null))
                        .id();
        CreateReservationRequest change =
                new CreateReservationRequest(
                        target.table(), "Updated", "0912345678", 2, start, end, null);
        List<Boolean> results =
                race(
                        () -> reservations.update(first, change),
                        () -> reservations.update(second, change));
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    }

    @Test
    void migrationPreservesLegacyHistoryAndEighteenDomainTables() throws Exception {
        String legacy = "migration_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure()
                .dataSource(url, "kltn_test", "")
                .schemas(legacy)
                .defaultSchema(legacy)
                .target("1")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "kltn_test", "")) {
            connection.setSchema(legacy);
            try (Statement query = connection.createStatement()) {
                query.execute(
                        """
                        insert into account values (1,'Legacy','0912345678','hash','CASHIER','ACTIVE');
                        insert into dining_area values (1,'Tầng 1','ACTIVE');
                        insert into dining_table values (1,1,'T01',4,'AVAILABLE');
                        insert into service_session values (1,1,null,1,now(),now(),'CLOSED');
                        insert into menu_item values (1,'Legacy dish',50000,'ON_SALE');
                        insert into ingredient values (1,'Legacy ingredient','kg',0,false,false,'ACTIVE');
                        insert into food_order values (1,1,1,now());
                        insert into order_item values (1,1,1,1,null,'READY',null,50000);
                        insert into stock_movement values (1,1,null,null,1,null,1,'CONSUMPTION',-1,null,now());
                        insert into invoice values (1,1,1,'CASH',50000,now());
                        """);
            }
        }
        Flyway migrated =
                Flyway.configure()
                        .dataSource(url, "kltn_test", "")
                        .schemas(legacy)
                        .defaultSchema(legacy)
                        .load();
        migrated.migrate();
        migrated.validate();
        assertEquals(0, migrated.migrate().migrationsExecuted);
        try (Connection connection = DriverManager.getConnection(url, "kltn_test", "")) {
            connection.setSchema(legacy);
            try (Statement query = connection.createStatement();
                    ResultSet row =
                            query.executeQuery(
                                    """
                                    select (select count(*) from information_schema.tables where table_schema = current_schema() and table_name <> 'flyway_schema_history'),
                                           t.name, t.code, a.code, v.amount, sm.quantity_change, i.status, i.ingredient_requirements
                                    from dining_table t join dining_area a on a.id=t.area_id cross join invoice v cross join stock_movement sm cross join order_item i
                                    """)) {
                assertTrue(row.next());
                assertEquals(18, row.getInt(1));
                assertEquals("T01", row.getString(2));
                assertEquals("T01-B01", row.getString(3));
                assertEquals("T01", row.getString(4));
                decimal("50000", row.getBigDecimal(5));
                decimal("-1", row.getBigDecimal(6));
                assertEquals("READY", row.getString(7));
                assertEquals(null, row.getString(8));
            }
        }
    }

    @Test
    void receiptAndStartSerializeWithoutLosingAvailableStock() throws Exception {
        Fixture f = fixture("0.5", "0.8", "0");
        race(
                () -> kitchen.start(f.item(), false),
                () ->
                        inventory.createReceipt(
                                new CreateStockReceiptRequest(
                                        List.of(
                                                new vn.ueh.kltn.restaurant.dto.request
                                                        .StockReceiptItemRequest(
                                                        f.ingredient(),
                                                        new BigDecimal("0.5"),
                                                        "RACE-RECEIPT",
                                                        null)))));
        decimal("1", stock(f.ingredient()));
        assertTrue(stock(f.ingredient()).subtract(held(f.ingredient())).signum() >= 0);
    }

    @Test
    void lotMetadataReceiptsAndReadPermissionsWorkOverRealHttp() throws Exception {
        long ingredientId =
                call(
                                Role.OWNER,
                                "POST",
                                "/inventory/ingredients",
                                """
                                {"name":"Lots %s","unit":"kg","minStock":0,"trackLot":false,"trackExpiry":false}
                                """
                                        .formatted(UUID.randomUUID()),
                                200)
                        .get("id")
                        .asLong();
        String listPath = "/inventory/ingredients/" + ingredientId + "/lots";
        assertEquals(0, call(Role.OWNER, "GET", listPath, null, 200).size());
        for (Role role : Role.values()) {
            call(
                    role,
                    "GET",
                    listPath,
                    null,
                    role == Role.OWNER || role == Role.HEAD_CHEF ? 200 : 403);
        }
        HttpResponse<String> unauthenticated =
                http.send(
                        HttpRequest.newBuilder(URI.create(api + listPath)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthenticated.statusCode());
        for (String invalid : List.of("-1", "91", "1.5")) {
            call(Role.OWNER, "GET", listPath + "?days=" + invalid, null, 400);
            call(Role.OWNER, "GET", "/inventory/lots/attention?days=" + invalid, null, 400);
        }
        call(Role.OWNER, "GET", listPath + "?days=0", null, 200);
        call(Role.OWNER, "GET", listPath + "?days=90", null, 200);
        call(Role.OWNER, "GET", "/inventory/lots/attention?days=0", null, 200);
        call(Role.OWNER, "GET", "/inventory/lots/attention?days=90", null, 200);
        call(Role.OWNER, "GET", "/inventory/ingredients/9223372036854775807/lots", null, 404);
        LocalDate today =
                LocalDate.now(context.getBean(Clock.class).withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        List<String> dates =
                List.of(
                        today.minusDays(2).toString(),
                        today.toString(),
                        today.plusDays(5).toString(),
                        today.plusDays(120).toString());
        for (String date : dates) {
            call(
                    Role.HEAD_CHEF,
                    "POST",
                    "/inventory/receipts",
                    """
                    {"items":[{"ingredientId":%d,"quantity":2,"lotNumber":"  SAME  ","expiryDate":"%s"}]}
                    """
                            .formatted(ingredientId, date),
                    200);
        }
        call(
                Role.HEAD_CHEF,
                "POST",
                "/inventory/receipts",
                """
                {"items":[{"ingredientId":%d,"quantity":1,"lotNumber":"NO-DATE"}]}
                """
                        .formatted(ingredientId),
                200);
        JsonNode lots = call(Role.HEAD_CHEF, "GET", listPath + "?days=7", null, 200);
        assertEquals(5, lots.size());
        assertEquals("EXPIRED", lots.get(0).get("status").asText());
        assertEquals("EXPIRING_SOON", lots.get(1).get("status").asText());
        assertEquals(0, lots.get(1).get("daysRemaining").asLong());
        assertEquals("EXPIRING_SOON", lots.get(2).get("status").asText());
        assertEquals("VALID", lots.get(3).get("status").asText());
        assertEquals("NO_EXPIRY", lots.get(4).get("status").asText());
        assertTrue(lots.get(4).get("expiryDate").isNull());
        assertEquals("SAME", lots.get(0).get("lotNumber").asText());
        assertTrue(lots.get(0).get("id").asLong() != lots.get(1).get("id").asLong());
        assertEquals(2, lots.get(0).get("initialQuantity").asInt());
        assertEquals(
                "VALID",
                call(Role.OWNER, "GET", listPath, null, 200).get(2).get("status").asText(),
                "Omitted days must retain configured default3");
        JsonNode ingredients = call(Role.OWNER, "GET", "/inventory/ingredients", null, 200);
        JsonNode row = null;
        for (JsonNode ingredient : ingredients) {
            if (ingredient.get("id").asLong() == ingredientId) row = ingredient;
        }
        assertNotNull(row);
        assertEquals(today.minusDays(2).toString(), row.get("nearestExpiryDate").asText());
        assertEquals(5, row.get("lotCount").asInt());
        assertEquals(9, row.get("currentStock").asInt());
        assertEquals(0, row.get("reservedStock").asInt());
        assertEquals(9, row.get("availableStock").asInt());
    }

    @Test
    void invalidLotRollsBackEveryReceiptRowAndCannotBypassHttpValidation() throws Exception {
        Fixture f = fixture("3", null, "0");
        actor(Role.HEAD_CHEF);
        IngredientLotRepository lots = context.getBean(IngredientLotRepository.class);
        StockReceiptRepository receipts = context.getBean(StockReceiptRepository.class);
        StockReceiptItemRepository receiptItems = context.getBean(StockReceiptItemRepository.class);
        StockMovementRepository movements = context.getBean(StockMovementRepository.class);
        List<Long> before =
                List.of(lots.count(), receipts.count(), receiptItems.count(), movements.count());
        assertThrows(
                BusinessException.class,
                () ->
                        inventory.createReceipt(
                                new CreateStockReceiptRequest(
                                        List.of(
                                                new StockReceiptItemRequest(
                                                        f.ingredient(),
                                                        BigDecimal.ONE,
                                                        "FIRST-VALID",
                                                        null),
                                                new StockReceiptItemRequest(
                                                        f.ingredient(),
                                                        BigDecimal.ONE,
                                                        "   ",
                                                        null)))));
        assertEquals(
                before,
                List.of(lots.count(), receipts.count(), receiptItems.count(), movements.count()));
        decimal("3", stock(f.ingredient()));
        for (String invalid : List.of("null", "\"   \"", "\"" + "X".repeat(51) + "\"")) {
            call(
                    Role.HEAD_CHEF,
                    "POST",
                    "/inventory/receipts",
                    "{\"items\":[{\"ingredientId\":"
                            + f.ingredient()
                            + ",\"quantity\":1,\"lotNumber\":"
                            + invalid
                            + "}]}",
                    400);
        }
        long tracked =
                call(
                                Role.OWNER,
                                "POST",
                                "/inventory/ingredients",
                                """
                                {"name":"Expiry %s","unit":"kg","minStock":0,"trackLot":false,"trackExpiry":true}
                                """
                                        .formatted(UUID.randomUUID()),
                                200)
                        .get("id")
                        .asLong();
        call(
                Role.HEAD_CHEF,
                "POST",
                "/inventory/receipts",
                """
                {"items":[{"ingredientId":%d,"quantity":1,"lotNumber":"NEEDS-DATE"}]}
                """
                        .formatted(tracked),
                400);
        assertEquals(
                before,
                List.of(lots.count(), receipts.count(), receiptItems.count(), movements.count()));
    }

    @Test
    void ownerManagesAreasAndTablesWithoutHidingActiveServiceOrReservations() throws Exception {
        String key =
                ("QA" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                        .toUpperCase(Locale.ROOT);
        long area =
                call(
                                Role.OWNER,
                                "POST",
                                "/areas",
                                "{\"name\":\"Khu thử\",\"code\":\"" + key + "\"}",
                                200)
                        .get("id")
                        .asLong();
        long otherArea =
                call(
                                Role.OWNER,
                                "POST",
                                "/areas",
                                "{\"name\":\"Khu khác\",\"code\":\"" + key + "X\"}",
                                200)
                        .get("id")
                        .asLong();
        String body =
                "{\"areaId\":"
                        + area
                        + ",\"name\":\"Bàn thử\",\"code\":\" "
                        + key.toLowerCase()
                        + "-B01 \",\"capacity\":6}";
        JsonNode created = call(Role.OWNER, "POST", "/tables", body, 200);
        long table = created.get("id").asLong();
        assertEquals(key + "-B01", created.get("code").asText());
        for (Role role : Role.values()) {
            call(role, "GET", "/areas", null, role == Role.OWNER ? 200 : 403);
            call(role, "GET", "/tables", null, role == Role.OWNER ? 200 : 403);
            if (role != Role.OWNER) {
                call(role, "PATCH", "/tables/" + table + "/deactivate", null, 403);
                call(
                        role,
                        "POST",
                        "/areas",
                        "{\"name\":\"Không được tạo\",\"code\":\"DENIED\"}",
                        403);
            }
        }
        call(Role.OWNER, "POST", "/tables", body, 409);
        call(
                Role.OWNER,
                "POST",
                "/areas",
                "{\"name\":\"Trùng\",\"code\":\"" + key.toLowerCase() + "\"}",
                409);
        call(Role.OWNER, "POST", "/tables", body.replace("\"capacity\":6", "\"capacity\":0"), 400);
        Instant start = Instant.now().plusSeconds(3600);
        String booking =
                "{\"tableId\":"
                        + table
                        + ",\"customerName\":\"Khách\",\"customerPhone\":\"0911111111\",\"guestCount\":4,\"startTime\":\""
                        + start
                        + "\",\"endTime\":\""
                        + start.plusSeconds(3600)
                        + "\"}";
        long reservation =
                call(Role.MANAGER, "POST", "/reservations", booking, 200).get("id").asLong();
        call(Role.OWNER, "PATCH", "/tables/" + table + "/deactivate", null, 409);
        call(Role.OWNER, "PATCH", "/areas/" + area + "/deactivate", null, 409);
        call(
                Role.OWNER,
                "PUT",
                "/tables/" + table,
                body.replace("\"capacity\":6", "\"capacity\":2"),
                409);
        call(
                Role.OWNER,
                "PUT",
                "/tables/" + table,
                body.replace("\"areaId\":" + area + ",", "\"areaId\":" + otherArea + ","),
                409);
        call(Role.OWNER, "PUT", "/tables/" + table, body.replace("Bàn thử", "Bàn đổi tên"), 200);
        call(Role.MANAGER, "PATCH", "/reservations/" + reservation + "/cancel", null, 200);
        String expired =
                booking.replace(start.toString(), start.minusSeconds(10800).toString())
                        .replace(
                                start.plusSeconds(3600).toString(),
                                start.minusSeconds(7200).toString());
        call(Role.MANAGER, "POST", "/reservations", expired, 200);
        assertEquals(
                "INACTIVE",
                call(Role.OWNER, "PATCH", "/tables/" + table + "/deactivate", null, 200)
                        .get("status")
                        .asText());
        call(Role.OWNER, "PATCH", "/tables/" + table + "/reactivate", null, 200);
        call(Role.OWNER, "PATCH", "/areas/" + area + "/deactivate", null, 200);
        call(Role.WAITER, "POST", "/sessions", "{\"tableId\":" + table + "}", 409);
        call(Role.MANAGER, "POST", "/reservations", booking, 409);
        call(Role.OWNER, "POST", "/tables", body.replace("-B01", "-B02"), 409);
        call(Role.OWNER, "PATCH", "/areas/" + area + "/reactivate", null, 200);
        call(Role.WAITER, "POST", "/sessions", "{\"tableId\":" + table + "}", 200);
        call(Role.OWNER, "PATCH", "/tables/" + table + "/deactivate", null, 409);
        call(Role.OWNER, "PATCH", "/areas/" + area + "/deactivate", null, 409);
    }

    @Test
    void areaDeactivationSerializesWithOpeningAndCreatingReservations() throws Exception {
        TableService admin = context.getBean(TableService.class);
        for (boolean book : List.of(false, true)) {
            String code = "RACE" + UUID.randomUUID().toString().replace("-", "");
            Long area = admin.createArea(new SaveDiningAreaRequest("Race", code)).id();
            Long table =
                    admin.createTable(new SaveDiningTableRequest(area, "Race", code + "-B1", 4))
                            .id();
            Instant start = Instant.now().plusSeconds(3600);
            List<Boolean> result =
                    race(
                            () -> admin.deactivateArea(area),
                            () ->
                                    book
                                            ? reservations.create(
                                                    new CreateReservationRequest(
                                                            table,
                                                            "Khách",
                                                            "0912345678",
                                                            2,
                                                            start,
                                                            start.plusSeconds(3600),
                                                            null))
                                            : sessions.open(new OpenSessionRequest(table, null)));
            assertEquals(
                    1,
                    result.stream().filter(Boolean::booleanValue).count(),
                    "Cannot both stop an area and open/book its table");
            boolean stopped =
                    context.getBean(DiningAreaRepository.class)
                                    .findById(area)
                                    .orElseThrow()
                                    .getStatus()
                            == AreaStatus.INACTIVE;
            assertEquals(result.getFirst(), stopped);
        }
    }

    @Test
    void menuDefaultIsOnSaleFirstAndNewestIdFirstWithinEachStatus() throws Exception {
        String name = "Sort" + UUID.randomUUID().toString().replace("-", "");
        long old =
                call(
                                Role.OWNER,
                                "POST",
                                "/menu-items",
                                "{\"name\":\"" + name + "Old\",\"price\":1}",
                                200)
                        .get("id")
                        .asLong();
        long recent =
                call(
                                Role.OWNER,
                                "POST",
                                "/menu-items",
                                "{\"name\":\"" + name + "New\",\"price\":1}",
                                200)
                        .get("id")
                        .asLong();
        long stopped =
                call(
                                Role.OWNER,
                                "POST",
                                "/menu-items",
                                "{\"name\":\"" + name + "Stopped\",\"price\":1}",
                                200)
                        .get("id")
                        .asLong();
        call(Role.OWNER, "PATCH", "/menu-items/" + stopped + "/discontinue", null, 200);
        JsonNode list = call(Role.OWNER, "GET", "/menu-items?q=" + name, null, 200);
        assertEquals(
                List.of(recent, old, stopped),
                List.of(
                        list.get(0).get("id").asLong(),
                        list.get(1).get("id").asLong(),
                        list.get(2).get("id").asLong()));
        assertEquals(
                1,
                call(Role.OWNER, "GET", "/menu-items?q=" + name + "&status=DISCONTINUED", null, 200)
                        .size());
    }

    @Test
    void paymentCommitFailureRollsBackInvoiceSessionAndTable() throws Exception {
        Fixture f = fixture("0", null, "0");
        sessions.cancel(f.item());
        sql(
                "create function reject_close_"
                        + f.session()
                        + "() returns trigger language plpgsql as $$ begin raise exception"
                        + " 'isolated payment rollback'; end $$; create trigger reject_close_"
                        + f.session()
                        + " before update on service_session for each row when (new.id = "
                        + f.session()
                        + " and new.status = 'CLOSED') execute function reject_close_"
                        + f.session()
                        + "()");
        call(
                Role.CASHIER,
                "POST",
                "/payments",
                "{\"sessionId\":" + f.session() + ",\"paymentMethod\":\"CASH\"}",
                500);
        assertEquals("ACTIVE", sessions.detail(f.session()).status().name());
        assertFalse(context.getBean(InvoiceRepository.class).existsBySessionId(f.session()));
        assertEquals(
                "OCCUPIED",
                context.getBean(DiningTableRepository.class)
                        .findById(f.table())
                        .orElseThrow()
                        .getStatus()
                        .name());
    }

    @Test
    void openingAndEditingReservationCannotCrossTableLinks() throws Exception {
        Fixture f = fixture("0", null, "0");
        Fixture target = fixture("0", null, "0");
        // Release this fixture through the normal zero-amount payment workflow first.
        sessions.cancel(f.item());
        call(
                Role.CASHIER,
                "POST",
                "/payments",
                "{\"sessionId\":" + f.session() + ",\"paymentMethod\":\"CASH\"}",
                200);
        actor(Role.MANAGER);
        Instant start = Instant.now().plusSeconds(60);
        Instant end = start.plusSeconds(3600);
        Long id =
                reservations
                        .create(
                                new CreateReservationRequest(
                                        f.table(),
                                        "Concurrent guest",
                                        "0912345678",
                                        2,
                                        start,
                                        end,
                                        null))
                        .id();
        List<Boolean> result =
                race(
                        () ->
                                reservations.update(
                                        id,
                                        new CreateReservationRequest(
                                                target.table(),
                                                "Moved guest",
                                                "0912345678",
                                                2,
                                                start,
                                                end,
                                                null)),
                        () -> sessions.open(new OpenSessionRequest(f.table(), id)));
        assertEquals(1, result.stream().filter(Boolean::booleanValue).count());
    }

    @Test
    void startPublishesOrderSseAfterReservationCommits() throws Exception {
        Fixture f = fixture("1", "0.8", "0");
        HttpRequest streamRequest =
                HttpRequest.newBuilder(URI.create(api + "/events"))
                        .header("Authorization", "Bearer " + tokens.get(Role.WAITER))
                        .GET()
                        .build();
        HttpResponse<InputStream> stream =
                http.send(streamRequest, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, stream.statusCode());
        try (InputStream input = stream.body();
                ExecutorService reader = Executors.newSingleThreadExecutor()) {
            Future<String> signal =
                    reader.submit(
                            () -> {
                                BufferedReader lines =
                                        new BufferedReader(
                                                new InputStreamReader(
                                                        input, StandardCharsets.UTF_8));
                                String line;
                                while ((line = lines.readLine()) != null)
                                    if (line.startsWith("data:")) return line.substring(5).trim();
                                throw new IllegalStateException("SSE closed before an ORDER event");
                            });
            try {
                call(Role.HEAD_CHEF, "PATCH", "/kitchen/items/" + f.item() + "/start", "{}", 200);
                JsonNode event = json.readTree(signal.get(5, TimeUnit.SECONDS));
                assertEquals("ORDER", event.path("type").asString());
                assertEquals(f.session().longValue(), event.path("sessionId").asLong());
                decimal("0.8", held(f.ingredient()));
            } finally {
                input.close();
            }
        }
    }
}
