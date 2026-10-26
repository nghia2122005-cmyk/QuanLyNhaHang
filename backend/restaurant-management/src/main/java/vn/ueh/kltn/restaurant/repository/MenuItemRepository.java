package vn.ueh.kltn.restaurant.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import vn.ueh.kltn.restaurant.entity.MenuItem;

import java.util.Optional;

public interface MenuItemRepository
        extends JpaRepository<MenuItem, Long>, JpaSpecificationExecutor<MenuItem> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id = :id")
    Optional<MenuItem> findLockedById(Long id);
}
