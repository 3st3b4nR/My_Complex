package com.mycomplex.users.application.service;

import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;

public class LinkAuthenticatedUserService
        implements LinkAuthenticatedUserUseCase {

    private final UserInvitationPersistencePort persistencePort;

    public LinkAuthenticatedUserService(
            UserInvitationPersistencePort persistencePort
    ) {
        this.persistencePort = persistencePort;
    }

    @Override
    public void link(String auth0Subject) {
        persistencePort.linkAuthenticatedUser(auth0Subject);
    }
}
