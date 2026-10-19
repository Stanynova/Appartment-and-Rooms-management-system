package com.arms.repository;

import com.arms.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /** FR-2.3 / FR-5.3: search tenants by full name or phone number. */
    @Query("""
            select t from Tenant t
            where lower(t.fullName) like lower(concat('%', :q, '%'))
               or t.phone like concat('%', :q, '%')
            order by t.fullName
            """)
    List<Tenant> search(@Param("q") String query);
}
