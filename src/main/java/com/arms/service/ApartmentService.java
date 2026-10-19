package com.arms.service;

import com.arms.dto.ApartmentDtos.ApartmentResponse;
import com.arms.dto.ApartmentDtos.CreateApartmentRequest;
import com.arms.dto.ApartmentDtos.UpdateApartmentRequest;
import com.arms.entity.Apartment;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.ApartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Apartment inventory (SRS FR-1.1, FR-1.6).
 *
 * <p>Deactivation is used instead of permanent deletion so that historical
 * reporting stays correct (FR-1.6).</p>
 */
@Service
public class ApartmentService {

    private final ApartmentRepository apartments;
    private final Messages messages;

    public ApartmentService(ApartmentRepository apartments, Messages messages) {
        this.apartments = apartments;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> list() {
        return apartments.findAll(Sort.by("name")).stream()
                .map(DtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApartmentResponse get(Long id) {
        return DtoMapper.toResponse(require(id));
    }

    @Transactional
    public ApartmentResponse create(CreateApartmentRequest request) {
        ensureNameUnique(request.name(), null);
        Apartment apartment = new Apartment();
        apartment.setName(request.name().trim());
        apartment.setLocation(emptyToNull(request.location()));
        apartment.setAddress(emptyToNull(request.address()));
        apartment.setDescription(emptyToNull(request.description()));
        apartment.setActive(true);
        apartments.save(apartment);
        return DtoMapper.toResponse(apartment);
    }

    @Transactional
    public ApartmentResponse update(Long id, UpdateApartmentRequest request) {
        Apartment apartment = require(id);
        ensureNameUnique(request.name(), id);
        apartment.setName(request.name().trim());
        apartment.setLocation(emptyToNull(request.location()));
        apartment.setAddress(emptyToNull(request.address()));
        apartment.setDescription(emptyToNull(request.description()));
        return DtoMapper.toResponse(apartment);
    }

    /** FR-1.6: deactivation, never a permanent delete. */
    @Transactional
    public ApartmentResponse deactivate(Long id) {
        Apartment apartment = require(id);
        apartment.setActive(false);
        return DtoMapper.toResponse(apartment);
    }

    /** Re-activation, used to undo a deactivation. */
    @Transactional
    public ApartmentResponse activate(Long id) {
        Apartment apartment = require(id);
        apartment.setActive(true);
        return DtoMapper.toResponse(apartment);
    }

    @Transactional(readOnly = true)
    public Apartment require(Long id) {
        return apartments.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("apartment.notFound"), "APARTMENT_NOT_FOUND"));
    }

    private void ensureNameUnique(String name, Long selfId) {
        boolean taken = apartments.findByNameIgnoreCase(name.trim())
                .map(existing -> !existing.getId().equals(selfId))
                .orElse(false);
        if (taken) {
            throw new ConflictException(messages.get("apartment.nameExists"), "APARTMENT_NAME_EXISTS");
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
