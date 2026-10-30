package com.mycomplex.users.application.port.in;

public interface LinkAuthenticatedUserUseCase {

    void link(String auth0Subject);
}
