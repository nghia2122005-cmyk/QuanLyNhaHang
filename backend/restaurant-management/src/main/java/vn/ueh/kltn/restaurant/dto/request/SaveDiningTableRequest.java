package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaveDiningTableRequest(
        @NotNull(message = "Vui lòng chọn khu vực") @Positive(message = "Mã khu vực phải lớn hơn 0")
                Long areaId,
        @NotBlank(message = "Vui lòng nhập tên bàn")
                @Size(max = 50, message = "Tên bàn tối đa 50 ký tự")
                String name,
        @NotBlank(message = "Vui lòng nhập mã bàn")
                @Size(max = 64, message = "Mã bàn tối đa 64 ký tự")
                String code,
        @Positive(message = "Sức chứa phải lớn hơn 0") int capacity) {}
