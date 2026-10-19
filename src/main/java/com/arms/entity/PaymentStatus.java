package com.arms.entity;

/**
 * Payment status (SRS FR-4.4, Appendix A glossary).
 */
public enum PaymentStatus {
    /** Full amount due has been received. */
    PAID,
    /** Payment expected but not yet received. */
    PENDING,
    /** Payment due date has passed without full settlement. */
    OVERDUE
}
