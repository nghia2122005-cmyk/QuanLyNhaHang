package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;

@Entity
@Table(name = "ingredient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock;

    @Column(name = "track_lot", nullable = false)
    private boolean trackLot;

    @Column(name = "track_expiry", nullable = false)
    private boolean trackExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IngredientStatus status;

    public Ingredient(
            String name,
            String unit,
            BigDecimal minStock,
            boolean trackLot,
            boolean trackExpiry) {
        updateInfo(name, unit, minStock, trackLot, trackExpiry);
        this.status = IngredientStatus.ACTIVE;
    }

    public void updateInfo(
            String name,
            String unit,
            BigDecimal minStock,
            boolean trackLot,
            boolean trackExpiry) {
        this.name = name;
        this.unit = unit;
        this.minStock = minStock;
        this.trackLot = trackLot;
        this.trackExpiry = trackExpiry;
    }

    public void activate() {
        this.status = IngredientStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = IngredientStatus.INACTIVE;
    }

    public boolean isBelowMinStock(BigDecimal currentStock) {
        return currentStock.compareTo(minStock) < 0;
    }
}
