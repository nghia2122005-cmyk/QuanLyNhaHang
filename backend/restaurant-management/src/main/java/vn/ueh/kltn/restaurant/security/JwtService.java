package vn.ueh.kltn.restaurant.security;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import vn.ueh.kltn.restaurant.config.AppProperties;
import vn.ueh.kltn.restaurant.dto.response.LoginResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.mapper.AccountMapper;

@Component
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;
    private final AppProperties properties;
    private final Clock clock;

    public LoginResponse issue(Account account) {
        var now = clock.instant();
        var expiresAt = now.plus(properties.jwt().expirationHours(), ChronoUnit.HOURS);
        var claims = JwtClaimsSet.builder().subject(account.getId().toString())
                .issuedAt(now).expiresAt(expiresAt).claim("fullName", account.getFullName())
                .claim("role", account.getRole().name()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, expiresAt, AccountMapper.toResponse(account));
    }
}
