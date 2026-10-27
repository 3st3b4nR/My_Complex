package com.mycomplex.users.application.port.in;

import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;

public interface RegisterPropertyMemberUseCase {

    PropertyMemberDetails register(
            PropertyMemberRegistration registration,
            String auth0Subject
    );
}
