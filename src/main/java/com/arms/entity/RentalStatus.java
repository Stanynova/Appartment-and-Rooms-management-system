package com.arms.entity;

/**
 * Rental lifecycle status (SRS Appendix A glossary, FR-3.1).
 */
public enum RentalStatus {
    /** Rental period is current; the room is occupied. */
    ACTIVE,
    /** Rental period has ended and the tenant has vacated. */
    COMPLETED,
    /** Withdrawn before the rental start date. */
    CANCELLED
}
