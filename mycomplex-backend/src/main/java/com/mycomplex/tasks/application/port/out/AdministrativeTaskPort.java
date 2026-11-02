package com.mycomplex.tasks.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.users.api.TaskAccessDirectory.Administrator;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

public interface AdministrativeTaskPort {
    List<AdministrativeTask> find(TaskSearchCriteria criteria);
    Optional<AdministrativeTask> findOne(UUID propertyId, UUID taskId);
    AdministrativeTask create(CreateAdministrativeTask command,
            Administrator administrator, Worker worker);
    Optional<AdministrativeTask> update(UpdateAdministrativeTask command,
            Worker worker);
    Optional<AdministrativeTask> changeStatus(UUID propertyId, UUID taskId,
            TaskStatus status, long version);
}
