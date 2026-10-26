package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.Reservation;
import vn.ueh.kltn.restaurant.enums.ReservationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    @Query("select r.table.id from Reservation r where r.id = :id")
    Optional<Long> findTableIdById(@Param("id") Long id);

    @Query(
            "select distinct r.table.id from Reservation r where r.table.id in :tableIds and"
                    + " r.status = vn.ueh.kltn.restaurant.enums.ReservationStatus.CONFIRMED and"
                    + " r.endTime > :now")
    List<Long> findLiveConfirmedTableIds(Collection<Long> tableIds, Instant now);

    @Query(
            "select coalesce(max(r.guestCount), 0) from Reservation r where r.table.id = :tableId"
                    + " and r.status = vn.ueh.kltn.restaurant.enums.ReservationStatus.CONFIRMED and"
                    + " r.endTime > :now")
    int maxLiveGuestCount(Long tableId, Instant now);

    @Query(
            "select (count(r) > 0) from Reservation r where r.table.id = :tableId and r.id <>"
                + " :excludedId and r.status in :statuses and :startTime < r.endTime and :endTime >"
                + " r.startTime")
    boolean existsOverlapExcluding(
            Long tableId,
            Instant startTime,
            Instant endTime,
            Collection<ReservationStatus> statuses,
            Long excludedId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(@Param("id") Long id);

    @Query(
            """
            select (count(r) > 0) from Reservation r where r.table.id = :tableId and r.status in :statuses
            and :startTime < r.endTime and :endTime > r.startTime
            """)
    boolean existsOverlap(
            @Param("tableId") Long tableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("statuses") Collection<ReservationStatus> statuses);

    @Query(
            """
            select r from Reservation r where r.table.id in :tableIds
            and r.status = vn.ueh.kltn.restaurant.enums.ReservationStatus.CONFIRMED
            and r.endTime > :now and r.startTime < :warningEnd order by r.startTime, r.id
            """)
    List<Reservation> findUpcoming(
            @Param("tableIds") Collection<Long> tableIds,
            @Param("now") Instant now,
            @Param("warningEnd") Instant warningEnd);

    @Override
    @EntityGraph(attributePaths = {"table", "createdBy"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);
}
