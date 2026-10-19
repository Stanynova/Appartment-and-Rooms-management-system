package com.arms.service;

import com.arms.dto.RentalDtos.CreateRentalRequest;
import com.arms.dto.RentalDtos.RentalResponse;
import com.arms.dto.RentalDtos.UpdateRentalDatesRequest;
import com.arms.entity.AppUser;
import com.arms.entity.AuditEventType;
import com.arms.entity.PaymentStatus;
import com.arms.entity.Rental;
import com.arms.entity.RentalStatus;
import com.arms.entity.Room;
import com.arms.entity.RoomStatus;
import com.arms.entity.Tenant;
import com.arms.exception.BadRequestException;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.PaymentRepository;
import com.arms.repository.RentalRepository;
import com.arms.repository.RoomRepository;
import com.arms.repository.TenantRepository;
import com.arms.repository.UserRepository;
import com.arms.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rental management (SRS FR-3.1 to FR-3.7).
 *
 * <p>Every mutation is transactional so that the rental record and the room
 * status change together as a single consistent update (FR-3.3, NFR-SF-2),
 * and double booking of a room is prevented before anything is written
 * (FR-3.2, BR-1).</p>
 */
@Service
public class RentalService {

    private final RentalRepository rentals;
    private final TenantRepository tenants;
    private final RoomRepository rooms;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final Messages messages;

    public RentalService(RentalRepository rentals,
                         TenantRepository tenants,
                         RoomRepository rooms,
                         PaymentRepository payments,
                         UserRepository users,
                         CurrentUser currentUser,
                         AuditService audit,
                         Messages messages) {
        this.rentals = rentals;
        this.tenants = tenants;
        this.rooms = rooms;
        this.payments = payments;
        this.users = users;
        this.currentUser = currentUser;
        this.audit = audit;
        this.messages = messages;
    }

    /** FR-3.1: record a rental for a tenant and an available room. */
    @Transactional
    public RentalResponse create(CreateRentalRequest request) {
        validatePeriod(request.startDate(), request.endDate());

        Tenant tenant = tenants.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException(
                        messages.get("rental.tenantNotFound"), "TENANT_NOT_FOUND"));
        Room room = rooms.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException(
                        messages.get("rental.roomNotFound"), "ROOM_NOT_FOUND"));

        if (!room.isActive()) {
            throw new ConflictException(messages.get("rental.roomDeactivated"), "ROOM_DEACTIVATED");
        }
        // FR-3.7 / BR-2: no allocation while the room is under maintenance
        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) {
            throw new ConflictException(
                    messages.get("rental.underMaintenance"), "ROOM_UNDER_MAINTENANCE");
        }
        // FR-3.2 / BR-1: no two active rentals with overlapping dates
        assertNoOverlap(room.getId(), request.startDate(), request.endDate(), null);

        AppUser creator = users.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException(
                        messages.get("user.notFound"), "USER_NOT_FOUND"));

        Rental rental = new Rental();
        rental.setTenant(tenant);
        rental.setRoom(room);
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        rental.setStatus(RentalStatus.ACTIVE);
        // FR-1.7: snapshot so later rent changes do not affect this rental
        rental.setRentAmount(room.getRentAmount());
        rental.setCreatedBy(creator);                       // FR-3.6
        rentals.save(rental);

        // FR-3.3: an active rental occupies the room, atomically with the insert
        room.setStatus(RoomStatus.OCCUPIED);

        return DtoMapper.toResponse(rental, BigDecimal.ZERO);
    }

    /** FR-5.x / FR-6.3: list rentals, optionally filtered. */
    @Transactional(readOnly = true)
    public List<RentalResponse> search(Long tenantId, Long roomId, RentalStatus status) {
        Specification<Rental> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));
            }
            if (roomId != null) {
                predicates.add(cb.equal(root.get("room").get("id"), roomId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return rentals.findAll(spec, Sort.by(Sort.Direction.DESC, "startDate")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalResponse get(Long id) {
        return toResponse(require(id));
    }

    /** FR-3.4: amend dates of a rental that has not yet started; re-verify availability. */
    @Transactional
    public RentalResponse updateDates(Long id, UpdateRentalDatesRequest request) {
        Rental rental = require(id);
        ensureActive(rental);
        if (!rental.getStartDate().isAfter(LocalDate.now())) {
            throw new ConflictException(messages.get("rental.alreadyStarted"), "RENTAL_ALREADY_STARTED");
        }
        validatePeriod(request.startDate(), request.endDate());
        assertNoOverlap(rental.getRoom().getId(), request.startDate(), request.endDate(), id);
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        return toResponse(rental);
    }

    /** FR-3.5: cancel before the start date and release the room. */
    @Transactional
    public RentalResponse cancel(Long id) {
        Rental rental = require(id);
        ensureActive(rental);
        if (!rental.getStartDate().isAfter(LocalDate.now())) {
            throw new ConflictException(messages.get("rental.alreadyStarted"), "RENTAL_ALREADY_STARTED");
        }

        rental.setStatus(RentalStatus.CANCELLED);
        releaseRoomIfFree(rental.getRoom());

        // FR-7.6: rental cancellations are recorded as security-relevant events
        audit.record(AuditEventType.RENTAL_CANCELLED,
                currentUser.username(), currentUser.id(),
                "Rental #" + rental.getId() + " on room "
                        + rental.getRoom().getRoomNumber() + " cancelled");
        return toResponse(rental);
    }

    /** FR-3.3: completing a rental returns the room to availability. */
    @Transactional
    public RentalResponse complete(Long id) {
        Rental rental = require(id);
        ensureActive(rental);
        if (rental.getStartDate().isAfter(LocalDate.now())) {
            throw new ConflictException(messages.get("rental.notStarted"), "RENTAL_NOT_STARTED");
        }

        rental.setStatus(RentalStatus.COMPLETED);
        releaseRoomIfFree(rental.getRoom());
        return toResponse(rental);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Rental require(Long id) {
        return rentals.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("rental.notFound"), "RENTAL_NOT_FOUND"));
    }

    private RentalResponse toResponse(Rental rental) {
        BigDecimal paid = payments.sumByRentalIdAndStatus(rental.getId(), PaymentStatus.PAID);
        return DtoMapper.toResponse(rental, paid);
    }

    private void ensureActive(Rental rental) {
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new ConflictException(messages.get("rental.notActive"), "RENTAL_NOT_ACTIVE");
        }
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException(messages.get("rental.invalidPeriod"), "INVALID_PERIOD");
        }
    }

    /** FR-3.2 / BR-1: the requested period must not clash with an active rental. */
    private void assertNoOverlap(Long roomId, LocalDate from, LocalDate to, Long excludeRentalId) {
        boolean clashing = rentals.findOverlapping(roomId, from, to, RentalStatus.ACTIVE).stream()
                .anyMatch(existing -> excludeRentalId == null
                        || !existing.getId().equals(excludeRentalId));
        if (clashing) {
            throw new ConflictException(messages.get("rental.overlapping"), "DOUBLE_BOOKING");
        }
    }

    /** FR-3.3: the room becomes available only when no active rental remains. */
    private void releaseRoomIfFree(Room room) {
        if (!rentals.existsByRoomIdAndStatus(room.getId(), RentalStatus.ACTIVE)) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
    }
}
