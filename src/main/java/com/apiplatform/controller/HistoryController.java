package com.apiplatform.controller;

import com.apiplatform.exception.ForbiddenOperationException;
import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.RequestHistory;
import com.apiplatform.model.User;
import com.apiplatform.repository.RequestHistoryRepository;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.web.dto.PageResponse;
import com.apiplatform.web.dto.response.RequestHistoryResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/history", "/api/history"}) // legacy "/api/history" kept temporarily; see CHANGELOG.md
public class HistoryController {

    private static final int MAX_PAGE_SIZE = 200;

    private final RequestHistoryRepository historyRepository;
    private final CurrentUserProvider currentUserProvider;

    public HistoryController(RequestHistoryRepository historyRepository, CurrentUserProvider currentUserProvider) {
        this.historyRepository = historyRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<RequestHistoryResponse>> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        User user = currentUserProvider.getCurrentUser();
        int clampedSize = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
        Page<RequestHistory> result = historyRepository.findByUserIdOrderByTimestampDesc(
                user.getId(), PageRequest.of(Math.max(page, 0), clampedSize));
        PageResponse<RequestHistory> paged = PageResponse.from(result);
        PageResponse<RequestHistoryResponse> response = new PageResponse<>(
                paged.data().stream().map(ResponseMapper::toResponse).toList(),
                paged.page(), paged.size(), paged.totalElements(), paged.totalPages(), paged.hasNext());
        return ResponseEntity.ok(response);
    }

    /**
     * The history detail view previously had no way to fetch a single entry by id --
     * it fetched "/history/me" and searched client-side, which silently failed for
     * anything beyond the first page. This closes that gap, the same way the mock log
     * viewer's equivalent workaround was fixed.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RequestHistoryResponse> getHistoryById(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        RequestHistory entry = historyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("History entry", id));
        if (entry.getUserId() == null || !entry.getUserId().equals(user.getId())) {
            throw ForbiddenOperationException.notOwner("history entry");
        }
        return ResponseEntity.ok(ResponseMapper.toResponse(entry));
    }
}
