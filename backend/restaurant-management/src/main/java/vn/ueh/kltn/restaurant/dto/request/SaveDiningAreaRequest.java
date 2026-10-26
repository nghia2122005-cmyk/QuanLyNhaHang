package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveDiningAreaRequest(
        @NotBlank(message = "Vui lòng nhập tên khu vực")
                @Size(max = 100, message = "Tên khu vực tối đa 100 ký tự")
                String name,
        @NotBlank(message = "Vui lòng nhập mã khu vực")
                @Size(max = 40, message = "Mã khu vực tối đa 40 ký tự")
                String code) {}
