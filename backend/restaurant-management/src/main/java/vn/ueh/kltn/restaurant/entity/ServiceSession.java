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
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ueh.kltn.restaurant.enums.SessionStatus;

@Entity
@Table(name = "service_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reservation_id", nullable = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Account createdBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at", nullable = true)
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SessionStatus status;

    public ServiceSession(
            DiningTable table,
            Reservation reservation,
            Account createdBy,
            Instant openedAt) {
        this.table = table;
        this.reservation = reservation;
        this.createdBy = createdBy;
        this.openedAt = openedAt;
        this.status = SessionStatus.ACTIVE;
    }
    public void close(Instant closedAt) {
        if (status == SessionStatus.CLOSED) return;
        this.status = SessionStatus.CLOSED;
        this.closedAt = Objects.requireNonNull(closedAt);
    }
}
