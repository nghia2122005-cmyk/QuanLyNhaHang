package vn.ueh.kltn.restaurant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RevenueAnalysisResponse(
        BigDecimal averageInvoiceAmount,
        BigDecimal averageDailyRevenue,
        BigDecimal peakRevenue,
        List<String> peakDates,
        long activeDays,
        long calendarDays) {}
