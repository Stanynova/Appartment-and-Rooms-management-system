package com.arms.repository;

import com.arms.entity.Rental;
import com.arms.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RentalRepository
        extends JpaRepository<Rental, Long>, JpaSpecificationExecutor<Rental> {

    /**
     * FR-3.2 / BR-1: rentals of a room that overlap a requested period.
     * Two periods overlap when each starts before the other ends.
     */
    @Query("""
            select r from Rental r
            where r.room.id = :roomId
              and r.status = :status
              and r.startDate <= :to
              and r.endDate >= :from
            """)
    List<Rental> findOverlapping(@Param("roomId") Long roomId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 @Param("status") RentalStatus status);

    /** FR-3.3 / BR-3: does the room have an active rental? */
    boolean existsByRoomIdAndStatus(Long roomId, RentalStatus status);

    /** FR-2.5: a tenant to which any rental refers may not be deleted. */
    long countByTenantId(Long tenantId);

    long countByStatus(RentalStatus status);

    /** FR-6.1: rentals touching a reporting period (cancelled rentals excluded). */
    @Query("""
            select r from Rental r
            where r.status in :statuses
              and r.startDate <= :to
              and r.endDate >= :from
            """)
    List<Rental> findOverlappingPeriod(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("statuses") Collection<RentalStatus> statuses);
}
