package vn.ueh.kltn.restaurant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static vn.ueh.kltn.restaurant.TestFixtures.CLOCK;
import static vn.ueh.kltn.restaurant.TestFixtures.NOW;
import static vn.ueh.kltn.restaurant.TestFixtures.appProperties;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import vn.ueh.kltn.restaurant.dto.response.DailyRevenueResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentMethodSummaryResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentSummaryResponse;
import vn.ueh.kltn.restaurant.dto.response.RevenueSummaryResponse;
import vn.ueh.kltn.restaurant.dto.response.TopMenuItemResponse;
import vn.ueh.kltn.restaurant.enums.OrderItemStatus;
import vn.ueh.kltn.restaurant.enums.PaymentMethod;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.FoodOrderRepository;
import vn.ueh.kltn.restaurant.repository.InvoiceRepository;
import vn.ueh.kltn.restaurant.repository.OrderItemRepository;
import vn.ueh.kltn.restaurant.repository.ServiceSessionRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

class RevenueSummaryTest {
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final PaymentServiceImpl service = paymentService("Asia/Ho_Chi_Minh");

    private PaymentServiceImpl paymentService(String timezone) {
        return paymentService(timezone, CLOCK);
    }

    private PaymentServiceImpl paymentService(String timezone, Clock reportClock) {
        return new PaymentServiceImpl(
                mock(ServiceSessionRepository.class),
                mock(FoodOrderRepository.class),
                invoices,
                mock(AccountRepository.class),
                mock(CurrentUser.class),
                reportClock,
                mock(ApplicationEventPublisher.class),
                items,
                appProperties(timezone));
    }

    @Test
    void emptyPeriodReturnsZeroAndBothMethods() {
        RevenueSummaryResponse result = service.summarizeRevenue(NOW, NOW.plusSeconds(60));
        assertEquals(BigDecimal.ZERO, result.systemRevenue());
        assertEquals(0, result.invoiceCount());
        assertEquals(2, result.methods().size());
        assertEquals(0, result.methods().getFirst().invoiceCount());
    }

