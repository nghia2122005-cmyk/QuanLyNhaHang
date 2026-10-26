package vn.ueh.kltn.restaurant.dto.response;

import java.util.List;

public record PaymentSummaryResponse(
        RevenueSummaryResponse revenue,
        List<TopMenuItemResponse> topItems,
        List<DailyRevenueResponse> dailyRevenue,
        RevenueAnalysisResponse analysis) {}
