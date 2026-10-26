package vn.ueh.kltn.restaurant.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.kltn.restaurant.dto.request.CreateReconciliationRequest;
import vn.ueh.kltn.restaurant.dto.response.PageResponse;
import vn.ueh.kltn.restaurant.dto.response.ReconciliationResponse;
import vn.ueh.kltn.restaurant.dto.response.RevenueSummaryResponse;
import vn.ueh.kltn.restaurant.entity.Account;
import vn.ueh.kltn.restaurant.entity.RevenueReconciliation;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;
import vn.ueh.kltn.restaurant.mapper.ReconciliationMapper;
import vn.ueh.kltn.restaurant.repository.AccountRepository;
import vn.ueh.kltn.restaurant.repository.RevenueReconciliationRepository;
import vn.ueh.kltn.restaurant.security.CurrentUser;
import vn.ueh.kltn.restaurant.service.PaymentService;
import vn.ueh.kltn.restaurant.service.ReconciliationService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationServiceImpl implements ReconciliationService {
    private final RevenueReconciliationRepository reconciliations;
    private final PaymentService payments;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Override
    public RevenueSummaryResponse preview(Instant from, Instant to) {
        requireEndedShift(to, clock.instant());
        return payments.summarizeRevenue(from, to);
    }

    @Override
    @Transactional
    public ReconciliationResponse create(CreateReconciliationRequest request) {
        Instant now = clock.instant();
        requireEndedShift(request.endTime(), now);
        RevenueSummaryResponse revenue =
                payments.summarizeRevenue(request.startTime(), request.endTime());
        if (revenue.systemRevenue().compareTo(new BigDecimal("99999999999999")) > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Doanh thu hệ thống vượt giới hạn lưu trữ, vui lòng chia nhỏ khoảng thời gian");
        }
        RevenueReconciliation reconciliation =
                new RevenueReconciliation(
                        actor(),
                        request.startTime(),
                        request.endTime(),
                        revenue.systemRevenue(),
                        request.actualAmount(),
                        request.differenceReason(),
                        now);
        return ReconciliationMapper.toResponse(reconciliations.save(reconciliation));
    }

    @Override
    public PageResponse<ReconciliationResponse> list(Boolean confirmed, int page, int size) {
        Page<RevenueReconciliation> result =
                reconciliations.findAll(
                        (root, query, cb) ->
                                confirmed == null
                                        ? cb.conjunction()
                                        : confirmed
                                                ? cb.isNotNull(root.get("manager"))
                                                : cb.isNull(root.get("manager")),
                        PageRequest.of(
                                page, size, Sort.by(Sort.Direction.DESC, "reconciledAt", "id")));
        return PageResponse.from(result.map(ReconciliationMapper::toResponse));
    }

    @Override
    @Transactional
    public ReconciliationResponse confirm(Long id) {
        RevenueReconciliation reconciliation =
                reconciliations
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "Không tìm thấy đối soát"));
        reconciliation.confirm(actor());
        return ReconciliationMapper.toResponse(reconciliation);
    }

    private Account actor() {
        return accounts.findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private void requireEndedShift(Instant endTime, Instant now) {
        if (endTime != null && endTime.isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "Thời điểm kết thúc ca không được ở tương lai");
        }
    }
}
