package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.MockServer;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.MockServerRepository;
import com.apiplatform.security.access.OwnershipGuard;
import com.apiplatform.web.dto.MockServerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MockServerService {

    private final MockServerRepository mockServerRepository;
    private final WorkspaceService workspaceService;

    public MockServerService(MockServerRepository mockServerRepository, WorkspaceService workspaceService) {
        this.mockServerRepository = mockServerRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional(readOnly = true)
    public MockServer getOwned(Long id, User user) {
        MockServer server = mockServerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mock server", id));
        OwnershipGuard.assertOwnsWorkspace(server.getWorkspace(), user);
        return server;
    }

    @Transactional(readOnly = true)
    public List<MockServer> getForWorkspace(Long workspaceId, User user) {
        workspaceService.getOwnedWorkspace(workspaceId, user);
        return mockServerRepository.findByWorkspaceId(workspaceId);
    }

    public MockServer create(MockServerRequest request, User user) {
        if (request.workspaceId() == null) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        Workspace workspace = workspaceService.getOwnedWorkspace(request.workspaceId(), user);

        MockServer server = new MockServer();
        server.setName(request.name());
        server.setWorkspace(workspace);
        server.setPort(8080);

        String prefix = (request.pathPrefix() == null || request.pathPrefix().isBlank())
                ? UUID.randomUUID().toString().substring(0, 8)
                : request.pathPrefix().trim();
        server.setPathPrefix(prefix);
        return mockServerRepository.save(server);
    }

    public MockServer update(Long id, MockServerRequest request, User user) {
        MockServer server = getOwned(id, user);
        if (request.name() != null && !request.name().isBlank()) {
            server.setName(request.name());
        }
        if (request.pathPrefix() != null && !request.pathPrefix().isBlank()) {
            server.setPathPrefix(request.pathPrefix());
        }
        return mockServerRepository.save(server);
    }

    public void delete(Long id, User user) {
        MockServer server = getOwned(id, user);
        mockServerRepository.delete(server);
    }
}
