package vn.ueh.kltn.restaurant.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import tools.jackson.databind.json.JsonMapper;

import vn.ueh.kltn.restaurant.controller.AccountController;
import vn.ueh.kltn.restaurant.controller.InventoryController;
import vn.ueh.kltn.restaurant.controller.MenuController;
import vn.ueh.kltn.restaurant.controller.ServiceSessionController;
import vn.ueh.kltn.restaurant.controller.TableController;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.exception.GlobalExceptionHandler;
import vn.ueh.kltn.restaurant.security.ApiSecurityErrorHandler;
import vn.ueh.kltn.restaurant.service.AccountService;
import vn.ueh.kltn.restaurant.service.InventoryService;
import vn.ueh.kltn.restaurant.service.MenuService;
import vn.ueh.kltn.restaurant.service.ReservationService;
import vn.ueh.kltn.restaurant.service.ServiceSessionService;
import vn.ueh.kltn.restaurant.service.TableService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

class PhaseOneSecurityTest {
    private record Endpoint(HttpMethod method, String path, String body, Set<Role> allowed) {}

    private static Endpoint endpoint(HttpMethod method, String path, String body, Role... roles) {
        return new Endpoint(method, path, body, Set.of(roles));
    }

    @Test
    void completeSixRoleMatrixUsesActualControllerAnnotations() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfig.class);
            context.refresh();
            MockMvc mvc =
                    MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            List<PhaseOneSecurityTest.Endpoint> endpoints =
                    List.of(
                            endpoint(HttpMethod.GET, "/api/accounts", null, Role.OWNER),
                            endpoint(HttpMethod.GET, "/api/areas", null, Role.OWNER),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/areas",
                                    "{\"name\":\"Khu mới\",\"code\":\"KM\"}",
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PUT,
                                    "/api/areas/1",
                                    "{\"name\":\"Khu mới\",\"code\":\"KM\"}",
                                    Role.OWNER),
                            endpoint(HttpMethod.PATCH, "/api/areas/1/deactivate", null, Role.OWNER),
                            endpoint(HttpMethod.PATCH, "/api/areas/1/reactivate", null, Role.OWNER),
                            endpoint(HttpMethod.GET, "/api/tables", null, Role.OWNER),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/tables",
                                    "{\"areaId\":1,\"name\":\"Bàn"
                                            + " mới\",\"code\":\"KM-B01\",\"capacity\":4}",
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PUT,
                                    "/api/tables/1",
                                    "{\"areaId\":1,\"name\":\"Bàn"
                                            + " mới\",\"code\":\"KM-B01\",\"capacity\":4}",
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PATCH, "/api/tables/1/deactivate", null, Role.OWNER),
                            endpoint(
                                    HttpMethod.PATCH, "/api/tables/1/reactivate", null, Role.OWNER),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/accounts",
                                    "{\"fullName\":\"Nhân"
                                        + " viên\",\"phone\":\"0910000001\",\"password\":\"Demo@123\",\"role\":\"WAITER\"}",
                                    Role.OWNER),
                            endpoint(HttpMethod.PATCH, "/api/accounts/2/lock", null, Role.OWNER),
                            endpoint(HttpMethod.PATCH, "/api/accounts/2/unlock", null, Role.OWNER),
                            endpoint(
                                    HttpMethod.PATCH,
                                    "/api/accounts/2/role",
                                    "{\"role\":\"WAITER\"}",
                                    Role.OWNER),
                            endpoint(HttpMethod.GET, "/api/menu-items", null, Role.values()),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/menu-items",
                                    "{\"name\":\"Phở\",\"price\":50000}",
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PUT,
                                    "/api/menu-items/1",
                                    "{\"name\":\"Phở\",\"price\":55000}",
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PATCH,
                                    "/api/menu-items/1/discontinue",
                                    null,
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.PATCH,
                                    "/api/menu-items/1/reactivate",
                                    null,
                                    Role.OWNER),
                            endpoint(
                                    HttpMethod.GET,
                                    "/api/menu-items/1/recipe",
                                    null,
                                    Role.OWNER,
                                    Role.HEAD_CHEF),
                            endpoint(
                                    HttpMethod.PUT,
                                    "/api/menu-items/1/recipe",
                                    "{\"lines\":[]}",
                                    Role.OWNER,
                                    Role.HEAD_CHEF),
                            endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/ingredients",
                                    null,
                                    Role.OWNER,
                                    Role.HEAD_CHEF),
                            endpoint(
                                    HttpMethod.GET,
                                    "/api/tables/floor-plan",
                                    null,
                                    Role.OWNER,
                                    Role.MANAGER,
                                    Role.CASHIER,
                                    Role.WAITER),
                            endpoint(
                                    HttpMethod.GET,
                                    "/api/reservations",
                                    null,
                                    Role.OWNER,
                                    Role.MANAGER,
                                    Role.WAITER),
                            endpoint(
                                    HttpMethod.GET,
                                    "/api/reservations/available-tables?startTime=2026-10-23T10:00:00Z&endTime=2026-10-23T12:00:00Z&guestCount=2",
                                    null,
                                    Role.MANAGER),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/reservations",
                                    "{\"tableId\":1,\"customerName\":\"Khách\",\"customerPhone\":\"0910000001\",\"guestCount\":2,\"startTime\":\"2026-10-23T10:00:00Z\",\"endTime\":\"2026-10-23T12:00:00Z\"}",
                                    Role.MANAGER),
                            endpoint(
                                    HttpMethod.PUT,
                                    "/api/reservations/1",
                                    "{\"tableId\":1,\"customerName\":\"Khách\",\"customerPhone\":\"0910000001\",\"guestCount\":2,\"startTime\":\"2026-10-25T10:00:00Z\",\"endTime\":\"2026-10-25T12:00:00Z\"}",
                                    Role.MANAGER),
                            endpoint(
                                    HttpMethod.PATCH,
                                    "/api/reservations/1/cancel",
                                    null,
                                    Role.MANAGER),
                            endpoint(
                                    HttpMethod.POST,
                                    "/api/sessions",
                                    "{\"tableId\":1}",
                                    Role.MANAGER,
                                    Role.WAITER));
            for (Role role : Role.values()) {
                String token =
                        context.getBean(JwtEncoder.class)
                                .encode(
                                        JwtEncoderParameters.from(
                                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                                JwtClaimsSet.builder()
                                                        .subject("1")
                                                        .expiresAt(Instant.now().plusSeconds(3600))
                                                        .claim("role", role.name())
                                                        .build()))
                                .getTokenValue();
                for (PhaseOneSecurityTest.Endpoint endpoint : endpoints) {
                    MockHttpServletRequestBuilder req =
                            request(endpoint.method(), endpoint.path())
                                    .header("Authorization", "Bearer " + token);
                    if (endpoint.body() != null)
                        req.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
                    mvc.perform(req)
                            .andExpect(
                                    endpoint.allowed().contains(role)
                                            ? status().isOk()
                                            : status().isForbidden());
                }
            }
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
        SecurityConfig.class,
        JwtConfig.class,
        AccountController.class,
        MenuController.class,
        InventoryController.class,
        TableController.class,
        ServiceSessionController.class,
        GlobalExceptionHandler.class
    })
    static class TestConfig {
        @Bean
        AppProperties properties() {
            return new AppProperties(
                    "Asia/Ho_Chi_Minh",
                    false,
                    new AppProperties.Jwt("test-secret-for-phase-one-security-only-32", 12),
                    new AppProperties.Alert(3),
                    new AppProperties.Reservation(60));
        }

        @Bean
        ApiSecurityErrorHandler errors() {
            return new ApiSecurityErrorHandler(JsonMapper.builder().build());
        }

        @Bean
        AccountService accounts() {
            return mock(AccountService.class);
        }

        @Bean
        MenuService menu() {
            return mock(MenuService.class);
        }

        @Bean
        InventoryService ingredients() {
            return mock(InventoryService.class);
        }

        @Bean
        TableService tables() {
            return mock(TableService.class);
        }

        @Bean
        ReservationService reservations() {
            return mock(ReservationService.class);
        }

        @Bean
        ServiceSessionService sessions() {
            return mock(ServiceSessionService.class);
        }
    }
}
