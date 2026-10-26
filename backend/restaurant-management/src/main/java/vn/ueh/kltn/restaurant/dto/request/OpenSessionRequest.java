package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OpenSessionRequest(
        @NotNull(message = "Vui lòng chọn bàn") @Positive(message = "Mã bàn phải lớn hơn 0")
                Long tableId,
        @Positive(message = "Mã đặt bàn phải lớn hơn 0") Long reservationId) {}
