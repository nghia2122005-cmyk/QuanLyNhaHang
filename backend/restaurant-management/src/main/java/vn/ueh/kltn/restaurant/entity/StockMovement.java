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

import vn.ueh.kltn.restaurant.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_movement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "lot_id", nullable = true)
    private IngredientLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "receipt_item_id", nullable = true)
    private StockReceiptItem receiptItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_item_id", nullable = true)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "stocktake_item_id", nullable = true)
    private StocktakeItem stocktakeItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private Account performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityChange;

    @Column(name = "reason", nullable = true, length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public StockMovement(
            Ingredient ingredient,
            IngredientLot lot,
            StockReceiptItem receiptItem,
            OrderItem orderItem,
            StocktakeItem stocktakeItem,
            Account performedBy,
            MovementType movementType,
            BigDecimal quantityChange,
            String reason,
            Instant occurredAt) {
        this.ingredient = ingredient;
        this.lot = lot;
        this.receiptItem = receiptItem;
        this.orderItem = orderItem;
        this.stocktakeItem = stocktakeItem;
        this.performedBy = performedBy;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public boolean isIncrease() {
        return quantityChange.signum() > 0;
    }

    public boolean isDecrease() {
        return quantityChange.signum() < 0;
    }
}
