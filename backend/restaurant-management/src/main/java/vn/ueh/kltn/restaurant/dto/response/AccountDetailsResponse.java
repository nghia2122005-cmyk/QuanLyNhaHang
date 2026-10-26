package vn.ueh.kltn.restaurant.dto.response;

import vn.ueh.kltn.restaurant.enums.AccountStatus;
import vn.ueh.kltn.restaurant.enums.Role;

public record AccountDetailsResponse(
        Long id, String fullName, String phone, Role role, AccountStatus status) {}
