package com.arms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An apartment: a property containing one or more rentable rooms (SRS FR-1.1).
 *
 * <p>Deactivation instead of deletion is used so that historical rentals
 * remain correct (SRS FR-1.6).</p>
 */
@Entity
@Table(name = "apartments")
public class Apartment extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apartment_id")
    private Long id;

    @Column(name = "apartment_name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 120)
    private String location;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
