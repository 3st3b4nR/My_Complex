package com.mycomplex.users.application.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;

/** Pure structural validation shared by registration and update. */
public final class ResidentialAssignmentValidator {

    public Optional<ResidentialAssignmentValidationError> validate(
            List<ResidentialAssignmentUpdate> assignments
    ) {
        Set<UUID> unitIds = new HashSet<>();
        int primaryCount = 0;
        for (ResidentialAssignmentUpdate assignment : assignments) {
            if (assignment == null
                    || assignment.unitId() == null
                    || assignment.relationshipType() == null) {
                return Optional.of(
                        ResidentialAssignmentValidationError.INCOMPLETE
                );
            }
            if (!unitIds.add(assignment.unitId())) {
                return Optional.of(
                        ResidentialAssignmentValidationError.DUPLICATE_UNIT
                );
            }
            if (assignment.primary()) {
                primaryCount++;
            }
        }
        if (primaryCount > 1) {
            return Optional.of(
                    ResidentialAssignmentValidationError.MULTIPLE_PRIMARY
            );
        }
        return Optional.empty();
    }
}
