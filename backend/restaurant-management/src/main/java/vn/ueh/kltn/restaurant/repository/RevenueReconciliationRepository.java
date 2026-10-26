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

import vn.ueh.kltn.restaurant.entity.RevenueReconciliation;

import java.util.Optional;

public interface RevenueReconciliationRepository
        extends JpaRepository<RevenueReconciliation, Long>,
                JpaSpecificationExecutor<RevenueReconciliation> {
    @Override
    @EntityGraph(attributePaths = {"cashier", "manager"})
    Page<RevenueReconciliation> findAll(
            Specification<RevenueReconciliation> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RevenueReconciliation r where r.id = :id")
    Optional<RevenueReconciliation> findLockedById(@Param("id") Long id);
}
