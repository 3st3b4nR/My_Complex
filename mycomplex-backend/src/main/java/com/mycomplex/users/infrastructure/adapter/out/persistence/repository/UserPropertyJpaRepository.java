package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.entity.UserPropertyEntity;

public interface UserPropertyJpaRepository
        extends JpaRepository<UserPropertyEntity, UUID> {

    @Query("""
            SELECT new com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo(
                relationship.property.id,
                relationship.property.name,
                relationship.property.city,
                relationship.role
            )
            FROM UserPropertyEntity relationship
            WHERE relationship.user.auth0Subject = :auth0Subject
              AND relationship.user.status = 'ACTIVO'
              AND relationship.user.deletedAt IS NULL
              AND relationship.property.status = 'ACTIVA'
              AND relationship.property.deletedAt IS NULL
              AND relationship.status = 'ACTIVA'
              AND relationship.deletedAt IS NULL
            ORDER BY relationship.property.name
            """)
    List<AssignedPropertyDbo> findActiveByAuth0Subject(
            @Param("auth0Subject") String auth0Subject
    );
}