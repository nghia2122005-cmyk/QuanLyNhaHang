package vn.ueh.kltn.restaurant.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

class CurrentUserTest {
    private final CurrentUser user = new CurrentUser();

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void readsSignedSubjectAndRole() {
        authenticate("4", "WAITER");
        assertEquals(4L, user.accountId());
        assertEquals(Role.WAITER, user.role());
    }

    @Test void absentAuthenticationIsUnauthorized() {
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(BusinessException.class, user::role).getErrorCode());
        assertThrows(BusinessException.class, user::accountId);
    }

    @Test void invalidSubjectAndUnknownRoleAreUnauthorized() {
        authenticate("invalid", "UNKNOWN");
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(BusinessException.class, user::accountId).getErrorCode());
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(BusinessException.class, user::role).getErrorCode());
    }

    private void authenticate(String subject, String role) {
        var jwt = Jwt.withTokenValue("test").header("alg", "HS256").subject(subject).claim("role", role).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }
}
