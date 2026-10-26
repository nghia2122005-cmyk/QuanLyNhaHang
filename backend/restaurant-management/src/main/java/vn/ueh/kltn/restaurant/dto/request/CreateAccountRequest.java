package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import vn.ueh.kltn.restaurant.enums.Role;
import vn.ueh.kltn.restaurant.validation.Utf8Size;

public record CreateAccountRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
                @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
                String fullName,
        @NotBlank(message = "Vui lòng nhập số điện thoại")
                @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
                @Pattern(
                        regexp = "^(?:\\s*|0\\d{9})$",
                        message = "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0")
                String phone,
        @NotBlank(message = "Vui lòng nhập mật khẩu")
                @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự")
                @Utf8Size(max = 72)
                String password,
        @NotNull(message = "Vui lòng chọn vai trò") Role role) {}
