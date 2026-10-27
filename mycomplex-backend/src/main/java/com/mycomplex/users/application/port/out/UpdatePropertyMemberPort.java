package com.mycomplex.users.application.port.out;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;

public interface UpdatePropertyMemberPort {

    PropertyMember update(
            PropertyMemberUpdate update,
            String auth0Subject
    );
}
