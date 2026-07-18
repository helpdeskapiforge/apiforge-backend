package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.Environment;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.EnvironmentRepository;
import com.apiplatform.security.access.OwnershipGuard;
import com.apiplatform.web.dto.EnvironmentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final WorkspaceService workspaceService;

    public EnvironmentService(EnvironmentRepository environmentRepository, WorkspaceService workspaceService) {
        this.environmentRepository = environmentRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional(readOnly = true)
    public List<Environment> getForWorkspace(Long workspaceId, User user) {
        workspaceService.getOwnedWorkspace(workspaceId, user);
        return environmentRepository.findByWorkspaceId(workspaceId);
    }

    public Environment create(EnvironmentRequest request, User user) {
        if (request.workspaceId() == null) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        Workspace workspace = workspaceService.getOwnedWorkspace(request.workspaceId(), user);
        Environment env = new Environment();
        env.setName(request.name());
        env.setVariables(request.variables());
        env.setWorkspace(workspace);
        return environmentRepository.save(env);
    }

    public Environment update(Long id, EnvironmentRequest request, User user) {
        Environment env = getOwned(id, user);
        if (request.name() != null) env.setName(request.name());
        if (request.variables() != null) env.setVariables(request.variables());
        return environmentRepository.save(env);
    }

    public void delete(Long id, User user) {
        Environment env = getOwned(id, user);
        environmentRepository.delete(env);
    }

    @Transactional(readOnly = true)
    public Environment getOwned(Long id, User user) {
        Environment env = environmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Environment", id));
        OwnershipGuard.assertOwnsWorkspace(env.getWorkspace(), user);
        return env;
    }
}
