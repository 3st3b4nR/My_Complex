package com.mycomplex.users.application.port.in;

import com.mycomplex.users.domain.model.PropertyMemberUnlink;

public interface UnlinkPropertyMemberUseCase {

    void unlink(PropertyMemberUnlink unlink, String auth0Subject);
}
