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
import vn.ueh.kltn.restaurant.enums.MenuItemStatus;
import vn.ueh.kltn.restaurant.exception.BusinessException;
import vn.ueh.kltn.restaurant.exception.ErrorCode;

@Entity
@Table(name = "menu_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 14, scale = 0)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MenuItemStatus status;

    public MenuItem(String name, BigDecimal price) { this.name = name; changePrice(price); status = MenuItemStatus.ON_SALE; }
    public void updateInfo(String name, BigDecimal price) { this.name = name; changePrice(price); }
    public void changePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "Giá món phải lớn hơn hoặc bằng 0");
        this.price = price;
    }
    public void activate() { status = MenuItemStatus.ON_SALE; }
    public void discontinue() { status = MenuItemStatus.DISCONTINUED; }
}
