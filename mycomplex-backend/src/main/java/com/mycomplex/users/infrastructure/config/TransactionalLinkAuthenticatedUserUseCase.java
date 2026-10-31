package com.mycomplex.users.infrastructure.config;

import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;

public class TransactionalLinkAuthenticatedUserUseCase
        implements LinkAuthenticatedUserUseCase {

    private final LinkAuthenticatedUserUseCase delegate;

    public TransactionalLinkAuthenticatedUserUseCase(
            LinkAuthenticatedUserUseCase delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public boolean link(String auth0Subject) {
        return delegate.link(auth0Subject);
    }
}
