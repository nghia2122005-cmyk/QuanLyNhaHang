package vn.ueh.kltn.restaurant.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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

import vn.ueh.kltn.restaurant.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.Set;

class PhaseFourSecurityTest {
    private record Endpoint(HttpMethod method, String path, String body, Set<Role> roles) {}

    @Test
    void fiveEndpointsEnforceSixRolesAnd401WithRealJwtConverter() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(PhaseThreeSecurityTest.TestConfig.class);
            context.refresh();
            MockMvc mvc =
                    MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            String period = "?from=2026-10-22T17:00:00Z&to=2026-10-23T17:00:00Z";
            List<Endpoint> endpoints =
                    List.of(
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/payments/summary" + period,
                                    null,
                                    Set.of(Role.OWNER)),
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/reconciliations/preview" + period,
                                    null,
                                    Set.of(Role.CASHIER)),
                            new Endpoint(
                                    HttpMethod.GET,
                                    "/api/reconciliations",
                                    null,
                                    Set.of(Role.CASHIER, Role.MANAGER)),
                            new Endpoint(
                                    HttpMethod.POST,
                                    "/api/reconciliations",
                                    "{\"startTime\":\"2026-10-22T17:00:00Z\",\"endTime\":\"2026-10-23T17:00:00Z\",\"actualAmount\":0}",
                                    Set.of(Role.CASHIER)),
                            new Endpoint(
                                    HttpMethod.PATCH,
                                    "/api/reconciliations/1/confirm",
                                    null,
                                    Set.of(Role.MANAGER)));
            for (Endpoint endpoint : endpoints) {
                mvc.perform(build(endpoint)).andExpect(status().isUnauthorized());
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
                    mvc.perform(build(endpoint).header("Authorization", "Bearer " + token))
                            .andExpect(
                                    endpoint.roles().contains(role)
                                            ? status().isOk()
                                            : status().isForbidden());
                }
                if (role == Role.CASHIER) {
                    mvc.perform(
                                    request(HttpMethod.POST, "/api/reconciliations")
                                            .header("Authorization", "Bearer " + token)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"actualAmount\":-1}"))
                            .andExpect(status().isBadRequest());
                    mvc.perform(
                                    request(HttpMethod.GET, "/api/reconciliations/preview")
                                            .header("Authorization", "Bearer " + token))
                            .andExpect(status().isBadRequest());
                    mvc.perform(
                                    request(HttpMethod.GET, "/api/reconciliations?size=101")
                                            .header("Authorization", "Bearer " + token))
                            .andExpect(status().isBadRequest());
                }
            }
        }
    }

    private MockHttpServletRequestBuilder build(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        return endpoint.body() == null
                ? builder
                : builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
    }
}
