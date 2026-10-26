package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.ueh.kltn.restaurant.enums.ReservationStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

import java.time.Instant;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Account createdBy;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "note", nullable = true, length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Reservation(
            DiningTable table,
            Account createdBy,
            String customerName,
            String customerPhone,
            int guestCount,
            Instant startTime,
            Instant endTime,
            String note,
            Instant createdAt) {
        this.table = table;
        this.createdBy = createdBy;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.guestCount = guestCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
        this.createdAt = createdAt;
        this.status = ReservationStatus.CONFIRMED;
    }

    public void updateInfo(
            DiningTable table,
            String customerName,
            String customerPhone,
            int guestCount,
            Instant startTime,
            Instant endTime,
            String note) {
        requireConfirmed();
        this.table = table;
        this.customerName = customerName.trim();
        this.customerPhone = customerPhone;
        this.guestCount = guestCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) return;
        requireConfirmed();
        status = ReservationStatus.CANCELLED;
    }

    public void confirmSeated() {
        requireConfirmed();
        status = ReservationStatus.SEATED;
    }

    private void requireConfirmed() {
        if (status != ReservationStatus.CONFIRMED)
            throw new BusinessException(
                    ErrorCode.INVALID_STATE, "Đặt bàn không còn ở trạng thái đã xác nhận");
    }
}
