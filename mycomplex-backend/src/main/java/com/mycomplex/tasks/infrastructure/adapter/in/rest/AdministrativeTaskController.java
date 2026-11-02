package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.tasks.application.port.in.FindAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.ChangeTaskStatusRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.CreateTaskRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.TaskResponse;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.TaskWorkerResponse;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.UpdateTaskRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/properties/{propertyId}")
public class AdministrativeTaskController {

    private final FindAdministrativeTasksUseCase query;
    private final ManageAdministrativeTasksUseCase commands;
    private final TaskRestMapper mapper;

    public AdministrativeTaskController(
            FindAdministrativeTasksUseCase query,
            ManageAdministrativeTasksUseCase commands,
            TaskRestMapper mapper
    ) {
        this.query = query;
        this.commands = commands;
        this.mapper = mapper;
    }

    @GetMapping("/tasks")
    public List<TaskResponse> find(
            @PathVariable UUID propertyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assignedMembershipId,
            @RequestParam(defaultValue = "false") boolean overdue,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var criteria = new TaskSearchCriteria(
                propertyId, search,
                status == null || status.isBlank() ? null
                        : mapper.status(status),
                priority == null || priority.isBlank() ? null
                        : mapper.priority(priority),
                assignedMembershipId, overdue
        );
        return query.find(criteria, jwt.getSubject()).stream()
                .map(mapper::toResponse).toList();
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse findOne(@PathVariable UUID propertyId,
            @PathVariable UUID taskId, @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(query.findOne(
                propertyId, taskId, jwt.getSubject()
        ));
    }

    @GetMapping("/task-workers")
    public List<TaskWorkerResponse> findWorkers(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return query.findEligibleWorkers(propertyId, jwt.getSubject()).stream()
                .map(worker -> new TaskWorkerResponse(
                        worker.membershipId(), worker.displayName()
                )).toList();
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable UUID propertyId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(commands.create(
                mapper.toDomain(propertyId, request), jwt.getSubject()
        ));
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable UUID propertyId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(commands.update(
                mapper.toDomain(propertyId, taskId, request), jwt.getSubject()
        ));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable UUID propertyId,
            @PathVariable UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(commands.changeStatus(
                propertyId, taskId, mapper.status(request.status()),
                request.version(), jwt.getSubject()
        ));
    }
}
