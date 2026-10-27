package com.mycomplex.users.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMember;

public interface LoadPropertyMembersPort {

    boolean hasActiveAdministratorAccess(
            UUID propertyId,
            String auth0Subject
    );

    List<PropertyMember> loadByPropertyId(UUID propertyId);

    Optional<PropertyMember> loadByMembershipId(
            UUID propertyId,
            UUID membershipId
    );
}
