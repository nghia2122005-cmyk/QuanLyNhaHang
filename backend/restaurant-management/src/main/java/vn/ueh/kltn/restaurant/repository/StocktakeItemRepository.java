package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ueh.kltn.restaurant.entity.StocktakeItem;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {
}
