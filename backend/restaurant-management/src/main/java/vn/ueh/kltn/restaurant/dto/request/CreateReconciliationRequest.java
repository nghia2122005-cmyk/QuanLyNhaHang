package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateReconciliationRequest(
        @NotNull(message = "Vui lòng chọn thời điểm bắt đầu") Instant startTime,
        @NotNull(message = "Vui lòng chọn thời điểm kết thúc") Instant endTime,
        @NotNull(message = "Vui lòng nhập tổng thực nhận")
                @DecimalMin(value = "0", message = "Tổng thực nhận không được âm")
                @Digits(
                        integer = 14,
                        fraction = 0,
                        message = "Số tiền phải là số nguyên, tối đa 14 chữ số")
                BigDecimal actualAmount,
        @Size(max = 500, message = "Lý do không quá 500 ký tự") String differenceReason) {}
