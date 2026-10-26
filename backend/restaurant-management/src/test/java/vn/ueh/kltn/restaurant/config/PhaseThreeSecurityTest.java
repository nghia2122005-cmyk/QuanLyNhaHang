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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import vn.ueh.kltn.restaurant.controller.InventoryController;
import vn.ueh.kltn.restaurant.controller.PaymentController;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.service.InventoryService;
import vn.ueh.kltn.restaurant.service.PaymentService;
import vn.ueh.kltn.restaurant.service.ReconciliationService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

class PhaseThreeSecurityTest {

    private record Endpoint(HttpMethod method, String path, String body, Set<Role> roles) {}

    @Test
    void allPhaseThreeEndpointsEnforceSixRoleMatrixAndAuthentication() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfig.class);
            context.refresh();

            MockMvc mvc =
                    MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            Set<Role> cashierOnly = Set.of(Role.CASHIER);
            Set<Role> cashierManagerOwner = Set.of(Role.CASHIER, Role.MANAGER, Role.OWNER);
            Set<Role> ownerOnly = Set.of(Role.OWNER);
            Set<Role> headChefOnly = Set.of(Role.HEAD_CHEF);
            Set<Role> ownerAndHeadChef = Set.of(Role.OWNER, Role.HEAD_CHEF);

            List<Endpoint> endpoints =
                    List.of(
                            // 1. Payment open sessions: CASHIER
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/payments/open-sessions",
                                    null,
                                    cashierOnly),
                            // 2. Payment session preview: CASHIER
                            new Endpoint(
                                    HttpMethod.GET, "/api/payments/sessions/1", null, cashierOnly),
                            // 3. POST Payment: CASHIER
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/payments",
                                    "{\"sessionId\":1,\"paymentMethod\":\"CASH\"}",
                                    cashierOnly),
                            // 4. Invoices list: CASHIER, MANAGER, OWNER
                            new Endpoint(
                                    HttpMethod.GET, "/api/invoices", null, cashierManagerOwner),
                            // 5. GET Ingredients: OWNER, HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/ingredients",
                                    null,
                                    ownerAndHeadChef),
                            // 6. POST Ingredients: OWNER
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/inventory/ingredients",
                                    "{\"name\":\"Gia"
                                        + " vị\",\"unit\":\"g\",\"minStock\":10,\"trackLot\":false,\"trackExpiry\":false}",
                                    ownerOnly),
                            // 7. PUT Ingredients: OWNER
                            new Endpoint(
                                    HttpMethod.PUT,
                                    "/api/inventory/ingredients/1",
                                    "{\"name\":\"Gia vị"
                                        + " mới\",\"unit\":\"g\",\"minStock\":10,\"trackLot\":false,\"trackExpiry\":false}",
                                    ownerOnly),
                            // 8. PATCH Deactivate: OWNER
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/inventory/ingredients/1/deactivate",
                                    null,
                                    ownerOnly),
                            // 9. PATCH Activate: OWNER
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/inventory/ingredients/1/activate",
                                    null,
                                    ownerOnly),
                            // 10. Attention Lots: OWNER, HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/lots/attention",
                                    null,
                                    ownerAndHeadChef),
                            // 11. POST Receipts: HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/inventory/receipts",
                                    "{\"items\":[{\"ingredientId\":1,\"quantity\":5.0,\"lotNumber\":\"LOT-1\"}]}",
                                    headChefOnly),
                            // 12. GET Receipts: OWNER, HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/receipts",
                                    null,
                                    ownerAndHeadChef),
                            // 13. Stocktake Template: HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/stocktakes/template",
                                    null,
                                    headChefOnly),
                            // 14. POST Stocktakes: HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/inventory/stocktakes",
                                    "{\"items\":[{\"ingredientId\":1,\"actualQuantity\":10.0}]}",
                                    headChefOnly),
                            // 15. GET Stocktakes: OWNER, HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/stocktakes",
                                    null,
                                    ownerAndHeadChef),
                            // 16. POST Disposals: HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/inventory/disposals",
                                    "{\"ingredientId\":1,\"quantity\":2.0,\"reason\":\"Hỏng\"}",
                                    headChefOnly),
                            // 17. GET Movements: OWNER, HEAD_CHEF
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/movements",
                                    null,
                                    ownerAndHeadChef),
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/inventory/ingredients/1/lots",
                                    null,
                                    ownerAndHeadChef));

            // Test unauthenticated access -> 401
            for (Endpoint endpoint : endpoints) {
                MockHttpServletRequestBuilder req = request(endpoint.method(), endpoint.path());
                if (endpoint.body() != null) {
                    req.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
                }
                mvc.perform(req).andExpect(status().isUnauthorized());
            }

            // Test each role
            for (Role role : Role.values()) {
                String token =
                        context.getBean(JwtEncoder.class)
                                .encode(
                                        JwtEncoderParameters.from(
                                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                                JwtClaimsSet.builder()
                                                        .subject("2")
                                                        .expiresAt(Instant.now().plusSeconds(3600))
                                                        .claim("role", role.name())
                                                        .build()))
                                .getTokenValue();

                for (Endpoint endpoint : endpoints) {
                    MockHttpServletRequestBuilder req =
                            request(endpoint.method(), endpoint.path())
                                    .header("Authorization", "Bearer " + token);
                    if (endpoint.body() != null) {
                        req.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
                    }
                    mvc.perform(req)
                            .andExpect(
                                    endpoint.roles().contains(role)
                                            ? status().isOk()
                                            : status().isForbidden());
                }
            }
        }
    }

    @Configuration
    @Import({
        PhaseOneSecurityTest.TestConfig.class,
        PaymentController.class,
        InventoryController.class
    })
    static class TestConfig {
        @Bean
        ReconciliationService reconciliationService() {
            return mock(ReconciliationService.class);
        }

        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        InventoryService inventoryService() {
            return mock(InventoryService.class);
        }
    }
}
