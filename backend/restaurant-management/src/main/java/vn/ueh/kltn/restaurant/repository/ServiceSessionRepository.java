package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.entity.ServiceSession;
import vn.ueh.kltn.restaurant.enums.SessionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    boolean existsByReservationId(Long reservationId);

    boolean existsByTableIdAndStatus(Long tableId, SessionStatus status);

    List<ServiceSession> findByTableIdInAndStatus(Collection<Long> tableIds, SessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from ServiceSession entity where entity.id = :id")
    Optional<ServiceSession> findLockedById(@Param("id") Long id);

    @Query(
            "select s from ServiceSession s join fetch s.table t join fetch t.area"
                    + " left join fetch s.reservation where s.id = :id")
    Optional<ServiceSession> findDetailById(Long id);

    @Query(
            "select distinct s from ServiceSession s join fetch s.table t join fetch t.area left"
                    + " join fetch s.reservation where s.status = :status order by s.openedAt desc")
    List<ServiceSession> findByStatusWithTableAndArea(@Param("status") SessionStatus status);
}
