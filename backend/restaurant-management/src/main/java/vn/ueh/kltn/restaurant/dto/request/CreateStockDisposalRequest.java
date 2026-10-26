package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateStockDisposalRequest(
        @NotNull(message = "Mã nguyên vật liệu không được để trống") Long ingredientId,
        Long lotId,
        @NotNull(message = "Số lượng xuất hủy không được để trống")
                @DecimalMin(value = "0.001", message = "Số lượng xuất hủy phải lớn hơn 0")
                BigDecimal quantity,
        @NotBlank(message = "Lý do xuất hủy không được để trống")
                @Size(max = 500, message = "Lý do xuất hủy không vượt quá 500 ký tự")
                String reason) {}
