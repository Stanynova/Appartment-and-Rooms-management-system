package com.arms.entity;

/**
 * Room operational status (SRS Appendix A glossary, FR-1.3).
 */
public enum RoomStatus {
    /** Unoccupied and ready for allocation to a new rental. */
    AVAILABLE,
    /** Allocated to a tenant under an active rental. */
    OCCUPIED,
    /** Withdrawn from availability for repair or servicing. */
    UNDER_MAINTENANCE
}
