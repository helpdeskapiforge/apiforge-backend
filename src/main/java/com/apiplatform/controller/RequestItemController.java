package com.apiplatform.controller;

import com.apiplatform.model.RequestItem;
import com.apiplatform.model.User;
import com.apiplatform.payload.request.ApiRequestDto;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.RequestItemService;
import com.apiplatform.web.dto.PageResponse;
import com.apiplatform.web.dto.response.RequestItemResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/requests", "/api/requests"}) // legacy "/api/requests" kept temporarily; see CHANGELOG.md
public class RequestItemController {

    private final RequestItemService requestItemService;
    private final CurrentUserProvider currentUserProvider;

    public RequestItemController(RequestItemService requestItemService, CurrentUserProvider currentUserProvider) {
        this.requestItemService = requestItemService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<PageResponse<RequestItemResponse>> getRequestsByCollection(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        User user = currentUserProvider.getCurrentUser();
        PageResponse<RequestItem> result = requestItemService.getRequestsForCollection(collectionId, page, size, user);
        PageResponse<RequestItemResponse> response = new PageResponse<>(
                result.data().stream().map(ResponseMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), result.hasNext());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestItemResponse> getRequestById(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ResponseMapper.toResponse(requestItemService.getOwnedRequest(id, user)));
    }

    @PostMapping("/create")
    public ResponseEntity<RequestItemResponse> createRequest(@Valid @RequestBody ApiRequestDto apiRequestDto) {
        User user = currentUserProvider.getCurrentUser();
        RequestItem saved = requestItemService.createRequest(apiRequestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequestItemResponse> updateRequest(@PathVariable Long id, @Valid @RequestBody ApiRequestDto apiRequestDto) {
        User user = currentUserProvider.getCurrentUser();
        RequestItem updated = requestItemService.updateRequest(id, apiRequestDto, user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        requestItemService.deleteRequest(id, user);
        return ResponseEntity.noContent().build();
    }
}
