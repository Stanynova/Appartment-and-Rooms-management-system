package com.arms.service;

import com.arms.dto.TenantDtos.CreateTenantRequest;
import com.arms.dto.TenantDtos.TenantResponse;
import com.arms.dto.TenantDtos.UpdateTenantRequest;
import com.arms.entity.Tenant;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.RentalRepository;
import com.arms.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tenant registration and profiles (SRS FR-2.1 to FR-2.5).
 */
@Service
public class TenantService {

    private final TenantRepository tenants;
    private final RentalRepository rentals;
    private final Messages messages;

    public TenantService(TenantRepository tenants, RentalRepository rentals, Messages messages) {
        this.tenants = tenants;
        this.rentals = rentals;
        this.messages = messages;
    }

    /** FR-2.3: search by name or phone number; a blank term lists everyone. */
    @Transactional(readOnly = true)
    public List<TenantResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return tenants.findAll(org.springframework.data.domain.Sort.by("fullName")).stream()
                    .map(DtoMapper::toResponse)
                    .toList();
        }
        return tenants.search(query.trim()).stream()
                .map(DtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return DtoMapper.toResponse(require(id));
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        Tenant tenant = new Tenant();
        apply(tenant, request.fullName(), request.phone(), request.email(), request.address());
        tenants.save(tenant);
        return DtoMapper.toResponse(tenant);
    }

    @Transactional
    public TenantResponse update(Long id, UpdateTenantRequest request) {
        Tenant tenant = require(id);
        apply(tenant, request.fullName(), request.phone(), request.email(), request.address());
        return DtoMapper.toResponse(tenant);
    }

    /**
     * FR-2.5: deletion is refused while any rental refers to the tenant;
     * FR-2.4: otherwise the profile has no active rental and may be removed.
     */
    @Transactional
    public void delete(Long id) {
        Tenant tenant = require(id);
        if (rentals.countByTenantId(id) > 0) {
            throw new ConflictException(messages.get("tenant.hasRentals"), "TENANT_HAS_RENTALS");
        }
        tenants.delete(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant require(Long id) {
        return tenants.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("tenant.notFound"), "TENANT_NOT_FOUND"));
    }

    private static void apply(Tenant tenant, String fullName, String phone, String email, String address) {
        tenant.setFullName(fullName.trim());
        tenant.setPhone(phone.trim());
        tenant.setEmail(blankToNull(email));
        tenant.setAddress(blankToNull(address));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
