package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.security.JwtService;

class JwtConfigTest {
    private final JwtConfig config = new JwtConfig();
    private final AppProperties properties = properties("test-secret-for-unit-tests-only-32-characters");

    private static AppProperties properties(String secret) {
        return new AppProperties("Asia/Ho_Chi_Minh", false, new AppProperties.Jwt(secret, 12),
                new AppProperties.Alert(3), new AppProperties.Reservation(60));
    }

    @Test
    void tokenUsesHs256IdentityRoleAndTwelveHourExpiry() {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var key = config.jwtSecretKey(properties);
        var service = new JwtService(config.jwtEncoder(key), properties, Clock.fixed(now, ZoneOffset.UTC));
        var account = new Account("Chủ quán", "0900000001", "bcrypt-hash", Role.OWNER);
        ReflectionTestUtils.setField(account, "id", 1L);
        var response = service.issue(account);
        var decoded = config.jwtDecoder(key).decode(response.accessToken());
        assertEquals("HS256", decoded.getHeaders().get("alg"));
        assertEquals("1", decoded.getSubject());
        assertEquals("OWNER", decoded.getClaimAsString("role"));
        assertEquals("Chủ quán", decoded.getClaimAsString("fullName"));
        assertEquals(now.plusSeconds(12 * 3600), decoded.getExpiresAt());
        assertEquals(decoded.getExpiresAt(), response.expiresAt());
    }

    @Test
    void expiredTokenIsRejected() {
        var key = config.jwtSecretKey(properties);
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("1").issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600)).claim("role", "OWNER").build();
        var token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertThrows(JwtException.class, () -> config.jwtDecoder(key).decode(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        var differentKey = config.jwtSecretKey(properties("another-test-secret-that-is-at-least-32-characters"));
        var claims = JwtClaimsSet.builder().subject("1").expiresAt(Instant.now().plusSeconds(3600)).claim("role", "OWNER").build();
        var token = config.jwtEncoder(differentKey).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertThrows(JwtException.class, () -> config.jwtDecoder(config.jwtSecretKey(properties)).decode(token));
    }

    @Test
    void unsecuredTokenIsRejected() {
        var token = new com.nimbusds.jwt.PlainJWT(new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject("1").claim("role", "OWNER").expirationTime(java.util.Date.from(Instant.now().plusSeconds(3600))).build());
        assertThrows(JwtException.class, () -> config.jwtDecoder(config.jwtSecretKey(properties)).decode(token.serialize()));
    }

    @Test
    void hs512EvenWithTheCorrectSecretIsRejected() throws Exception {
        var longProperties = properties("long-test-secret-for-HS512-abcdefghijklmnopqrstuvwxyz-01234567890123456789");
        var token = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS512),
                new com.nimbusds.jwt.JWTClaimsSet.Builder().subject("1").claim("role", "OWNER")
                        .expirationTime(java.util.Date.from(Instant.now().plusSeconds(3600))).build());
        token.sign(new com.nimbusds.jose.crypto.MACSigner(longProperties.jwt().secret()));
        assertThrows(JwtException.class, () -> config.jwtDecoder(config.jwtSecretKey(longProperties)).decode(token.serialize()));
    }
}
