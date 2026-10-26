package vn.ueh.kltn.restaurant.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ueh.kltn.restaurant.dto.response.DailyRevenueResponse;
import vn.ueh.kltn.restaurant.dto.response.PaymentMethodSummaryResponse;
import vn.ueh.kltn.restaurant.entity.Invoice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    @Override
    @EntityGraph(attributePaths = {"session.table.area", "cashier"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    boolean existsBySessionId(Long sessionId);

    @Query(
            "select new vn.ueh.kltn.restaurant.dto.response.PaymentMethodSummaryResponse("
                    + "i.paymentMethod, count(i), sum(i.amount)) from Invoice i "
                    + "where i.paidAt >= :from and i.paidAt < :to group by i.paymentMethod")
    List<PaymentMethodSummaryResponse> summarizeRevenue(
            @Param("from") Instant from, @Param("to") Instant to);

    @Query(
            value =
                    """
                    select to_char(paid_at at time zone :timezone, 'YYYY-MM-DD') as date,
                           count(*) as invoice_count, sum(amount) as revenue,
                           sum(case when payment_method = :cashMethod then amount else 0 end) as cash_revenue,
                           sum(case when payment_method = :bankTransferMethod then amount else 0 end) as bank_transfer_revenue
                    from invoice where paid_at >= :from and paid_at < :to
                    group by 1 order by 1
                    """,
            nativeQuery = true)
    List<DailyRevenueResponse> summarizeDailyRevenue(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("timezone") String timezone,
            @Param("cashMethod") String cashMethod,
            @Param("bankTransferMethod") String bankTransferMethod);

    @Query(
            "select i from Invoice i join fetch i.session s join fetch s.table t join fetch"
                    + " i.cashier c where i.session.id = :sessionId")
    Optional<Invoice> findBySessionId(@Param("sessionId") Long sessionId);
}
