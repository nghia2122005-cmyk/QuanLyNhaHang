package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.IngredientLot;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface IngredientLotRepository extends JpaRepository<IngredientLot, Long> {
    @EntityGraph(attributePaths = "ingredient")
    List<IngredientLot> findByIngredientIdOrderByExpiryDateAscIdAsc(Long ingredientId);

    @Query(
            "select l.ingredient.id, count(l), min(l.expiryDate) from IngredientLot l "
                    + "where l.ingredient.id in :ingredientIds group by l.ingredient.id")
    List<Object[]> summarizeByIngredientIds(@Param("ingredientIds") Collection<Long> ingredientIds);

    @Query(
            "select l from IngredientLot l join fetch l.ingredient i where l.expiryDate is not"
                    + " null and l.expiryDate <= :thresholdDate order by l.expiryDate asc")
    List<IngredientLot> findAttentionLots(@Param("thresholdDate") LocalDate thresholdDate);
}
