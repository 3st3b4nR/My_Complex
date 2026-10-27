package com.mycomplex.users.application.service;

import java.util.List;
import java.util.UUID;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;

public final class FindPropertyMembersService
        implements FindPropertyMembersUseCase {

    private final LoadPropertyMembersPort loadPropertyMembersPort;

    public FindPropertyMembersService(
            LoadPropertyMembersPort loadPropertyMembersPort
    ) {
        this.loadPropertyMembersPort = loadPropertyMembersPort;
    }

    @Override
    public List<PropertyMember> findByPropertyId(
            UUID propertyId,
            String auth0Subject
    ) {
        if (propertyId == null) {
            throw new IllegalArgumentException(
                    "propertyId must not be null"
            );
        }

        if (auth0Subject == null || auth0Subject.isBlank()) {
            throw new IllegalArgumentException(
                    "auth0Subject must not be blank"
            );
        }

        boolean hasAccess = loadPropertyMembersPort
                .hasActiveAdministratorAccess(
                        propertyId,
                        auth0Subject
                );

        if (!hasAccess) {
            throw new PropertyMembersAccessDeniedException();
        }

        return loadPropertyMembersPort.loadByPropertyId(propertyId);
    }
}
