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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "ingredient_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "lot_number", nullable = true, length = 50)
    private String lotNumber;

    @Column(name = "expiry_date", nullable = true)
    private LocalDate expiryDate;

    public IngredientLot(Ingredient ingredient, String lotNumber, LocalDate expiryDate) {
        this.ingredient = ingredient;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    public boolean isExpiringSoon(LocalDate today, int days) {
        return expiryDate != null && !expiryDate.isBefore(today) && !expiryDate.isAfter(today.plusDays(days));
    }
}
