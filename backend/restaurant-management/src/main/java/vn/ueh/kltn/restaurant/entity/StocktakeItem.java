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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

@Entity
@Table(name = "stocktake_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StocktakeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stocktake_id", nullable = false)
    private Stocktake stocktake;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "lot_id", nullable = true)
    private IngredientLot lot;

    @Column(name = "system_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal systemQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal actualQuantity;

    @Column(name = "note", nullable = true, length = 500)
    private String note;

    public StocktakeItem(
            Ingredient ingredient,
            BigDecimal systemQuantity,
            BigDecimal actualQuantity,
            String note) {
        this.ingredient = ingredient;
        this.systemQuantity = systemQuantity;
        this.actualQuantity = actualQuantity;
        this.note = note;
    }
    void attachTo(Stocktake stocktake) {
        if (this.stocktake != null) throw new BusinessException(ErrorCode.INVALID_STATE, "Chi tiết đã thuộc phiếu kiểm kê");
        this.stocktake = stocktake;
    }
    public BigDecimal calculateDifference() { return actualQuantity.subtract(systemQuantity); }
}
