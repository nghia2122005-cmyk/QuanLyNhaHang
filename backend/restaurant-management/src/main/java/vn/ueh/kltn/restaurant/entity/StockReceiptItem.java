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
@Table(name = "stock_receipt_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private StockReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "lot_id", nullable = true)
    private IngredientLot lot;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public StockReceiptItem(Ingredient ingredient, IngredientLot lot, BigDecimal quantity) {
        this.ingredient = ingredient;
        this.lot = lot;
        this.quantity = quantity;
    }

    void attachTo(StockReceipt receipt) {
        if (this.receipt != null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Chi tiết đã thuộc phiếu nhập");
        }
        this.receipt = receipt;
    }
}
