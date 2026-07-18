package com.apiplatform.controller;

import com.apiplatform.model.MockLog;
import com.apiplatform.model.User;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.MockLogService;
import com.apiplatform.web.dto.PageResponse;
import com.apiplatform.web.dto.response.MockLogResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/logs", "/api/logs"}) // legacy "/api/logs" kept temporarily; see CHANGELOG.md
public class MockLogController {

    private final MockLogService mockLogService;
    private final CurrentUserProvider currentUserProvider;

    public MockLogController(MockLogService mockLogService, CurrentUserProvider currentUserProvider) {
        this.mockLogService = mockLogService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockLogResponse> getLogById(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ResponseMapper.toResponse(mockLogService.getOwnedLog(id, user)));
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<PageResponse<MockLogResponse>> getLogs(
            @PathVariable Long serverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        User user = currentUserProvider.getCurrentUser();
        PageResponse<MockLog> result = mockLogService.getForServer(serverId, page, size, user);
        PageResponse<MockLogResponse> response = new PageResponse<>(
                result.data().stream().map(ResponseMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), result.hasNext());
        return ResponseEntity.ok(response);
    }
}
