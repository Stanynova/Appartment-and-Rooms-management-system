package com.arms.service;

import com.arms.dto.RoomDtos.CreateRoomRequest;
import com.arms.dto.RoomDtos.MaintenanceRequest;
import com.arms.dto.RoomDtos.RoomResponse;
import com.arms.dto.RoomDtos.UpdateRentRequest;
import com.arms.dto.RoomDtos.UpdateRoomRequest;
import com.arms.entity.Apartment;
import com.arms.entity.RentalStatus;
import com.arms.entity.Room;
import com.arms.entity.RoomStatus;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.RoomRepository;
import com.arms.repository.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Room inventory (SRS FR-1.2 to FR-1.7).
 *
 * <p>Room status is only ever changed through the methods of this service and
 * through rental operations, so that no transition outside the state model of
 * SRS Figure 4 can occur (FR-1.3).</p>
 */
@Service
public class RoomService {

    private final RoomRepository rooms;
    private final RentalRepository rentals;
    private final ApartmentService apartmentService;
    private final Messages messages;

    public RoomService(RoomRepository rooms,
                       RentalRepository rentals,
                       ApartmentService apartmentService,
                       Messages messages) {
        this.rooms = rooms;
        this.rentals = rentals;
        this.apartmentService = apartmentService;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(Long apartmentId, RoomStatus status) {
        List<Room> found;
        if (apartmentId != null && status != null) {
            found = rooms.findByApartmentIdAndStatusOrderByRoomNumberAsc(apartmentId, status);
        } else if (apartmentId != null) {
            found = rooms.findByApartmentIdOrderByRoomNumberAsc(apartmentId);
        } else if (status != null) {
            found = rooms.findByStatusOrderByRoomNumberAsc(status);
        } else {
            found = rooms.findAll();
        }
        return found.stream()
                .sorted(Comparator.comparing((Room r) -> r.getApartment().getName())
                        .thenComparing(Room::getRoomNumber))
                .map(DtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long id) {
        return DtoMapper.toResponse(require(id));
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        Apartment apartment = apartmentService.require(request.apartmentId());
        if (!apartment.isActive()) {
            throw new ConflictException(messages.get("room.apartmentNotFound"), "APARTMENT_NOT_FOUND");
        }
        ensureNumberUnique(apartment.getId(), request.roomNumber(), null);

        Room room = new Room();
        room.setApartment(apartment);
        room.setRoomNumber(request.roomNumber().trim());
        room.setRoomType(emptyToNull(request.roomType()));
        room.setRentAmount(request.rentAmount());
        room.setStatus(RoomStatus.AVAILABLE);   // FR-1.2: initial status
        room.setActive(true);
        rooms.save(room);
        return DtoMapper.toResponse(room);
    }

    @Transactional
    public RoomResponse update(Long id, UpdateRoomRequest request) {
        Room room = require(id);
        ensureNumberUnique(room.getApartment().getId(), request.roomNumber(), id);
        room.setRoomNumber(request.roomNumber().trim());
        room.setRoomType(emptyToNull(request.roomType()));
        return DtoMapper.toResponse(room);
    }

    /** FR-1.7: only rentals created after this change use the new amount. */
    @Transactional
    public RoomResponse updateRent(Long id, UpdateRentRequest request) {
        Room room = require(id);
        room.setRentAmount(request.rentAmount());
        return DtoMapper.toResponse(room);
    }

    /** FR-1.4: place a room under maintenance with a stated reason. */
    @Transactional
    public RoomResponse placeUnderMaintenance(Long id, MaintenanceRequest request) {
        Room room = require(id);
        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) {
            throw new ConflictException(
                    messages.get("room.alreadyUnderMaintenance"), "ROOM_ALREADY_UNDER_MAINTENANCE");
        }
        // FR-1.5: warn and require explicit confirmation when an active rental exists
        if (rentals.existsByRoomIdAndStatus(id, RentalStatus.ACTIVE) && !request.confirm()) {
            throw ConflictException.confirmRequired(
                    messages.get("room.activeRentalExists"), "ACTIVE_RENTAL_EXISTS");
        }
        room.setStatus(RoomStatus.UNDER_MAINTENANCE);
        room.setMaintenanceReason(request.reason().trim());
        return DtoMapper.toResponse(room);
    }

    /**
     * Withdraw a room from maintenance. If an active rental still covers the
     * room (BR-3) it returns to OCCUPIED, otherwise to AVAILABLE.
     */
    @Transactional
    public RoomResponse releaseFromMaintenance(Long id) {
        Room room = require(id);
        if (room.getStatus() != RoomStatus.UNDER_MAINTENANCE) {
            throw new ConflictException(
                    messages.get("room.notUnderMaintenance"), "ROOM_NOT_UNDER_MAINTENANCE");
        }
        boolean hasActiveRental = rentals.existsByRoomIdAndStatus(id, RentalStatus.ACTIVE);
        room.setStatus(hasActiveRental ? RoomStatus.OCCUPIED : RoomStatus.AVAILABLE);
        room.setMaintenanceReason(null);
        return DtoMapper.toResponse(room);
    }

    /** FR-1.6: deactivation, never a permanent delete. */
    @Transactional
    public RoomResponse deactivate(Long id) {
        Room room = require(id);
        room.setActive(false);
        return DtoMapper.toResponse(room);
    }

    /** Undo a deactivation. */
    @Transactional
    public RoomResponse activate(Long id) {
        Room room = require(id);
        room.setActive(true);
        return DtoMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public Room require(Long id) {
        return rooms.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("room.notFound"), "ROOM_NOT_FOUND"));
    }

    private void ensureNumberUnique(Long apartmentId, String roomNumber, Long selfId) {
        boolean taken = rooms.existsByApartmentIdAndRoomNumberIgnoreCaseAndIdNot(
                apartmentId, roomNumber.trim(), selfId == null ? -1L : selfId);
        if (taken) {
            throw new ConflictException(
                    messages.get("room.numberExists", roomNumber.trim()), "ROOM_NUMBER_EXISTS");
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
