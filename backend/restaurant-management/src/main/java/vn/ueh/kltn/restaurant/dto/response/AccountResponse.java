package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.Role;

public record AccountResponse(Long accountId, String fullName, Role role) { }
