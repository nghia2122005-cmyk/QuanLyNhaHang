package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.DiningTable;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    Optional<DiningTable> findByName(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("select t.area.id from DiningTable t where t.id = :id")
    Optional<Long> findAreaIdById(Long id);

    @Query("select t from DiningTable t join fetch t.area order by t.area.id, t.id")
    List<DiningTable> findAllWithArea();

    @Query("select t.id from DiningTable t where t.area.id = :areaId order by t.id")
    List<Long> findIdsByAreaId(Long areaId);

    @Query(
            "select t from DiningTable t join fetch t.area a where a.status ="
                    + " vn.ueh.kltn.restaurant.enums.AreaStatus.ACTIVE order by a.id, t.name")
    List<DiningTable> findFloorPlanTables();

    @Query(
            """
            select t from DiningTable t join fetch t.area a
            where a.status = vn.ueh.kltn.restaurant.enums.AreaStatus.ACTIVE
              and t.status <> vn.ueh.kltn.restaurant.enums.TableStatus.INACTIVE and t.capacity >= :guestCount
              and not exists (select r.id from Reservation r where r.table = t and r.status in :statuses and r.id <> coalesce(:excludedId, -1)
                              and :startTime < r.endTime and :endTime > r.startTime)
            order by t.capacity, a.id, t.name
            """)
    List<DiningTable> findAvailable(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("guestCount") int guestCount,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("excludedId") Long excludedId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from DiningTable entity where entity.id = :id")
    Optional<DiningTable> findLockedById(@Param("id") Long id);
}
