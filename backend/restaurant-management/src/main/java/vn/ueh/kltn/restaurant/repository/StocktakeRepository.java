package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.Stocktake;

import java.util.List;
import java.util.Optional;

public interface StocktakeRepository
        extends JpaRepository<Stocktake, Long>, JpaSpecificationExecutor<Stocktake> {

    @Query(
            "select s from Stocktake s join fetch s.createdBy left join fetch s.items i"
                    + " left join fetch i.ingredient left join fetch i.lot where s.id = :id")
    Optional<Stocktake> findDetailById(@Param("id") Long id);

    @Query(
            "select distinct s from Stocktake s join fetch s.createdBy left join fetch s.items i"
                    + " left join fetch i.ingredient left join fetch i.lot where s.id in :ids")
    List<Stocktake> findDetailsByIdIn(@Param("ids") List<Long> ids);
}
