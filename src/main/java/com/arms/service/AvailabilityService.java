package com.arms.service;

import com.arms.dto.ApartmentDtos.ApartmentResponse;
import com.arms.dto.RoomDtos.RoomResponse;
import com.arms.dto.SearchDtos.AvailabilityGroupResponse;
import com.arms.dto.SearchDtos.SearchResponse;
import com.arms.dto.TenantDtos.TenantResponse;
import com.arms.entity.Apartment;
import com.arms.entity.Room;
import com.arms.entity.RoomStatus;
import com.arms.exception.BadRequestException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.ApartmentRepository;
import com.arms.repository.RoomRepository;
import com.arms.repository.TenantRepository;
import com.arms.repository.RentalRepository;
import com.arms.entity.RentalStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Search and room availability (SRS FR-5.1 to FR-5.3).
 *
 * <p>An available room for a period is a room whose status is AVAILABLE and
 * which is not linked to an active rental for the dates in question (SRS
 * glossary).</p>
 */
@Service
public class AvailabilityService {

    private final ApartmentRepository apartments;
    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final RentalRepository rentals;
    private final Messages messages;

    public AvailabilityService(ApartmentRepository apartments,
                               RoomRepository rooms,
                               TenantRepository tenants,
                               RentalRepository rentals,
                               Messages messages) {
        this.apartments = apartments;
        this.rooms = rooms;
        this.tenants = tenants;
        this.rentals = rentals;
        this.messages = messages;
    }

    /**
     * FR-5.1: the status of every room grouped by apartment;
     * FR-5.2: optional status filter; optional period for availability.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityGroupResponse> availability(RoomStatus status,
                                                        Long apartmentId,
                                                        LocalDate from,
                                                        LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(messages.get("common.invalidPeriod"), "INVALID_PERIOD");
        }

        List<Apartment> apartmentList;
        if (apartmentId != null) {
            apartmentList = apartments.findById(apartmentId)
                    .map(List::of)
                    .orElseThrow(() -> new NotFoundException(
                            messages.get("apartment.notFound"), "APARTMENT_NOT_FOUND"));
        } else {
            apartmentList = apartments.findByActiveTrueOrderByNameAsc();
        }

        List<AvailabilityGroupResponse> groups = new ArrayList<>();
        for (Apartment apartment : apartmentList) {
            List<Room> candidates = rooms.findByApartmentIdOrderByRoomNumberAsc(apartment.getId());
            List<RoomResponse> selected = new ArrayList<>();
            for (Room room : candidates) {
                if (status != null && room.getStatus() != status) {
                    continue;
                }
                if (from != null && to != null) {
                    // SRS glossary "available room": a room whose status is
                    // AVAILABLE AND which is not linked to an active rental
                    // for the dates in question. Rooms occupied or under
                    // maintenance for the period therefore never qualify.
                    if (room.getStatus() != RoomStatus.AVAILABLE) {
                        continue;
                    }
                    boolean booked = !rentals
                            .findOverlapping(room.getId(), from, to, RentalStatus.ACTIVE)
                            .isEmpty();
                    if (booked) {
                        continue;
                    }
                }
                selected.add(DtoMapper.toResponse(room));
            }
            if (apartmentId != null || !selected.isEmpty() || status == null) {
                groups.add(new AvailabilityGroupResponse(
                        apartment.getId(), apartment.getName(), apartment.isActive(), selected));
            }
        }
        return groups;
    }

    /** FR-5.3: search apartments by name, rooms by number, tenants by name or phone. */
    @Transactional(readOnly = true)
    public SearchResponse search(String type, String query) {
        boolean all = type == null || type.isBlank() || "all".equalsIgnoreCase(type);
        boolean wantApartments = all || "apartment".equalsIgnoreCase(type);
        boolean wantRooms = all || "room".equalsIgnoreCase(type);
        boolean wantTenants = all || "tenant".equalsIgnoreCase(type);

        if (!all && !wantApartments && !wantRooms && !wantTenants) {
            throw new BadRequestException(messages.get("search.invalidType"), "INVALID_SEARCH_TYPE");
        }
        if (query == null || query.isBlank()) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }
        String term = query.trim();

        List<ApartmentResponse> foundApartments = wantApartments
                ? apartments.searchByName(term).stream().map(DtoMapper::toResponse).toList()
                : List.of();
        List<RoomResponse> foundRooms = wantRooms
                ? rooms.searchByNumber(term).stream().map(DtoMapper::toResponse).toList()
                : List.of();
        List<TenantResponse> foundTenants = wantTenants
                ? tenants.search(term).stream().map(DtoMapper::toResponse).toList()
                : List.of();

        return new SearchResponse(foundApartments, foundRooms, foundTenants);
    }
}
