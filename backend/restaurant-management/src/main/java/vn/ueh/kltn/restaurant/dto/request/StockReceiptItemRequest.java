package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockReceiptItemRequest(
        @NotNull(message = "Mã nguyên vật liệu không được để trống") Long ingredientId,
        @NotNull(message = "Số lượng nhập không được để trống")
                @DecimalMin(value = "0.001", message = "Số lượng nhập phải lớn hơn 0")
                BigDecimal quantity,
        @NotBlank(message = "Số lô không được để trống")
                @Size(max = 50, message = "Số lô không vượt quá 50 ký tự")
                String lotNumber,
        LocalDate expiryDate) {}
