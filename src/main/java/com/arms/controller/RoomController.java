package com.arms.controller;

import com.arms.dto.RoomDtos.CreateRoomRequest;
import com.arms.dto.RoomDtos.MaintenanceRequest;
import com.arms.dto.RoomDtos.RoomResponse;
import com.arms.dto.RoomDtos.UpdateRentRequest;
import com.arms.dto.RoomDtos.UpdateRoomRequest;
import com.arms.entity.RoomStatus;
import com.arms.service.RoomService;
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
 * Room inventory endpoints (SRS FR-1.2 to FR-1.7, BR-7: only an
 * administrator may deactivate a room).
 *
 * <p>Room status is changed only by the dedicated endpoints here and by
 * rental operations, so that every transition belongs to the state model of
 * SRS Figure 4 (FR-1.3).</p>
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomResponse> list(@RequestParam(required = false) Long apartmentId,
                                   @RequestParam(required = false) RoomStatus status) {
        return roomService.list(apartmentId, status);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return roomService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateRoomRequest request) {
        return roomService.update(id, request);
    }

    /** FR-1.7: change the rent amount; existing rentals keep their snapshot. */
    @PutMapping("/{id}/rent")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse updateRent(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRentRequest request) {
        return roomService.updateRent(id, request);
    }

    /** FR-1.4 / FR-1.5: place under maintenance, warning before occupied rooms. */
    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse placeUnderMaintenance(@PathVariable Long id,
                                              @Valid @RequestBody MaintenanceRequest request) {
        return roomService.placeUnderMaintenance(id, request);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse releaseFromMaintenance(@PathVariable Long id) {
        return roomService.releaseFromMaintenance(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse deactivate(@PathVariable Long id) {
        return roomService.deactivate(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse activate(@PathVariable Long id) {
        return roomService.activate(id);
    }
}
