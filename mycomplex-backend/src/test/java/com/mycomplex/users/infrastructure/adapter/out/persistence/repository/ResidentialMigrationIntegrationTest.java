package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class ResidentialMigrationIntegrationTest {

    private static final UUID LOS_CEREZOS_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsExpectedLosCerezosResidentialData() {
        Integer structureCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM estructuras_copropiedad
                WHERE copropiedad_id = ?
                """,
                Integer.class,
                LOS_CEREZOS_ID
        );
        Integer unitCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM unidades_residenciales
                WHERE copropiedad_id = ?
                """,
                Integer.class,
                LOS_CEREZOS_ID
        );
        Integer relationshipCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades_unidades
                WHERE copropiedad_id = ?
                """,
                Integer.class,
                LOS_CEREZOS_ID
        );

        Integer memberCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades
                WHERE copropiedad_id = ?
                """,
                Integer.class,
                LOS_CEREZOS_ID
        );
        Integer johnUnitCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades_unidades relacion
                JOIN usuarios_copropiedades membresia
                  ON membresia.id = relacion.usuario_copropiedad_id
                WHERE membresia.usuario_id =
                    '20000000-0000-0000-0000-000000000001'
                """,
                Integer.class
        );
        Integer carolinaUnitCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades_unidades relacion
                WHERE relacion.usuario_copropiedad_id =
                    '70000000-0000-0000-0000-000000000003'
                  AND relacion.estado = 'ACTIVA'
                  AND relacion.deleted_at IS NULL
                """,
                Integer.class
        );

        assertEquals(2, structureCount);
        assertEquals(4, unitCount);
        assertEquals(5, relationshipCount);
        assertEquals(5, memberCount);
        assertEquals(0, johnUnitCount);
        assertEquals(2, carolinaUnitCount);
    }

    @Test
    void rejectsResidentialRelationshipAcrossProperties() {
        UUID otherPropertyId = UUID.randomUUID();
        UUID otherStructureId = UUID.randomUUID();
        UUID otherUnitId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO copropiedades (
                    id, nombre, nit, direccion, ciudad, estado
                ) VALUES (?, 'Other property', ?, 'Test',
                          'Rionegro', 'ACTIVA')
                """,
                otherPropertyId,
                "NIT-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 20)
        );
        jdbcTemplate.update(
                """
                INSERT INTO estructuras_copropiedad (
                    id, copropiedad_id, nombre, tipo, estado
                ) VALUES (?, ?, 'Other tower', 'TORRE', 'ACTIVA')
                """,
                otherStructureId,
                otherPropertyId
        );
        jdbcTemplate.update(
                """
                INSERT INTO unidades_residenciales (
                    id, copropiedad_id, estructura_id,
                    numero, tipo, estado
                ) VALUES (?, ?, ?, '101', 'APARTAMENTO', 'ACTIVA')
                """,
                otherUnitId,
                otherPropertyId,
                otherStructureId
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO usuarios_copropiedades_unidades (
                            id, copropiedad_id,
                            usuario_copropiedad_id,
                            unidad_residencial_id,
                            tipo_vinculacion, es_principal,
                            estado, fecha_inicio
                        ) VALUES (?, ?, ?, ?, 'PROPIETARIO',
                                  FALSE, 'ACTIVA', CURRENT_DATE)
                        """,
                        UUID.randomUUID(),
                        otherPropertyId,
                        UUID.fromString(
                                "70000000-0000-0000-0000-000000000001"
                        ),
                        otherUnitId
                )
        );
    }

    @Test
    void rejectsDuplicateActiveRelationshipOfSameType() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO usuarios_copropiedades_unidades (
                            id, copropiedad_id,
                            usuario_copropiedad_id,
                            unidad_residencial_id,
                            tipo_vinculacion, es_principal,
                            estado, fecha_inicio
                        ) VALUES (
                            ?,
                            '10000000-0000-0000-0000-000000000001',
                            '70000000-0000-0000-0000-000000000001',
                            '50000000-0000-0000-0000-000000000101',
                            'PROPIETARIO', FALSE, 'ACTIVA',
                            CURRENT_DATE
                        )
                        """,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO usuarios_copropiedades_unidades (
                            id, copropiedad_id,
                            usuario_copropiedad_id,
                            unidad_residencial_id,
                            tipo_vinculacion, es_principal,
                            estado, fecha_inicio, fecha_fin
                        ) VALUES (
                            ?,
                            '10000000-0000-0000-0000-000000000001',
                            '70000000-0000-0000-0000-000000000004',
                            '50000000-0000-0000-0000-000000000202',
                            'RESIDENTE', FALSE, 'FINALIZADA',
                            DATE '2026-02-01', DATE '2026-01-31'
                        )
                        """,
                        UUID.randomUUID()
                )
        );
    }

}
