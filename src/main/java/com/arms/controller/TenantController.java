package com.arms.controller;

import com.arms.dto.RentalDtos.RentalResponse;
import com.arms.dto.TenantDtos.CreateTenantRequest;
import com.arms.dto.TenantDtos.TenantResponse;
import com.arms.dto.TenantDtos.UpdateTenantRequest;
import com.arms.service.RentalService;
import com.arms.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant management endpoints (SRS FR-2.1 to FR-2.5). Registration is a
 * day-to-day function of property staff (SRS 2.3), so create and update are
 * open to every authenticated role; deletion is restricted to administrators
 * and refused while rentals refer to the tenant (FR-2.5).
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final RentalService rentalService;

    public TenantController(TenantService tenantService, RentalService rentalService) {
        this.tenantService = tenantService;
        this.rentalService = rentalService;
    }

    /** FR-2.3 / FR-5.3: search by name or phone number. */
    @GetMapping
    public List<TenantResponse> search(@RequestParam(required = false) String q) {
        return tenantService.search(q);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    /** FR-6.3: active and past rentals of a tenant. */
    @GetMapping("/{id}/rentals")
    public List<RentalResponse> rentals(@PathVariable Long id) {
        tenantService.require(id);
        return rentalService.search(id, null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @PutMapping("/{id}")
    public TenantResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }
}
