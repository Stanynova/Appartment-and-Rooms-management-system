package com.arms.controller;

import com.arms.dto.SearchDtos.AvailabilityGroupResponse;
import com.arms.dto.SearchDtos.SearchResponse;
import com.arms.entity.RoomStatus;
import com.arms.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Search and room availability endpoints (SRS FR-5.1 to FR-5.3).
 */
@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * FR-5.1 / FR-5.2: every room grouped by apartment, optionally filtered by
     * status. With a period, only rooms that are available and free of an
     * active rental for those dates are returned (SRS glossary).
     */
    @GetMapping("/availability")
    public List<AvailabilityGroupResponse> availability(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.availability(status, apartmentId, from, to);
    }

    /** FR-5.3: search apartments, rooms and tenants by name, number or phone. */
    @GetMapping("/search")
    public SearchResponse search(@RequestParam(required = false) String type,
                                 @RequestParam(required = false) String q) {
        return availabilityService.search(type, q);
    }
}
