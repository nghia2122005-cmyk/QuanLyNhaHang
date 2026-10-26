package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveMenuItemRequest(
        @NotBlank(message = "Vui lòng nhập tên món")
                @Size(max = 100, message = "Tên món tối đa 100 ký tự")
                String name,
        @NotNull(message = "Vui lòng nhập giá món")
                @DecimalMin(value = "0", message = "Giá món phải lớn hơn hoặc bằng 0")
                @Digits(
                        integer = 14,
                        fraction = 0,
                        message = "Giá món là số nguyên tối đa 14 chữ số")
                BigDecimal price) {}
