package com.arms.repository;

import com.arms.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    /** FR-1.1: the apartment name must be unique. */
    boolean existsByNameIgnoreCase(String name);

    Optional<Apartment> findByNameIgnoreCase(String name);

    List<Apartment> findByActiveTrueOrderByNameAsc();

    /** FR-5.3: search apartments by name. */
    @Query("""
            select a from Apartment a
            where lower(a.name) like lower(concat('%', :q, '%'))
            order by a.name
            """)
    List<Apartment> searchByName(@Param("q") String query);
}
