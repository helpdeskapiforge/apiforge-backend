package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.MockLog;
import com.apiplatform.model.User;
import com.apiplatform.repository.MockLogRepository;
import com.apiplatform.security.access.OwnershipGuard;
import com.apiplatform.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MockLogService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final MockLogRepository mockLogRepository;
    private final MockServerService mockServerService;

    public MockLogService(MockLogRepository mockLogRepository, MockServerService mockServerService) {
        this.mockLogRepository = mockLogRepository;
        this.mockServerService = mockServerService;
    }

    public PageResponse<MockLog> getForServer(Long serverId, int page, int size, User user) {
        // Previously anyone could read another user's mock traffic logs (which may
        // contain sensitive request/response bodies) just by guessing the server id.
        mockServerService.getOwned(serverId, user);

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<MockLog> result = mockLogRepository.findByMockServerIdOrderByTimestampDesc(serverId, pageable);
        return PageResponse.from(result);
    }

    /**
     * The frontend log detail view previously had no way to fetch a single log by id
     * -- it fetched the first page of the server's logs and searched client-side,
     * which silently failed for anything not on page one. This closes that gap.
     */
    public MockLog getOwnedLog(Long id, User user) {
        MockLog log = mockLogRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Mock log", id));
        OwnershipGuard.assertOwnsWorkspace(log.getMockServer().getWorkspace(), user);
        return log;
    }

    private int clampSize(int requested) {
        if (requested <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
