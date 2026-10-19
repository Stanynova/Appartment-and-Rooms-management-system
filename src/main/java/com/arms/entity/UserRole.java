package com.arms.entity;

/**
 * Roles used by role-based access control (SRS FR-7.3).
 * Permissions are granted to roles, never to individual users.
 */
public enum UserRole {
    /** Full access, including user administration and reporting. */
    ADMIN,
    /** Day-to-day operation: tenants, rentals, payments, availability search. */
    PROPERTY_STAFF
}
