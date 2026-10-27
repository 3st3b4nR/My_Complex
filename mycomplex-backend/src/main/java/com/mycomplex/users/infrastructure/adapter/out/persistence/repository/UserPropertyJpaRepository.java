package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;
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

    @Query("""
            SELECT CASE WHEN COUNT(relationship) > 0 THEN true ELSE false END
            FROM UserPropertyEntity relationship
            WHERE relationship.property.id = :propertyId
              AND relationship.user.auth0Subject = :auth0Subject
              AND relationship.user.status = 'ACTIVO'
              AND relationship.user.deletedAt IS NULL
              AND relationship.property.status = 'ACTIVA'
              AND relationship.property.deletedAt IS NULL
              AND relationship.role = 'ADMIN_COPROPIEDAD'
              AND relationship.status = 'ACTIVA'
              AND relationship.deletedAt IS NULL
            """)
    boolean hasActiveAdministratorAccess(
            @Param("propertyId") UUID propertyId,
            @Param("auth0Subject") String auth0Subject
    );

    @Query("""
            SELECT new com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo(
                relationship.user.id,
                relationship.user.firstNames,
                relationship.user.lastNames,
                relationship.user.email,
                relationship.role,
                relationship.user.status,
                relationship.status
            )
            FROM UserPropertyEntity relationship
            WHERE relationship.property.id = :propertyId
              AND relationship.property.status = 'ACTIVA'
              AND relationship.property.deletedAt IS NULL
              AND relationship.user.deletedAt IS NULL
              AND relationship.deletedAt IS NULL
            ORDER BY relationship.user.lastNames,
                     relationship.user.firstNames,
                     relationship.user.email
            """)
    List<PropertyMemberDbo> findMembersByPropertyId(
            @Param("propertyId") UUID propertyId
    );
}
