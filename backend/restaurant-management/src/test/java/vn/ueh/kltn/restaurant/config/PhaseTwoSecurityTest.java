package vn.ueh.kltn.restaurant.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.ueh.kltn.restaurant.controller.EventController;
import vn.ueh.kltn.restaurant.controller.KitchenController;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.realtime.SseHub;
import vn.ueh.kltn.restaurant.service.KitchenService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

class PhaseTwoSecurityTest {
    private record Endpoint(HttpMethod method, String path, String body, Set<Role> roles) {}

    @Test
    void allTenEndpointsEnforceSixRoleMatrixAndAuthentication() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfig.class);
            context.refresh();
            MockMvc mvc =
                    MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            Set<Role> ordering = Set.of(Role.WAITER, Role.MANAGER);
            Set<Role> waiter = Set.of(Role.WAITER);
            Set<Role> kitchen = Set.of(Role.KITCHEN_STAFF, Role.HEAD_CHEF);
            List<Endpoint> endpoints =
                    List.of(
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/sessions/7",
                                    null,
                                    Set.of(Role.WAITER, Role.MANAGER, Role.CASHIER)),
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/sessions/7/orders",
                                    "{\"items\":[{\"menuItemId\":1,\"quantity\":1}]}",
                                    ordering),
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/sessions/items/9/cancel",
                                    null,
                                    ordering),
                            new Endpoint(
                                    HttpMethod.PATCH, "/api/sessions/items/9/serve", null, waiter),
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/sessions/items/awaiting-service",
                                    null,
                                    Set.of(Role.WAITER, Role.MANAGER, Role.CASHIER, Role.OWNER)),
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/sessions/items/9/acknowledge-unavailable",
                                    null,
                                    waiter),
                            new Endpoint(HttpMethod.GET, "/api/kitchen/orders/8", null, kitchen),
                            new Endpoint(HttpMethod.GET, "/api/kitchen/queue", null, kitchen),
                            new Endpoint(
                                    HttpMethod.PATCH, "/api/kitchen/items/9/start", null, kitchen),
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/kitchen/items/9/unavailable",
                                    "{\"reason\":\"Hết món\"}",
                                    kitchen),
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/kitchen/items/9/complete",
                                    null,
                                    kitchen),
                            new Endpoint(
                                    HttpMethod.GET, "/api/events", null, Set.of(Role.values())));
            for (Endpoint endpoint : endpoints) {
                mvc.perform(request(endpoint.method(), endpoint.path()))
                        .andExpect(status().isUnauthorized());
            }
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
                    if (endpoint.body() != null)
                        req.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
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
    @Import({PhaseOneSecurityTest.TestConfig.class, KitchenController.class, EventController.class})
    static class TestConfig {
        @Bean
        KitchenService kitchen() {
            return mock(KitchenService.class);
        }

        @Bean
        SseHub hub() {
            SseHub hub = mock(SseHub.class);
            when(hub.subscribe()).thenAnswer(invocation -> new SseEmitter(30 * 60 * 1000L));
            return hub;
        }
    }
}
