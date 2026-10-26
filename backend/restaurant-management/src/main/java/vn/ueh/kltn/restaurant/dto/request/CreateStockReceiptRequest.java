package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateStockReceiptRequest(
        @NotEmpty(message = "Danh sách chi tiết nhập kho không được để trống")
                List<@Valid StockReceiptItemRequest> items) {}
