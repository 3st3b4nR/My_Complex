package com.mycomplex.users.application.port.in;

import java.util.List;
import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMember;

public interface FindPropertyMembersUseCase {

    List<PropertyMember> findByPropertyId(
            UUID propertyId,
            String auth0Subject
    );
}
