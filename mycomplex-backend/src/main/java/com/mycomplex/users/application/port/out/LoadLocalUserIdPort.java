package com.mycomplex.users.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadLocalUserIdPort {

    Optional<UUID> findByAuth0Subject(String auth0Subject);
}
