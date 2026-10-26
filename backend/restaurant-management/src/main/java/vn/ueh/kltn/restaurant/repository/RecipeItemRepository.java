package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.RecipeItem;
import vn.ueh.kltn.restaurant.entity.RecipeItemId;

import java.util.List;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, RecipeItemId> {
    @Query(
            "select r from RecipeItem r join fetch r.ingredient where r.menuItem.id = :menuItemId"
                + " order by r.ingredient.name")
    List<RecipeItem> findByMenuItemId(@Param("menuItemId") Long menuItemId);
}
