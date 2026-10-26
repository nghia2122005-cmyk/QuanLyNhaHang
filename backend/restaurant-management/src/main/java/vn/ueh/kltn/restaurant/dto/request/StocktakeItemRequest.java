package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StocktakeItemRequest(
        @NotNull(message = "Mã nguyên vật liệu không được để trống") Long ingredientId,
        @NotNull(message = "Số lượng thực tế không được để trống")
                @DecimalMin(value = "0.000", message = "Số lượng thực tế phải lớn hơn hoặc bằng 0")
                BigDecimal actualQuantity,
        @Size(max = 500, message = "Ghi chú không vượt quá 500 ký tự") String note) {}
