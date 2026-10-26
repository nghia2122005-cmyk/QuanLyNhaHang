package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.Ingredient;
import vn.ueh.kltn.restaurant.enums.IngredientStatus;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository
        extends JpaRepository<Ingredient, Long>, JpaSpecificationExecutor<Ingredient> {
    List<Ingredient> findByStatusOrderByNameAsc(IngredientStatus status);

    List<Ingredient> findAllByOrderByNameAsc();

    Optional<Ingredient> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from Ingredient entity where entity.id = :id")
    Optional<Ingredient> findLockedById(@Param("id") Long id);
}
