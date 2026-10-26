package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

@Entity
@Table(name = "revenue_reconciliation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevenueReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private Account cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "manager_id", nullable = true)
    private Account manager;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "system_revenue", nullable = false, precision = 14, scale = 0)
    private BigDecimal systemRevenue;

    @Column(name = "actual_amount", nullable = false, precision = 14, scale = 0)
    private BigDecimal actualAmount;

    @Column(name = "difference_reason", nullable = true, length = 500)
    private String differenceReason;

    @Column(name = "reconciled_at", nullable = false)
    private Instant reconciledAt;

    public RevenueReconciliation(Account cashier, Instant startTime, Instant endTime, BigDecimal systemRevenue,
                                 BigDecimal actualAmount, String differenceReason, Instant reconciledAt) {
        this.cashier = cashier; this.startTime = startTime; this.endTime = endTime;
        this.systemRevenue = systemRevenue;
        this.actualAmount = actualAmount; this.differenceReason = differenceReason;
        this.reconciledAt = reconciledAt;
    }
    public BigDecimal calculateDifference() { return actualAmount.subtract(systemRevenue); }
    public void confirm(Account manager) {
        if (this.manager != null) throw new BusinessException(ErrorCode.INVALID_STATE, "Đối soát đã được xác nhận");
        this.manager = Objects.requireNonNull(manager);
    }
}
