package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import vn.ueh.kltn.restaurant.entity.DiningArea;

import java.util.Optional;

public interface DiningAreaRepository extends JpaRepository<DiningArea, Long> {
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DiningArea a where a.id = :id")
    Optional<DiningArea> findLockedById(Long id);
}
