package com.mycomplex.users.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.mycomplex.users.domain.model.ExternalUserIdentity;

public interface ExternalIdentityPort {

    Optional<ExternalUserIdentity> findByEmail(String email);

    ExternalUserIdentity create(
            UUID mycomplexUserId,
            String email,
            String firstNames,
            String lastNames
    );

    void requestInvitation(String email);
}
