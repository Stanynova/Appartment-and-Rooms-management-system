package com.arms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Apartment and Room Management System (ARMS).
 *
 * <p>Architecture follows SRS constraint C-01: a Spring Boot web application
 * organised into Controller / Service / Repository layers.</p>
 */
@SpringBootApplication
public class ArmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArmsApplication.class, args);
    }
}
