package com.apiplatform.controller;

import com.apiplatform.model.Collection;
import com.apiplatform.model.RequestItem;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.ApiRequestDto;
import com.apiplatform.repository.CollectionRepository;
import com.apiplatform.repository.RequestItemRepository;
import com.apiplatform.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/requests")
public class RequestItemController {

    @Autowired RequestItemRepository requestItemRepository;
    @Autowired CollectionRepository collectionRepository;
    @Autowired WorkspaceRepository workspaceRepository;

    // GET all requests inside a specific collection
    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<List<RequestItem>> getRequestsByCollection(@PathVariable Long collectionId) {
        return ResponseEntity.ok(requestItemRepository.findByCollectionId(collectionId));
    }

    // POST (Save) a new Request
    @PostMapping("/create")
    public ResponseEntity<?> createRequest(@RequestBody ApiRequestDto apiRequestDto) {
        Collection collection = collectionRepository.findById(apiRequestDto.getCollectionId())
                .orElseThrow(() -> new RuntimeException("Error: Collection not found."));

        Workspace workspace = workspaceRepository.findById(apiRequestDto.getWorkspaceId())
                .orElseThrow(() -> new RuntimeException("Error: Workspace not found."));

        RequestItem requestItem = new RequestItem();
        requestItem.setName(apiRequestDto.getName());
        requestItem.setMethod(apiRequestDto.getMethod());
        requestItem.setUrl(apiRequestDto.getUrl());
        requestItem.setHeaders(apiRequestDto.getHeaders());
        requestItem.setBody(apiRequestDto.getBody());
        requestItem.setAuthConfig(apiRequestDto.getAuthConfig());
        requestItem.setCollection(collection);
        requestItem.setWorkspace(workspace);

        RequestItem savedRequest = requestItemRepository.save(requestItem);
        return ResponseEntity.ok(savedRequest);
    }

    // PUT (Update) an existing Request
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody ApiRequestDto apiRequestDto) {
        RequestItem requestItem = requestItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Request not found."));

        requestItem.setName(apiRequestDto.getName());
        requestItem.setMethod(apiRequestDto.getMethod());
        requestItem.setUrl(apiRequestDto.getUrl());
        requestItem.setHeaders(apiRequestDto.getHeaders());
        requestItem.setBody(apiRequestDto.getBody());
        requestItem.setAuthConfig(apiRequestDto.getAuthConfig());

        requestItemRepository.save(requestItem);
        return ResponseEntity.ok("Request updated successfully!");
    }

    // GET a single request by ID
    @GetMapping("/{id}")
    public ResponseEntity<RequestItem> getRequestById(@PathVariable Long id) {
        RequestItem requestItem = requestItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Request not found."));
        return ResponseEntity.ok(requestItem);
    }

    // DELETE a request
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        requestItemRepository.deleteById(id);
        return ResponseEntity.ok("Request deleted successfully!");
    }
}