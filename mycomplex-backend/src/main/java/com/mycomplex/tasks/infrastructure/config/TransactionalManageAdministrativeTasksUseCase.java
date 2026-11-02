package com.mycomplex.tasks.infrastructure.config;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;

public class TransactionalManageAdministrativeTasksUseCase
        implements ManageAdministrativeTasksUseCase {

    private final ManageAdministrativeTasksUseCase delegate;

    public TransactionalManageAdministrativeTasksUseCase(
            ManageAdministrativeTasksUseCase delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public AdministrativeTask create(CreateAdministrativeTask command,
            String auth0Subject) {
        return delegate.create(command, auth0Subject);
    }

    @Override
    @Transactional
    public AdministrativeTask update(UpdateAdministrativeTask command,
            String auth0Subject) {
        return delegate.update(command, auth0Subject);
    }

    @Override
    @Transactional
    public AdministrativeTask changeStatus(UUID propertyId, UUID taskId,
            TaskStatus status, long version, String auth0Subject) {
        return delegate.changeStatus(
                propertyId, taskId, status, version, auth0Subject
        );
    }
}
