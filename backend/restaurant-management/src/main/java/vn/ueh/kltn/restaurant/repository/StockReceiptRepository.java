package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.StockReceipt;

import java.util.List;
import java.util.Optional;

public interface StockReceiptRepository
        extends JpaRepository<StockReceipt, Long>, JpaSpecificationExecutor<StockReceipt> {

    @Query(
            "select r from StockReceipt r join fetch r.createdBy left join fetch r.items i"
                    + " left join fetch i.ingredient left join fetch i.lot where r.id = :id")
    Optional<StockReceipt> findDetailById(@Param("id") Long id);

    @Query(
            "select distinct r from StockReceipt r join fetch r.createdBy left join fetch r.items i"
                    + " left join fetch i.ingredient left join fetch i.lot where r.id in :ids")
    List<StockReceipt> findDetailsByIdIn(@Param("ids") List<Long> ids);
}
