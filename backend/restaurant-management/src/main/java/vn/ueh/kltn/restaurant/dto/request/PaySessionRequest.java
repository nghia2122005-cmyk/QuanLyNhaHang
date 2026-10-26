package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.ueh.kltn.restaurant.enums.PaymentMethod;

public record PaySessionRequest(
        @NotNull(message = "Mã phiên phục vụ không được để trống") Long sessionId,
        @NotNull(message = "Phương thức thanh toán không được để trống")
                PaymentMethod paymentMethod) {}
