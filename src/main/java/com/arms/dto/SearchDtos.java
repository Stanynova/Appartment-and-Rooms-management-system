package com.arms.dto;

import java.util.List;

/** Search and availability endpoints (SRS FR-5.1 to FR-5.3). */
public final class SearchDtos {

    private SearchDtos() {
    }

    /** Rooms grouped by apartment (FR-5.1). */
    public record AvailabilityGroupResponse(
            Long apartmentId,
            String apartmentName,
            boolean active,
            List<RoomDtos.RoomResponse> rooms) {
    }

    /** FR-5.3: combined search across apartments, rooms and tenants. */
    public record SearchResponse(
            List<ApartmentDtos.ApartmentResponse> apartments,
            List<RoomDtos.RoomResponse> rooms,
            List<TenantDtos.TenantResponse> tenants) {
    }
}
