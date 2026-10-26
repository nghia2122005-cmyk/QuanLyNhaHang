package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnavailableRequest(
        @NotBlank(message = "Vui lòng nhập lý do không thể chế biến")
                @Size(max = 500, message = "Lý do tối đa 500 ký tự")
                String reason) {}
