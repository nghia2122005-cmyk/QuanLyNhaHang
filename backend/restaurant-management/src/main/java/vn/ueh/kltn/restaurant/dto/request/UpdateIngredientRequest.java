package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateIngredientRequest(
        @NotBlank(message = "Tên nguyên vật liệu không được để trống")
                @Size(max = 100, message = "Tên nguyên vật liệu không vượt quá 100 ký tự")
                String name,
        @NotBlank(message = "Đơn vị tính không được để trống")
                @Size(max = 20, message = "Đơn vị tính không vượt quá 20 ký tự")
                String unit,
        @NotNull(message = "Mức tồn kho tối thiểu không được để trống")
                @DecimalMin(value = "0.000", message = "Mức tồn kho tối thiểu phải lớn hơn hoặc bằng 0")
                BigDecimal minStock,
        Boolean trackLot,
        Boolean trackExpiry) {

    public UpdateIngredientRequest {
        trackLot = Boolean.TRUE.equals(trackLot);
        trackExpiry = Boolean.TRUE.equals(trackExpiry);
    }
}
