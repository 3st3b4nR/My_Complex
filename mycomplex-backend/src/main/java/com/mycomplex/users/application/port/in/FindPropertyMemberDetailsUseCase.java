package com.mycomplex.users.application.port.in;

import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMemberDetails;

public interface FindPropertyMemberDetailsUseCase {

    PropertyMemberDetails find(
            UUID propertyId,
            UUID membershipId,
            String auth0Subject
    );
}
