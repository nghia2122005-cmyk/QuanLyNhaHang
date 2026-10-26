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

import vn.ueh.kltn.restaurant.enums.TableStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

@Entity
@Table(name = "dining_table")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private DiningArea area;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TableStatus status;

    public DiningTable(DiningArea area, String name, int capacity) {
        this.code = area.getCode() + "-" + name;
        this.area = area;
        this.name = name;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

    public DiningTable(DiningArea area, String name, String code, int capacity) {
        this(area, name, capacity);
        this.code = code;
    }

    public boolean hasEnoughCapacity(int guestCount) {
        return guestCount > 0 && capacity >= guestCount;
    }

    public void updateInfo(DiningArea area, String name, String code, int capacity) {
        if (capacity <= 0)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Sức chứa phải lớn hơn 0");
        this.area = area;
        this.name = name;
        this.code = code;
        this.capacity = capacity;
    }

    public void deactivate() {
        if (status == TableStatus.OCCUPIED)
            throw new BusinessException(ErrorCode.TABLE_NOT_AVAILABLE);
        status = TableStatus.INACTIVE;
    }

    public void activate() {
        if (status == TableStatus.INACTIVE) status = TableStatus.AVAILABLE;
    }

    public void occupy() {
        if (status != TableStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Bàn không ở trạng thái trống");
        }
        this.status = TableStatus.OCCUPIED;
    }

    public void release() {
        if (status == TableStatus.AVAILABLE || status == TableStatus.INACTIVE) {
            return;
        }
        if (status != TableStatus.OCCUPIED) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Bàn không đang phục vụ");
        }
        this.status = TableStatus.AVAILABLE;
    }
}
