package com.apiplatform.repository;

import com.apiplatform.model.RequestItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {
    List<RequestItem> findByCollectionId(Long collectionId);
    Page<RequestItem> findByCollectionId(Long collectionId, Pageable pageable);
    List<RequestItem> findByWorkspaceId(Long workspaceId);
}