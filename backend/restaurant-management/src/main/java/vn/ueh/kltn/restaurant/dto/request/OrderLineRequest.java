package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.ueh.kltn.restaurant.validation.OrderLimits;

public record OrderLineRequest(
        @NotNull(message = "Vui lòng chọn món") @Positive(message = "Mã món không hợp lệ")
                Long menuItemId,
        @NotNull(message = "Vui lòng nhập số lượng")
                @Positive(message = "Số lượng món phải lớn hơn 0")
                @Max(value = OrderLimits.MAX_QUANTITY_PER_LINE, message = "Mỗi dòng tối đa 99 phần")
                Integer quantity,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note) {}
