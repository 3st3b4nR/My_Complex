package com.mycomplex.users.application.port.in;

import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMemberInvitation;

public interface InvitePropertyMemberUseCase {

    PropertyMemberInvitation invite(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    );

    PropertyMemberInvitation resend(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    );
}
