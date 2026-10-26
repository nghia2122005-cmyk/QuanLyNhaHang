package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.ueh.kltn.restaurant.validation.OrderLimits;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Vui lòng gửi danh sách món")
                @Size(
                        min = 1,
                        max = OrderLimits.MAX_LINES,
                        message = "Mỗi đợt gọi món cần từ 1 đến 50 dòng")
                List<@Valid @NotNull(message = "Dòng món không được để trống") OrderLineRequest>
                        items) {}
