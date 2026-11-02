package com.mycomplex.tasks.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.tasks.application.exception.TaskConflictException;
import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json",
    "fcm.enabled=false"
})
@Transactional
class AdministrativeTaskPersistenceIntegrationTest {

    private static final UUID PROPERTY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );
    private static final String ADMIN_SUBJECT =
            "auth0|6ad6a66c5703ba3cd5629f70";

    @Autowired
    private ManageAdministrativeTasksUseCase useCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsTaskAndOutboxAtomicallyAndRejectsStaleVersion() {
        assertCleanFlywayHistoryThroughV11AndTaskSchema();
        UUID workerUserId = UUID.randomUUID();
        UUID workerMembershipId = UUID.randomUUID();
        UUID secondWorkerUserId = UUID.randomUUID();
        UUID secondWorkerMembershipId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    es_admin_sistema, estado
                ) VALUES (?, ?, ?, 'Ana', 'Trabajadora', FALSE, 'ACTIVO')
                """, workerUserId, "auth0|worker-" + workerUserId,
                "worker-" + workerUserId + "@test.local");
        jdbcTemplate.update("""
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    es_admin_sistema, estado
                ) VALUES (?, ?, ?, 'Luis', 'Trabajador', FALSE, 'ACTIVO')
                """, secondWorkerUserId,
                "auth0|worker-" + secondWorkerUserId,
                "worker-" + secondWorkerUserId + "@test.local");
        jdbcTemplate.update("""
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol, estado
                ) VALUES (?, ?, ?, 'TRABAJADOR', 'ACTIVA')
                """, workerMembershipId, workerUserId, PROPERTY_ID);
        jdbcTemplate.update("""
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol, estado
                ) VALUES (?, ?, ?, 'TRABAJADOR', 'ACTIVA')
                """, secondWorkerMembershipId, secondWorkerUserId,
                PROPERTY_ID);

        var created = useCase.create(new CreateAdministrativeTask(
                PROPERTY_ID, "Revisar ascensor", "Validar funcionamiento",
                TaskPriority.HIGH, "Torre 1",
                OffsetDateTime.now().plusDays(1), null,
                workerMembershipId
        ), ADMIN_SUBJECT);

        Integer outboxCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM eventos_notificacion_fcm
                WHERE tarea_id = ? AND estado = 'PENDIENTE'
                """, Integer.class, created.id());
        assertThat(outboxCount).isEqualTo(1);

        var update = new UpdateAdministrativeTask(
                PROPERTY_ID, created.id(), created.title(),
                created.description(), created.priority(), created.location(),
                created.dueAt(), created.administrativeNote(),
                secondWorkerMembershipId, created.version()
        );
        useCase.update(update, ADMIN_SUBJECT);
        Integer reassignmentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM eventos_notificacion_fcm
                WHERE tarea_id = ? AND tipo_evento = 'TAREA_REASIGNADA'
                  AND vinculacion_asignada_id = ?
                """, Integer.class, created.id(), secondWorkerMembershipId);
        assertThat(reassignmentCount).isEqualTo(1);
        assertThatThrownBy(() -> useCase.update(update, ADMIN_SUBJECT))
                .isInstanceOf(TaskConflictException.class);

        Integer taskAuditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM eventos_auditoria_seguridad
                WHERE tipo_evento = 'TASK_CREATED' AND origen = 'tasks'
                """, Integer.class);
        assertThat(taskAuditCount).isEqualTo(1);
    }

    private void assertCleanFlywayHistoryThroughV11AndTaskSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                """, Integer.class);
        Integer v2Count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '2' AND success = TRUE
                """, Integer.class);
        Integer v10Count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '10' AND success = TRUE
                """, Integer.class);
        Integer v11Count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '11' AND success = TRUE
                """, Integer.class);
        Integer v11RecipientCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM usuarios
                WHERE id = '60000000-0000-0000-0000-000000000005'
                  AND auth0_subject = 'seed|esteban.roldan.propietario'
                  AND correo = 'esteban.roldanrm@gmail.com'
                  AND estado = 'ACTIVO'
                """, Integer.class);
        Integer duplicatedVersions = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT version FROM flyway_schema_history
                    WHERE version IS NOT NULL
                    GROUP BY version HAVING COUNT(*) > 1
                ) duplicates
                """, Integer.class);
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'tareas', 'instalaciones_fcm',
                    'eventos_notificacion_fcm'
                  )
                """, Integer.class);
        Integer validatedForeignKeys = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint constraint_definition
                JOIN pg_class relation
                  ON relation.oid = constraint_definition.conrelid
                WHERE relation.relname IN (
                    'tareas', 'instalaciones_fcm',
                    'eventos_notificacion_fcm'
                )
                  AND constraint_definition.contype = 'f'
                  AND constraint_definition.convalidated = TRUE
                """, Integer.class);
        String checks = jdbcTemplate.queryForObject("""
                SELECT STRING_AGG(pg_get_constraintdef(
                    constraint_definition.oid), ' ')
                FROM pg_constraint constraint_definition
                JOIN pg_class relation
                  ON relation.oid = constraint_definition.conrelid
                WHERE relation.relname IN (
                    'tareas', 'instalaciones_fcm',
                    'eventos_notificacion_fcm'
                ) AND constraint_definition.contype = 'c'
                """, String.class);
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename IN (
                    'tareas', 'instalaciones_fcm',
                    'eventos_notificacion_fcm'
                  )
                """, Integer.class);

        assertThat(migrationCount).isEqualTo(11);
        assertThat(v2Count).isEqualTo(1);
        assertThat(v10Count).isEqualTo(1);
        assertThat(v11Count).isEqualTo(1);
        assertThat(v11RecipientCount).isEqualTo(1);
        assertThat(duplicatedVersions).isZero();
        assertThat(tableCount).isEqualTo(3);
        assertThat(validatedForeignKeys).isEqualTo(7);
        assertThat(checks)
                .contains("BAJA", "MEDIA", "ALTA", "URGENTE")
                .contains("PENDIENTE", "EN_PROGRESO", "COMPLETADA")
                .contains("ENVIADA", "FALLIDA", "SIN_DESTINO");
        assertThat(indexCount).isGreaterThanOrEqualTo(12);
    }
}
