package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.infrastructure.adapter.out.persistence.PropertyMemberPersistenceAdapter;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberRow;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class UserPropertyJpaRepositoryTest {

    private static final UUID LOS_CEREZOS_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );

    @Autowired
    private UserPropertyJpaRepository repository;

    @Autowired
    private PropertyMemberPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void authorizesOnlyActivePropertyAdministrator() {
        UUID propertyId = insertProperty("Access property");
        String administratorSubject = "auth0|" + UUID.randomUUID();
        UUID administratorId = insertUser(
                administratorSubject,
                "ACTIVO",
                false
        );
        insertMembership(
                administratorId,
                propertyId,
                "ADMIN_COPROPIEDAD",
                "ACTIVA",
                false
        );

        String residentSubject = "auth0|" + UUID.randomUUID();
        UUID residentId = insertUser(
                residentSubject,
                "ACTIVO",
                false
        );
        insertMembership(
                residentId,
                propertyId,
                "RESIDENTE",
                "ACTIVA",
                false
        );

        assertTrue(repository.hasActiveAdministratorAccess(
                propertyId,
                administratorSubject
        ));
        assertFalse(repository.hasActiveAdministratorAccess(
                propertyId,
                residentSubject
        ));
        assertFalse(repository.hasActiveAdministratorAccess(
                propertyId,
                "auth0|not-associated"
        ));
    }

    @Test
    void returnsNonDeletedMembersOnlyFromRequestedProperty() {
        UUID requestedPropertyId = insertProperty(
                "Requested property"
        );
        UUID otherPropertyId = insertProperty("Other property");

        UUID activeMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        insertMembership(
                activeMemberId,
                requestedPropertyId,
                "PROPIETARIO",
                "ACTIVA",
                false
        );

        UUID blockedMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "BLOQUEADO",
                false
        );
        insertMembership(
                blockedMemberId,
                requestedPropertyId,
                "RESIDENTE",
                "SUSPENDIDA",
                false
        );

        UUID deletedMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                true
        );
        insertMembership(
                deletedMemberId,
                requestedPropertyId,
                "TRABAJADOR",
                "ACTIVA",
                false
        );

        UUID otherPropertyMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        insertMembership(
                otherPropertyMemberId,
                otherPropertyId,
                "VIGILANTE",
                "ACTIVA",
                false
        );

        List<PropertyMemberRow> members =
                repository.findMemberRowsByPropertyId(
                        requestedPropertyId
                );

        assertEquals(2, members.size());
        assertTrue(members.stream().anyMatch(member ->
                member.getUserId().equals(activeMemberId)
                        && member.getUserStatus().equals("ACTIVO")
                        && member.getMembershipStatus().equals("ACTIVA")
        ));
        assertTrue(members.stream().anyMatch(member ->
                member.getUserId().equals(blockedMemberId)
                        && member.getUserStatus().equals("BLOQUEADO")
                        && member.getMembershipStatus().equals("SUSPENDIDA")
        ));
        assertFalse(members.stream().anyMatch(member ->
                member.getUserId().equals(deletedMemberId)
                        || member.getUserId().equals(otherPropertyMemberId)
        ));
    }

    @Test
    void groupsMultipleUnitsWithoutDuplicatingMember() {
        UUID propertyId = insertProperty("Multiple units property");
        UUID memberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        UUID membershipId = insertMembership(
                memberId,
                propertyId,
                "ADMIN_COPROPIEDAD",
                "ACTIVA",
                false
        );
        UUID structureId = insertStructure(propertyId, "Tower A");
        UUID firstUnitId = insertUnit(
                propertyId,
                structureId,
                "101"
        );
        UUID secondUnitId = insertUnit(
                propertyId,
                structureId,
                "102"
        );
        insertResidentialRelationship(
                propertyId,
                membershipId,
                firstUnitId,
                "PROPIETARIO",
                "ACTIVA",
                true,
                false
        );
        insertResidentialRelationship(
                propertyId,
                membershipId,
                secondUnitId,
                "RESIDENTE",
                "ACTIVA",
                false,
                false
        );

        List<PropertyMember> members = adapter.loadByPropertyId(
                propertyId
        );

        assertEquals(1, members.size());
        assertEquals(memberId, members.getFirst().userId());
        assertEquals(2, members.getFirst().units().size());
    }

    @Test
    void keepsMemberWithoutUnitsAndExcludesEndedOrDeletedLinks() {
        UUID propertyId = insertProperty("Current links property");
        UUID memberWithoutUnitId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        insertMembership(
                memberWithoutUnitId,
                propertyId,
                "TRABAJADOR",
                "ACTIVA",
                false
        );

        UUID residentId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        UUID membershipId = insertMembership(
                residentId,
                propertyId,
                "RESIDENTE",
                "ACTIVA",
                false
        );
        UUID structureId = insertStructure(propertyId, "Tower B");
        UUID activeUnitId = insertUnit(propertyId, structureId, "201");
        UUID endedUnitId = insertUnit(propertyId, structureId, "202");
        UUID deletedUnitId = insertUnit(propertyId, structureId, "203");

        insertResidentialRelationship(
                propertyId,
                membershipId,
                activeUnitId,
                "RESIDENTE",
                "ACTIVA",
                true,
                false
        );
        insertResidentialRelationship(
                propertyId,
                membershipId,
                endedUnitId,
                "RESIDENTE",
                "FINALIZADA",
                false,
                false
        );
        insertResidentialRelationship(
                propertyId,
                membershipId,
                deletedUnitId,
                "RESIDENTE",
                "ACTIVA",
                false,
                true
        );

        List<PropertyMember> members = adapter.loadByPropertyId(
                propertyId
        );

        PropertyMember memberWithoutUnit = members.stream()
                .filter(member -> member.userId()
                        .equals(memberWithoutUnitId))
                .findFirst()
                .orElseThrow();
        PropertyMember resident = members.stream()
                .filter(member -> member.userId().equals(residentId))
                .findFirst()
                .orElseThrow();

        assertTrue(memberWithoutUnit.units().isEmpty());
        assertEquals(1, resident.units().size());
        assertEquals(activeUnitId, resident.units().getFirst().unit().id());
    }

    @Test
    void returnsSeededMembersWithSharedAndMultipleUnits() {
        List<PropertyMember> members = adapter.loadByPropertyId(
                LOS_CEREZOS_ID
        );

        PropertyMember john = memberById(
                members,
                "20000000-0000-0000-0000-000000000001"
        );
        PropertyMember ana = memberById(
                members,
                "60000000-0000-0000-0000-000000000001"
        );
        PropertyMember luis = memberById(
                members,
                "60000000-0000-0000-0000-000000000002"
        );
        PropertyMember carolina = memberById(
                members,
                "60000000-0000-0000-0000-000000000003"
        );

        assertEquals(5, members.size());
        assertTrue(john.units().isEmpty());
        assertEquals(2, carolina.units().size());
        assertEquals(ana.units().getFirst().unit().id(),
                luis.units().getFirst().unit().id());
        assertEquals(
                ResidentialRelationshipType.OWNER,
                ana.units().getFirst().relationshipType()
        );
        assertEquals(
                ResidentialRelationshipType.RESIDENT,
                luis.units().getFirst().relationshipType()
        );
    }

    private PropertyMember memberById(
            List<PropertyMember> members,
            String userId
    ) {
        UUID expectedId = UUID.fromString(userId);
        return members.stream()
                .filter(member -> member.userId().equals(expectedId))
                .findFirst()
                .orElseThrow();
    }

    private UUID insertProperty(String name) {
        UUID propertyId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO copropiedades (
                    id, nombre, nit, direccion, ciudad, estado
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                """,
                propertyId,
                name,
                "NIT-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 20),
                "Test address",
                "Rionegro"
        );
        return propertyId;
    }

    private UUID insertUser(
            String auth0Subject,
            String status,
            boolean deleted
    ) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    estado, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                userId,
                auth0Subject,
                userId + "@example.test",
                "Test",
                userId.toString(),
                status,
                deleted
        );
        return userId;
    }

    private UUID insertMembership(
            UUID userId,
            UUID propertyId,
            String role,
            String status,
            boolean deleted
    ) {
        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol,
                    estado, deleted_at
                ) VALUES (?, ?, ?, ?, ?,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                membershipId,
                userId,
                propertyId,
                role,
                status,
                deleted
        );
        return membershipId;
    }

    private UUID insertStructure(UUID propertyId, String name) {
        UUID structureId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO estructuras_copropiedad (
                    id, copropiedad_id, nombre, tipo, estado
                ) VALUES (?, ?, ?, 'TORRE', 'ACTIVA')
                """,
                structureId,
                propertyId,
                name
        );
        return structureId;
    }

    private UUID insertUnit(
            UUID propertyId,
            UUID structureId,
            String number
    ) {
        UUID unitId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO unidades_residenciales (
                    id, copropiedad_id, estructura_id,
                    numero, tipo, estado
                ) VALUES (?, ?, ?, ?, 'APARTAMENTO', 'ACTIVA')
                """,
                unitId,
                propertyId,
                structureId,
                number
        );
        return unitId;
    }

    private void insertResidentialRelationship(
            UUID propertyId,
            UUID membershipId,
            UUID unitId,
            String relationshipType,
            String status,
            boolean primary,
            boolean deleted
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades_unidades (
                    id, copropiedad_id, usuario_copropiedad_id,
                    unidad_residencial_id, tipo_vinculacion,
                    es_principal, estado, fecha_inicio, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                UUID.randomUUID(),
                propertyId,
                membershipId,
                unitId,
                relationshipType,
                primary,
                status,
                deleted
        );
    }
}
