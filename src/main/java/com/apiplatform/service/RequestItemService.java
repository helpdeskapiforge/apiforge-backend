package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.Collection;
import com.apiplatform.model.RequestItem;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.ApiRequestDto;
import com.apiplatform.repository.RequestItemRepository;
import com.apiplatform.security.access.OwnershipGuard;
import com.apiplatform.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RequestItemService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final RequestItemRepository requestItemRepository;
    private final CollectionService collectionService;
    private final WorkspaceService workspaceService;

    public RequestItemService(RequestItemRepository requestItemRepository,
                               CollectionService collectionService,
                               WorkspaceService workspaceService) {
        this.requestItemRepository = requestItemRepository;
        this.collectionService = collectionService;
        this.workspaceService = workspaceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestItem> getRequestsForCollection(Long collectionId, int page, int size, User user) {
        collectionService.getOwnedCollection(collectionId, user);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<RequestItem> result = requestItemRepository.findByCollectionId(collectionId, pageable);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public RequestItem getOwnedRequest(Long id, User user) {
        RequestItem item = requestItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Request", id));
        OwnershipGuard.assertOwnsWorkspace(item.getWorkspace(), user);
        return item;
    }

    public RequestItem createRequest(ApiRequestDto dto, User user) {
        if (dto.getCollectionId() == null || dto.getWorkspaceId() == null) {
            throw new IllegalArgumentException("collectionId and workspaceId are required.");
        }
        Collection collection = collectionService.getOwnedCollection(dto.getCollectionId(), user);
        Workspace workspace = workspaceService.getOwnedWorkspace(dto.getWorkspaceId(), user);

        // Data-integrity guard: previously a client could pass a collectionId belonging to
        // one workspace and a workspaceId belonging to another, silently corrupting the tree.
        if (!collection.getWorkspace().getId().equals(workspace.getId())) {
            throw new IllegalArgumentException("The collection does not belong to the given workspace.");
        }

        RequestItem item = new RequestItem();
        applyDto(item, dto);
        item.setCollection(collection);
        item.setWorkspace(workspace);
        return requestItemRepository.save(item);
    }

    public RequestItem updateRequest(Long id, ApiRequestDto dto, User user) {
        RequestItem item = getOwnedRequest(id, user);
        applyDto(item, dto);
        return requestItemRepository.save(item);
    }

    public void deleteRequest(Long id, User user) {
        RequestItem item = getOwnedRequest(id, user);
        requestItemRepository.delete(item);
    }

    private void applyDto(RequestItem item, ApiRequestDto dto) {
        item.setName(dto.getName());
        item.setMethod(dto.getMethod());
        item.setUrl(dto.getUrl());
        item.setHeaders(dto.getHeaders());
        item.setBody(dto.getBody());
        item.setAuthConfig(dto.getAuthConfig());
    }

    private int clampSize(int requested) {
        if (requested <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
