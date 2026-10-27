package com.mycomplex.users.application.service;

import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.ManagePropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;

public final class UnlinkPropertyMemberService
        implements UnlinkPropertyMemberUseCase {

    private final LoadPropertyMembersPort loadMembersPort;
    private final ManagePropertyMembersPort manageMembersPort;

    public UnlinkPropertyMemberService(
            LoadPropertyMembersPort loadMembersPort,
            ManagePropertyMembersPort manageMembersPort
    ) {
        this.loadMembersPort = loadMembersPort;
        this.manageMembersPort = manageMembersPort;
    }

    @Override
    public void unlink(PropertyMemberUnlink unlink, String auth0Subject) {
        if (unlink == null || unlink.propertyId() == null
                || unlink.membershipId() == null || unlink.version() < 0
                || auth0Subject == null || auth0Subject.isBlank()) {
            throw new InvalidPropertyMemberRegistrationException(
                    "La solicitud de desvinculación no es válida."
            );
        }
        if (!loadMembersPort.hasActiveAdministratorAccess(
                unlink.propertyId(),
                auth0Subject
        )) {
            throw new PropertyMembersAccessDeniedException();
        }
        manageMembersPort.unlink(unlink, auth0Subject);
    }
}
