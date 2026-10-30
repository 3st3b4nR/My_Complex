package com.mycomplex.users.domain.model;

import java.util.UUID;

public record ExternalUserIdentity(
        String subject,
        String email,
        UUID mycomplexUserId
) {
}
