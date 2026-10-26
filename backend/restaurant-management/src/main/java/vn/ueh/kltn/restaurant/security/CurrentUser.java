package vn.ueh.kltn.restaurant.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.enums.Role;

@Component
public class CurrentUser {
    public Long accountId() {
        try {
            return Long.valueOf(authentication().getToken().getSubject());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public Role role() {
        try {
            return Role.valueOf(authentication().getToken().getClaimAsString("role"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private JwtAuthenticationToken authentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return jwt;
    }
}
