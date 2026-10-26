package vn.ueh.kltn.restaurant.service;

import vn.ueh.kltn.restaurant.dto.request.CreateReconciliationRequest;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.ReconciliationResponse;
import vn.ueh.kltn.restaurant.dto.response.RevenueSummaryResponse;

import java.time.Instant;

public interface ReconciliationService {
    RevenueSummaryResponse preview(Instant from, Instant to);

    ReconciliationResponse create(CreateReconciliationRequest request);

    PageResponse<ReconciliationResponse> list(Boolean confirmed, int page, int size);

    ReconciliationResponse confirm(Long id);
}
