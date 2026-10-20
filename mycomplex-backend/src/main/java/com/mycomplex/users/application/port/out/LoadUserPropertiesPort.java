package com.mycomplex.users.application.port.out;

import java.util.List;

import com.mycomplex.users.domain.model.AssignedProperty;

public interface LoadUserPropertiesPort {

    List<AssignedProperty> loadActiveByAuth0Subject(
            String auth0Subject
    );
}