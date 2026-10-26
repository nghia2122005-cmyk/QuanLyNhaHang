package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveRecipeRequest(
        @NotNull(message = "Vui lòng gửi danh sách định lượng")
                List<
                                @Valid @NotNull(message = "Dòng định lượng không được để trống")
                                RecipeLineRequest>
                        lines) {}