    @Test
    void countIncludesZeroInvoicesAndTotalsEqualMethodSum() {
        when(invoices.summarizeRevenue(NOW, NOW.plusSeconds(60)))
                .thenReturn(
                        List.of(
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.CASH, 2, new BigDecimal("100")),
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.BANK_TRANSFER, 1, new BigDecimal("200"))));
        RevenueSummaryResponse result = service.summarizeRevenue(NOW, NOW.plusSeconds(60));
        assertEquals(3, result.invoiceCount());
        assertEquals(new BigDecimal("300"), result.systemRevenue());
        assertEquals(NOW, result.from());
        assertEquals(NOW.plusSeconds(60), result.to());
    }

    @Test
    void topItemAmountsDoNotReplaceOrMultiplyInvoiceTotalAndReadyIsNotExcluded() {
        when(invoices.summarizeRevenue(NOW, NOW.plusSeconds(60)))
                .thenReturn(
                        List.of(
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.CASH, 1, BigDecimal.TEN)));
        when(items.summarizeTopItems(
                        NOW,
                        NOW.plusSeconds(60),
                        OrderItemStatus.NON_BILLABLE,
                        PageRequest.of(0, 10)))
                .thenReturn(List.of(new TopMenuItemResponse(1L, "Món", 5, BigDecimal.TEN)));
        PaymentSummaryResponse result = service.summary(NOW, NOW.plusSeconds(60));
        assertEquals(BigDecimal.TEN, result.revenue().systemRevenue());
        assertEquals(5, result.topItems().getFirst().quantity());
        verify(items)
                .summarizeTopItems(
                        NOW,
                        NOW.plusSeconds(60),
                        OrderItemStatus.NON_BILLABLE,
                        PageRequest.of(0, 10));
    }

    @Test
    void rejectsMissingEqualOrReversedRange() {
        assertThrows(BusinessException.class, () -> service.summarizeRevenue(null, NOW));
        assertThrows(BusinessException.class, () -> service.summarizeRevenue(NOW, null));
        assertThrows(BusinessException.class, () -> service.summarizeRevenue(NOW, NOW));
        assertThrows(BusinessException.class, () -> service.summary(NOW.plusSeconds(60), NOW));
    }

    @Test
    void monthlyAnalysisUsesCalendarDaysAndKeepsTiedPeaksAndZeroInvoices() {
        Instant from = Instant.parse("2028-01-31T17:00:00Z");
        Instant to = Instant.parse("2028-02-29T17:00:00Z");
        when(invoices.summarizeRevenue(from, to))
                .thenReturn(
                        List.of(
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.CASH, 1, new BigDecimal("120000")),
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.BANK_TRANSFER, 2, new BigDecimal("120000"))));
        when(invoices.summarizeDailyRevenue(
                        from,
                        to,
                        "Asia/Ho_Chi_Minh",
                        PaymentMethod.CASH.name(),
                        PaymentMethod.BANK_TRANSFER.name()))
                .thenReturn(
                        List.of(
                                new DailyRevenueResponse(
                                        "2028-02-01",
                                        1,
                                        new BigDecimal("120000"),
                                        new BigDecimal("120000"),
                                        BigDecimal.ZERO),
                                new DailyRevenueResponse(
                                        "2028-02-15",
                                        2,
                                        new BigDecimal("120000"),
                                        BigDecimal.ZERO,
                                        new BigDecimal("120000"))));
        PaymentSummaryResponse result =
                paymentService(
                                "Asia/Ho_Chi_Minh",
                                Clock.fixed(Instant.parse("2028-03-01T00:00:00Z"), ZoneOffset.UTC))
                        .summary(from, to);
        assertEquals(29, result.analysis().calendarDays());
        assertEquals(2, result.analysis().activeDays());
        assertEquals(new BigDecimal("80000"), result.analysis().averageInvoiceAmount());
        assertEquals(new BigDecimal("8276"), result.analysis().averageDailyRevenue());
        assertEquals(List.of("2028-02-01", "2028-02-15"), result.analysis().peakDates());
        assertEquals(new BigDecimal("120000"), result.analysis().peakRevenue());
        assertEquals(3, result.revenue().invoiceCount());
    }

    @Test
    void zeroRevenueInvoiceHasAnActiveDayButNoInventedPeak() {
        when(invoices.summarizeRevenue(NOW, NOW.plusSeconds(60)))
                .thenReturn(
                        List.of(
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.CASH, 1, BigDecimal.ZERO)));
        when(invoices.summarizeDailyRevenue(
                        NOW,
                        NOW.plusSeconds(60),
                        "Asia/Ho_Chi_Minh",
                        PaymentMethod.CASH.name(),
                        PaymentMethod.BANK_TRANSFER.name()))
                .thenReturn(
                        List.of(
                                new DailyRevenueResponse(
                                        "2026-10-22",
                                        1,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO)));
        PaymentSummaryResponse result = service.summary(NOW, NOW.plusSeconds(60));
        assertEquals(1, result.analysis().activeDays());
        assertEquals(List.of(), result.analysis().peakDates());
        assertEquals(BigDecimal.ZERO, result.analysis().averageInvoiceAmount());
    }

    @Test
    void exclusiveVietnamMidnightDoesNotAddAnotherCalendarDay() {
        PaymentSummaryResponse result =
                service.summary(
                        Instant.parse("2026-10-20T17:00:00Z"),
                        Instant.parse("2026-10-21T17:00:00Z"));
        assertEquals(1, result.analysis().calendarDays());
        assertEquals(0, result.analysis().activeDays());
        assertEquals(BigDecimal.ZERO, result.analysis().averageDailyRevenue());
    }

    @Test
    void partialDaysAreGroupedInTheConfiguredTimezone() {
        Instant from = Instant.parse("2026-10-21T16:59:59Z");
        Instant to = Instant.parse("2026-10-21T17:00:01Z");
        assertEquals(2, service.summary(from, to).analysis().calendarDays());
        assertEquals(1, paymentService("UTC").summary(from, to).analysis().calendarDays());
        verify(invoices)
                .summarizeDailyRevenue(
                        from,
                        to,
                        "UTC",
                        PaymentMethod.CASH.name(),
                        PaymentMethod.BANK_TRANSFER.name());
    }

    @Test
    void wideEmptyPeriodRemainsSparseInsteadOfAllocatingEveryDate() {
        PaymentSummaryResponse result =
                service.summary(
                        Instant.parse("2000-01-01T00:00:00Z"),
                        Instant.parse("2100-01-01T00:00:00Z"));
        assertEquals(0, result.dailyRevenue().size());
        assertEquals(9792, result.analysis().calendarDays());
        assertEquals(BigDecimal.ZERO, result.revenue().systemRevenue());
    }

    @Test
    void currentMonthAverageIncludesTodayButNotFutureDays() {
        Instant from = Instant.parse("2026-09-30T17:00:00Z");
        Instant to = Instant.parse("2026-10-31T17:00:00Z");
        when(invoices.summarizeRevenue(from, to))
                .thenReturn(
                        List.of(
                                new PaymentMethodSummaryResponse(
                                        PaymentMethod.CASH, 2, new BigDecimal("2200"))));
        PaymentSummaryResponse result = service.summary(from, to);
        assertEquals(22, result.analysis().calendarDays());
        assertEquals(new BigDecimal("100"), result.analysis().averageDailyRevenue());
        assertEquals(new BigDecimal("1100"), result.analysis().averageInvoiceAmount());
    }

    @Test
    void futurePeriodHasZeroCalendarDaysAndZeroAverage() {
        PaymentSummaryResponse result =
                service.summary(
                        Instant.parse("2026-11-01T00:00:00Z"),
                        Instant.parse("2026-12-01T00:00:00Z"));
        assertEquals(0, result.analysis().calendarDays());
        assertEquals(BigDecimal.ZERO, result.analysis().averageDailyRevenue());
        assertEquals(List.of(), result.dailyRevenue());
    }

    @Test
    void todayIsDeterminedByClockAndConfiguredTimezone() {
        Instant from = Instant.parse("2026-09-30T17:00:00Z");
        Instant to = Instant.parse("2026-10-31T17:00:00Z");
        Clock eveningUtc = Clock.fixed(Instant.parse("2026-10-22T18:00:00Z"), ZoneOffset.UTC);
        assertEquals(
                23,
                paymentService("Asia/Ho_Chi_Minh", eveningUtc)
                        .summary(from, to)
                        .analysis()
                        .calendarDays());
        assertEquals(
                23, paymentService("UTC", eveningUtc).summary(from, to).analysis().calendarDays());
        Instant sameLocalMonthStart = Instant.parse("2026-10-01T00:00:00Z");
        assertEquals(
                22,
                paymentService("UTC", eveningUtc)
                        .summary(sameLocalMonthStart, to)
                        .analysis()
                        .calendarDays());
    }

    @Test
    void futureOnlyHoursTodayHaveNoElapsedCalendarDay() {
        PaymentSummaryResponse result = service.summary(NOW.plusSeconds(60), NOW.plusSeconds(120));
        assertEquals(0, result.analysis().calendarDays());
        assertEquals(BigDecimal.ZERO, result.analysis().averageDailyRevenue());
    }
}
