package vn.ueh.kltn.restaurant.mapper;

import vn.ueh.kltn.restaurant.dto.response.ReconciliationResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.RevenueReconciliation;

public final class ReconciliationMapper {
    private ReconciliationMapper() {}

    public static ReconciliationResponse toResponse(RevenueReconciliation reconciliation) {
        Account manager = reconciliation.getManager();
        return new ReconciliationResponse(
                reconciliation.getId(),
                reconciliation.getCashier().getId(),
                reconciliation.getCashier().getFullName(),
                manager == null ? null : manager.getId(),
                manager == null ? null : manager.getFullName(),
                reconciliation.getStartTime(),
                reconciliation.getEndTime(),
                reconciliation.getSystemRevenue(),
                reconciliation.getActualAmount(),
                reconciliation.calculateDifference(),
                reconciliation.getDifferenceReason(),
                reconciliation.getReconciledAt(),
                manager != null);
    }
}
