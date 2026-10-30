package com.mycomplex.users.infrastructure.config;

import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;

/** Keeps residential validation and association writes in one local transaction. */
public class TransactionalRegisterPropertyMemberUseCase
        implements RegisterPropertyMemberUseCase {

    private final RegisterPropertyMemberUseCase delegate;

    public TransactionalRegisterPropertyMemberUseCase(
            RegisterPropertyMemberUseCase delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public PropertyMemberDetails register(
            PropertyMemberRegistration registration,
            String auth0Subject
    ) {
        return delegate.register(registration, auth0Subject);
    }
}
