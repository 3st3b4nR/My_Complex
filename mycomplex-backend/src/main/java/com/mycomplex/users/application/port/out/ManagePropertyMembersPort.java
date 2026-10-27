package com.mycomplex.users.application.port.out;

import java.util.UUID;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberCandidate;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;

public interface ManagePropertyMembersPort {

    PropertyMemberCandidate findCandidate(UUID propertyId, String email);

    PropertyMember register(
            PropertyMemberRegistration registration,
            String auth0Subject
    );

    void unlink(PropertyMemberUnlink unlink, String auth0Subject);
}
