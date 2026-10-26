package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotNull;

import vn.ueh.kltn.restaurant.enums.Role;

public record ChangeRoleRequest(@NotNull(message = "Vui lòng chọn vai trò") Role role) {}
