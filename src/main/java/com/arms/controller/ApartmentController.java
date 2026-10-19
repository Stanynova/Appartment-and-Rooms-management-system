package com.arms.controller;

import com.arms.dto.ApartmentDtos.ApartmentResponse;
import com.arms.dto.ApartmentDtos.CreateApartmentRequest;
import com.arms.dto.ApartmentDtos.UpdateApartmentRequest;
import com.arms.service.ApartmentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Apartment inventory endpoints (SRS FR-1.1, FR-1.6 and BR-7:
 * only an administrator may deactivate an apartment).
 */
@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping
    public List<ApartmentResponse> list() {
        return apartmentService.list();
    }

    @GetMapping("/{id}")
    public ApartmentResponse get(@PathVariable Long id) {
        return apartmentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApartmentResponse create(@Valid @RequestBody CreateApartmentRequest request) {
        return apartmentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApartmentResponse update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateApartmentRequest request) {
        return apartmentService.update(id, request);
    }

    /** FR-1.6: deactivation, never a permanent delete. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApartmentResponse deactivate(@PathVariable Long id) {
        return apartmentService.deactivate(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApartmentResponse activate(@PathVariable Long id) {
        return apartmentService.activate(id);
    }
}
