package com.mycomplex.users.application.port.in;

import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;

public interface UpdatePropertyMemberUseCase {

    PropertyMemberDetails update(
            PropertyMemberUpdate update,
            String auth0Subject
    );
}
