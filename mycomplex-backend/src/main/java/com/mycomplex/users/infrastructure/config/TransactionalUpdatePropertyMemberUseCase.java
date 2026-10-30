package com.mycomplex.users.infrastructure.config;

import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;

/** Keeps residential validation and the locked update in one local transaction. */
public class TransactionalUpdatePropertyMemberUseCase
        implements UpdatePropertyMemberUseCase {

    private final UpdatePropertyMemberUseCase delegate;

    public TransactionalUpdatePropertyMemberUseCase(
            UpdatePropertyMemberUseCase delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public PropertyMemberDetails update(
            PropertyMemberUpdate update,
            String auth0Subject
    ) {
        return delegate.update(update, auth0Subject);
    }
}
