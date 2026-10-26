package vn.ueh.kltn.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateStocktakeRequest(
        @NotEmpty(message = "Danh sách chi tiết kiểm kê không được để trống")
                List<@Valid StocktakeItemRequest> items) {}
