package com.mycomplex.users.application.service;

import java.util.UUID;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberDetails;

public final class FindPropertyMemberDetailsService
        implements FindPropertyMemberDetailsUseCase {

    private final LoadPropertyMembersPort loadMembersPort;
    private final FindAvailableResidentialUnitsUseCase findUnitsUseCase;

    public FindPropertyMemberDetailsService(
            LoadPropertyMembersPort loadMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        this.loadMembersPort = loadMembersPort;
        this.findUnitsUseCase = findUnitsUseCase;
    }

    @Override
    public PropertyMemberDetails find(
            UUID propertyId,
            UUID membershipId,
            String auth0Subject
    ) {
        validateIdentifiers(propertyId, membershipId, auth0Subject);

        if (!loadMembersPort.hasActiveAdministratorAccess(
                propertyId,
                auth0Subject
        )) {
            throw new PropertyMembersAccessDeniedException();
        }

        PropertyMember member = loadMembersPort.loadByMembershipId(
                propertyId,
                membershipId
        ).orElseThrow(PropertyMemberNotFoundException::new);

        return new PropertyMemberDetails(
                member,
                findUnitsUseCase.findByPropertyId(propertyId)
        );
    }

    private void validateIdentifiers(
            UUID propertyId,
            UUID membershipId,
            String auth0Subject
    ) {
        if (propertyId == null || membershipId == null) {
            throw new IllegalArgumentException(
                    "propertyId and membershipId must not be null"
            );
        }
        if (auth0Subject == null || auth0Subject.isBlank()) {
            throw new IllegalArgumentException(
                    "auth0Subject must not be blank"
            );
        }
    }
}
