package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.StockReceiptItem;

import java.util.Collection;
import java.util.List;

public interface StockReceiptItemRepository extends JpaRepository<StockReceiptItem, Long> {
    List<StockReceiptItem> findByReceiptId(Long receiptId);

    @Query(
            "select sri.lot.id, coalesce(sum(sri.quantity), 0) from StockReceiptItem sri where"
                    + " sri.lot.id in :lotIds group by sri.lot.id")
    List<Object[]> findInitialQuantitiesByLotIds(@Param("lotIds") Collection<Long> lotIds);
}
