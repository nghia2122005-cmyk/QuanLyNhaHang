package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.PaySessionRequest;
import vn.ueh.kltn.restaurant.dto.response.InvoiceResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionPaymentResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentPreviewResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentSummaryResponse;
import vn.ueh.kltn.restaurant.dto.response.RevenueSummaryResponse;
import vn.ueh.kltn.restaurant.enums.PaymentMethod;

import java.time.Instant;
import java.util.List;

public interface PaymentService {
    RevenueSummaryResponse summarizeRevenue(Instant from, Instant to);

    PaymentSummaryResponse summary(Instant from, Instant to);

    List<OpenSessionPaymentResponse> listOpenSessions();

    PaymentPreviewResponse previewSession(Long sessionId);

    InvoiceResponse pay(PaySessionRequest request);

    PageResponse<InvoiceResponse> listInvoices(
            Instant from, Instant to, PaymentMethod paymentMethod, int page, int size);
}
