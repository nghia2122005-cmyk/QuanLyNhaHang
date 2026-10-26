package vn.ueh.kltn.restaurant.dto.response;

import java.time.Instant;

public record LoginResponse(String accessToken, Instant expiresAt, AccountResponse account) { }
