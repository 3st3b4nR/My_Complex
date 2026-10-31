package com.mycomplex.users.infrastructure.config;

import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;

public class TransactionalUnlinkPropertyMemberUseCase
        implements UnlinkPropertyMemberUseCase {

    private final UnlinkPropertyMemberUseCase delegate;

    public TransactionalUnlinkPropertyMemberUseCase(
            UnlinkPropertyMemberUseCase delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void unlink(PropertyMemberUnlink unlink, String auth0Subject) {
        delegate.unlink(unlink, auth0Subject);
    }
}
