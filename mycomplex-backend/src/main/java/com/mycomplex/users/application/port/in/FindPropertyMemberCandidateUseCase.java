package com.mycomplex.users.application.port.in;

import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMemberCandidate;

public interface FindPropertyMemberCandidateUseCase {

    PropertyMemberCandidate find(
            UUID propertyId,
            String email,
            String auth0Subject
    );
}
