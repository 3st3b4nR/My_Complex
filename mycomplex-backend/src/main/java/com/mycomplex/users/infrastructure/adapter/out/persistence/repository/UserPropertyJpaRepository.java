package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberRow;
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

    @Query(
            value = """
                    SELECT
                        membresia.id AS "membershipId",
                        usuario.id AS "userId",
                        usuario.nombres AS "firstNames",
                        usuario.apellidos AS "lastNames",
                        usuario.correo AS "email",
                        membresia.rol AS "role",
                        usuario.estado AS "userStatus",
                        membresia.estado AS "membershipStatus",
                        membresia.version AS "membershipVersion",
                        vinculacion.id AS "assignmentId",
                        unidad.id AS "unitId",
                        estructura.nombre AS "structureName",
                        estructura.tipo AS "structureType",
                        unidad.numero AS "unitNumber",
                        unidad.tipo AS "unitType",
                        vinculacion.tipo_vinculacion AS "relationshipType",
                        vinculacion.es_principal AS "primaryUnit",
                        vinculacion.estado AS "residentialStatus"
                    FROM usuarios_copropiedades membresia
                    JOIN usuarios usuario
                      ON usuario.id = membresia.usuario_id
                    JOIN copropiedades copropiedad
                      ON copropiedad.id = membresia.copropiedad_id
                    LEFT JOIN usuarios_copropiedades_unidades vinculacion
                      ON vinculacion.usuario_copropiedad_id = membresia.id
                     AND vinculacion.copropiedad_id = membresia.copropiedad_id
                     AND vinculacion.deleted_at IS NULL
                     AND vinculacion.estado = 'ACTIVA'
                     AND vinculacion.fecha_inicio <= CURRENT_DATE
                     AND (
                         vinculacion.fecha_fin IS NULL
                         OR vinculacion.fecha_fin >= CURRENT_DATE
                     )
                    LEFT JOIN unidades_residenciales unidad
                      ON unidad.id = vinculacion.unidad_residencial_id
                     AND unidad.copropiedad_id = membresia.copropiedad_id
                     AND unidad.estado = 'ACTIVA'
                     AND unidad.deleted_at IS NULL
                    LEFT JOIN estructuras_copropiedad estructura
                      ON estructura.id = unidad.estructura_id
                     AND estructura.copropiedad_id = unidad.copropiedad_id
                     AND estructura.estado = 'ACTIVA'
                     AND estructura.deleted_at IS NULL
                    WHERE membresia.copropiedad_id = :propertyId
                      AND (
                          CAST(:membershipId AS UUID) IS NULL
                          OR membresia.id = CAST(:membershipId AS UUID)
                      )
                      AND copropiedad.estado = 'ACTIVA'
                      AND copropiedad.deleted_at IS NULL
                      AND usuario.deleted_at IS NULL
                      AND membresia.deleted_at IS NULL
                    ORDER BY usuario.apellidos,
                             usuario.nombres,
                             usuario.correo,
                             vinculacion.es_principal DESC,
                             estructura.nombre,
                             unidad.numero
                    """,
            nativeQuery = true
    )
    List<PropertyMemberRow> findMemberRowsByPropertyId(
            @Param("propertyId") UUID propertyId,
            @Param("membershipId") UUID membershipId
    );
}
