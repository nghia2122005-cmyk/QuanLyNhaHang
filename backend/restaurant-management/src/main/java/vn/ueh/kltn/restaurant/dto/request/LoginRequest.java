package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(?:\\s*|0\\d{9})$", message = "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0") String phone,
        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(max = 72, message = "Mật khẩu không được vượt quá 72 ký tự") String password) {
    @Override
    public String toString() {
        return "LoginRequest[credentials=REDACTED]";
    }
}
