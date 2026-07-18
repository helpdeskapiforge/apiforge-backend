package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.MockRoute;
import com.apiplatform.model.MockServer;
import com.apiplatform.model.User;
import com.apiplatform.repository.MockRouteRepository;
import com.apiplatform.security.access.OwnershipGuard;
import com.apiplatform.web.dto.MockRouteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MockRouteService {

    /** Hard ceiling so a route can't be configured to block a worker thread forever (DoS). */
    private static final int MAX_DELAY_MS = 30_000;

    private final MockRouteRepository mockRouteRepository;
    private final MockServerService mockServerService;

    public MockRouteService(MockRouteRepository mockRouteRepository, MockServerService mockServerService) {
        this.mockRouteRepository = mockRouteRepository;
        this.mockServerService = mockServerService;
    }

    @Transactional(readOnly = true)
    public MockRoute getOwned(Long id, User user) {
        MockRoute route = mockRouteRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mock route", id));
        OwnershipGuard.assertOwnsWorkspace(route.getMockServer().getWorkspace(), user);
        return route;
    }

    @Transactional(readOnly = true)
    public List<MockRoute> getForServer(Long serverId, User user) {
        mockServerService.getOwned(serverId, user);
        return mockRouteRepository.findByMockServerId(serverId);
    }

    public MockRoute create(MockRouteRequest request, User user) {
        if (request.mockServerId() == null) {
            throw new IllegalArgumentException("mockServerId is required.");
        }
        MockServer server = mockServerService.getOwned(request.mockServerId(), user);

        MockRoute route = new MockRoute();
        route.setMockServer(server);
        route.setEnabled(true);
        route.setChaosEnabled(false);
        route.setFailureRate(0.0);
        applyDto(request, route);
        return mockRouteRepository.save(route);
    }

    public MockRoute update(Long id, MockRouteRequest request, User user) {
        MockRoute route = getOwned(id, user);
        applyDto(request, route);
        return mockRouteRepository.save(route);
    }

    public void delete(Long id, User user) {
        MockRoute route = getOwned(id, user);
        mockRouteRepository.delete(route);
    }

    private void applyDto(MockRouteRequest dto, MockRoute route) {
        if (dto.method() != null) route.setMethod(dto.method());
        if (dto.path() != null) route.setPath(dto.path());
        if (dto.statusCode() != null) route.setStatusCode(dto.statusCode());
        if (dto.contentType() != null) route.setContentType(dto.contentType());
        if (dto.responseBody() != null) route.setResponseBody(dto.responseBody());
        if (dto.responseHeaders() != null) route.setResponseHeaders(dto.responseHeaders());
        if (dto.delayMs() != null) {
            int delay = Math.max(0, Math.min(dto.delayMs(), MAX_DELAY_MS));
            route.setDelayMs(delay);
        }
        if (dto.isEnabled() != null) route.setEnabled(dto.isEnabled());
        if (dto.chaosEnabled() != null) route.setChaosEnabled(dto.chaosEnabled());
        if (dto.failureRate() != null) {
            double rate = Math.max(0.0, Math.min(dto.failureRate(), 1.0));
            route.setFailureRate(rate);
        }
    }
}
