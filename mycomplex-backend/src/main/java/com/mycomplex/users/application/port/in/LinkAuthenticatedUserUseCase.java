package com.mycomplex.users.application.port.in;

public interface LinkAuthenticatedUserUseCase {

    boolean link(String auth0Subject);
}
