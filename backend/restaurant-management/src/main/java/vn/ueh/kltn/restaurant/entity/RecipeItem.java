package vn.ueh.kltn.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeItem {
    @EmbeddedId private RecipeItemId id;

    @MapsId("menuItemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @MapsId("ingredientId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "quantity_per_serving", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityPerServing;

    public RecipeItem(MenuItem menuItem, Ingredient ingredient, BigDecimal quantityPerServing) {
        this.id = new RecipeItemId(menuItem.getId(), ingredient.getId());
        this.menuItem = menuItem;
        this.ingredient = ingredient;
        this.quantityPerServing = quantityPerServing;
    }

    public BigDecimal calculateConsumption(int portions) {
        return quantityPerServing.multiply(BigDecimal.valueOf(portions));
    }

    public void changeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Định lượng phải lớn hơn 0");
        }
        this.quantityPerServing = quantity;
    }
}
