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
    public boolean link(String auth0Subject) {
        return persistencePort.linkAuthenticatedUser(auth0Subject);
    }
}
