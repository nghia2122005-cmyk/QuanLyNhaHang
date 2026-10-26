package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateReservationRequest(
        @NotNull(message = "Vui lòng chọn bàn") @Positive(message = "Mã bàn phải lớn hơn 0")
                Long tableId,
        @NotBlank(message = "Vui lòng nhập tên khách")
                @Size(max = 100, message = "Tên khách tối đa 100 ký tự")
                String customerName,
        @NotBlank(message = "Vui lòng nhập số điện thoại")
                @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
                @Pattern(
                        regexp = "^(?:\\s*|0\\d{9})$",
                        message = "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0")
                String customerPhone,
        @NotNull(message = "Vui lòng nhập số khách") @Positive(message = "Số khách phải lớn hơn 0")
                Integer guestCount,
        @NotNull(message = "Vui lòng chọn giờ bắt đầu") Instant startTime,
        @NotNull(message = "Vui lòng chọn giờ kết thúc") Instant endTime,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note) {}
