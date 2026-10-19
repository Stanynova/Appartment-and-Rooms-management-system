package com.arms.controller;

import com.arms.dto.RentalDtos.CreateRentalRequest;
import com.arms.dto.RentalDtos.RentalResponse;
import com.arms.dto.RentalDtos.UpdateRentalDatesRequest;
import com.arms.entity.RentalStatus;
import com.arms.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
 * Rental management endpoints (SRS FR-3.1 to FR-3.7).
 *
 * <p>Recording, amending, cancelling and completing rentals are day-to-day
 * functions of property staff (SRS 2.3), so they are open to every
 * authenticated role; the business rules (BR-1, BR-2, BR-3) are enforced in
 * the service layer inside a single transaction.</p>
 */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    /** FR-3.1: record a rental; the service verifies availability first. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse create(@Valid @RequestBody CreateRentalRequest request) {
        return rentalService.create(request);
    }

    @GetMapping
    public List<RentalResponse> search(@RequestParam(required = false) Long tenantId,
                                       @RequestParam(required = false) Long roomId,
                                       @RequestParam(required = false) RentalStatus status) {
        return rentalService.search(tenantId, roomId, status);
    }

    @GetMapping("/{id}")
    public RentalResponse get(@PathVariable Long id) {
        return rentalService.get(id);
    }

    /** FR-3.4: amend dates of a rental that has not started; re-verified. */
    @PutMapping("/{id}/dates")
    public RentalResponse updateDates(@PathVariable Long id,
                                      @Valid @RequestBody UpdateRentalDatesRequest request) {
        return rentalService.updateDates(id, request);
    }

    /** FR-3.5: cancel before the start date; the room is released (audited FR-7.6). */
    @PostMapping("/{id}/cancel")
    public RentalResponse cancel(@PathVariable Long id) {
        return rentalService.cancel(id);
    }

    /** FR-3.3: completion returns the room to availability. */
    @PostMapping("/{id}/complete")
    public RentalResponse complete(@PathVariable Long id) {
        return rentalService.complete(id);
    }
}
