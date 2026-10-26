package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecipeLineRequest(
        @NotNull(message = "Vui lòng chọn nguyên vật liệu")
                @Positive(message = "Nguyên vật liệu không hợp lệ")
                Long ingredientId,
        @NotNull(message = "Vui lòng nhập định lượng")
                @DecimalMin(value = "0", inclusive = false, message = "Định lượng phải lớn hơn 0")
                @Digits(
                        integer = 9,
                        fraction = 3,
                        message = "Định lượng tối đa 9 chữ số nguyên và 3 chữ số thập phân")
                BigDecimal quantityPerServing) {}
