package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.FoodOrder;

import java.util.Collection;
import java.util.List;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    @Query(
            "select distinct o from FoodOrder o join fetch o.createdBy left join fetch o.items i"
                    + " left join fetch i.menuItem where o.session.id = :sessionId order by"
                    + " o.createdAt, o.id")
    List<FoodOrder> findDetailsBySessionId(@Param("sessionId") Long sessionId);

    @Query(
            "select distinct o from FoodOrder o join fetch o.createdBy left join fetch o.items i"
                    + " left join fetch i.menuItem where o.session.id in :sessionIds order by"
                    + " o.createdAt, o.id")
    List<FoodOrder> findDetailsBySessionIdIn(@Param("sessionIds") Collection<Long> sessionIds);
}
