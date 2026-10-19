package com.arms.repository;

import com.arms.entity.Payment;
import com.arms.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByRentalIdOrderByPaymentDateAsc(Long rentalId);

    /**
     * BR-5: the balance outstanding is the rent due less the sum of payments
     * recorded. Only PAID entries represent money actually received.
     *
     * <p>The aggregate is selected on its own (no coalesce literal) so that
     * Hibernate infers the NUMERIC column type and returns a BigDecimal;
     * every caller treats a {@code null} result as zero.</p>
     */
    @Query("""
            select sum(p.amount)
            from Payment p
            where p.rental.id = :rentalId and p.status = :status
            """)
    BigDecimal sumByRentalIdAndStatus(@Param("rentalId") Long rentalId,
                                      @Param("status") PaymentStatus status);

    /** FR-4.6: a payment may be reversed at most once. */
    boolean existsByReversesPaymentId(Long paymentId);

    /** FR-6.2: revenue for a period grouped by apartment. */
    @Query("""
            select a.id, a.name, sum(p.amount)
            from Payment p
                join p.rental r
                join r.room rm
                join rm.apartment a
            where p.paymentDate between :from and :to
              and p.status = :status
            group by a.id, a.name
            order by a.name
            """)
    List<Object[]> sumRevenueByApartment(@Param("from") LocalDate from,
                                         @Param("to") LocalDate to,
                                         @Param("status") PaymentStatus status);
}
