package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.security.ApiSecurityErrorHandler;

class SecurityConfigTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void close() { context.close(); }

    @ParameterizedTest
    @EnumSource(Role.class)
    void roleClaimMapsToExactlyOneRoleAuthority(Role role) {
        var token = Jwt.withTokenValue("test").header("alg", "HS256").subject("1").claim("role", role.name()).build();
        var auth = new SecurityConfig().jwtAuthenticationConverter().convert(token);
        assertEquals(java.util.List.of("ROLE_" + role.name()), auth.getAuthorities().stream()
                .map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).toList());
    }

    @Test
    void missingOrInvalidBearerReceivesJson401() throws Exception {
        mvc.perform(get("/probe/owner")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.fieldErrors").isMap());
        mvc.perform(get("/probe/owner").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void methodSecurityReturnsJson403ForWaiterAndAllowsOwner() throws Exception {
        mvc.perform(get("/probe/owner").header("Authorization", "Bearer " + token(Role.WAITER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/probe/owner").header("Authorization", "Bearer " + token(Role.OWNER)))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Test
    void loginRemainsPublicWithoutSessionCookie() throws Exception {
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    private String token(Role role) {
        return context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject("1").expiresAt(Instant.now().plusSeconds(3600)).claim("role", role.name()).build())).getTokenValue();
    }

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @Import({SecurityConfig.class, JwtConfig.class})
    static class TestConfig {
        @Bean AppProperties properties() {
            return new AppProperties("Asia/Ho_Chi_Minh", false,
                    new AppProperties.Jwt("isolated-test-secret-at-least-64-characters-abcdefghijklmnopqrstuvwxyz", 12),
                    new AppProperties.Alert(3), new AppProperties.Reservation(60));
        }
        @Bean ApiSecurityErrorHandler errors() { return new ApiSecurityErrorHandler(JsonMapper.builder().build()); }
        @Bean ProbeController controller() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/probe/owner")
        @PreAuthorize("hasRole('OWNER')")
        public String owner() { return "ok"; }

        @PostMapping("/api/auth/login")
        public String login() { return "ok"; }
    }
}
