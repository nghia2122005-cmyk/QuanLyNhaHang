package vn.ueh.kltn.restaurant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.dto.request.CreateReconciliationRequest;
import vn.ueh.kltn.restaurant.dto.request.PaySessionRequest;
import vn.ueh.kltn.restaurant.dto.response.InvoiceResponse;
import vn.ueh.kltn.restaurant.dto.response.OpenSessionPaymentResponse;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentPreviewResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentSummaryResponse;
import vn.ueh.kltn.restaurant.dto.response.ReconciliationResponse;
import vn.ueh.kltn.restaurant.dto.response.RevenueSummaryResponse;
import vn.ueh.kltn.restaurant.enums.PaymentMethod;
import vn.ueh.kltn.restaurant.service.PaymentService;
import vn.ueh.kltn.restaurant.service.ReconciliationService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ReconciliationService reconciliationService;

    @GetMapping("/payments/summary")
    @PreAuthorize("hasRole('OWNER')")
    public PaymentSummaryResponse summary(@RequestParam Instant from, @RequestParam Instant to) {
        return paymentService.summary(from, to);
    }

    @GetMapping("/reconciliations/preview")
    @PreAuthorize("hasRole('CASHIER')")
    public RevenueSummaryResponse previewRevenue(
            @RequestParam Instant from, @RequestParam Instant to) {
        return reconciliationService.preview(from, to);
    }

    @PostMapping("/reconciliations")
    @PreAuthorize("hasRole('CASHIER')")
    public ReconciliationResponse createReconciliation(
            @Valid @RequestBody CreateReconciliationRequest request) {
        return reconciliationService.create(request);
    }

    @GetMapping("/reconciliations")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
    public PageResponse<ReconciliationResponse> reconciliations(
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reconciliationService.list(confirmed, page, size);
    }

    @PatchMapping("/reconciliations/{id}/confirm")
    @PreAuthorize("hasRole('MANAGER')")
    public ReconciliationResponse confirmReconciliation(@PathVariable Long id) {
        return reconciliationService.confirm(id);
    }

    @GetMapping("/payments/open-sessions")
    @PreAuthorize("hasRole('CASHIER')")
    public List<OpenSessionPaymentResponse> openSessions() {
        return paymentService.listOpenSessions();
    }

    @GetMapping("/payments/sessions/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public PaymentPreviewResponse previewSession(@PathVariable Long id) {
        return paymentService.previewSession(id);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasRole('CASHIER')")
    public InvoiceResponse pay(@Valid @RequestBody PaySessionRequest request) {
        return paymentService.pay(request);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','OWNER')")
    public PageResponse<InvoiceResponse> invoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return paymentService.listInvoices(from, to, paymentMethod, page, size);
    }
}
