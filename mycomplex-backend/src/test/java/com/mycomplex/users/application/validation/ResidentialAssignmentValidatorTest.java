package com.mycomplex.users.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;

class ResidentialAssignmentValidatorTest {

    private final ResidentialAssignmentValidator validator =
            new ResidentialAssignmentValidator();

    @Test
    void acceptsEmptyAndStructurallyValidAssignments() {
        assertTrue(validator.validate(List.of()).isEmpty());
        assertTrue(validator.validate(List.of(
                assignment(UUID.randomUUID(), true)
        )).isEmpty());
    }

    @Test
    void rejectsIncompleteAssignment() {
        assertEquals(
                ResidentialAssignmentValidationError.INCOMPLETE,
                validator.validate(java.util.Collections.singletonList(null))
                        .orElseThrow()
        );
    }

    @Test
    void rejectsDuplicateUnits() {
        UUID unitId = UUID.randomUUID();
        assertEquals(
                ResidentialAssignmentValidationError.DUPLICATE_UNIT,
                validator.validate(List.of(
                        assignment(unitId, false),
                        assignment(unitId, false)
                )).orElseThrow()
        );
    }

    @Test
    void rejectsMultiplePrimaryUnits() {
        assertEquals(
                ResidentialAssignmentValidationError.MULTIPLE_PRIMARY,
                validator.validate(List.of(
                        assignment(UUID.randomUUID(), true),
                        assignment(UUID.randomUUID(), true)
                )).orElseThrow()
        );
    }

    private ResidentialAssignmentUpdate assignment(
            UUID unitId,
            boolean primary
    ) {
        return new ResidentialAssignmentUpdate(
                unitId,
                ResidentialRelationshipType.RESIDENT,
                primary
        );
    }
}
