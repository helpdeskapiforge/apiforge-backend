package com.apiplatform.repository;

import com.apiplatform.model.RequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestItemRepository extends JpaRepository<RequestItem, Long> {
    List<RequestItem> findByCollectionId(Long collectionId);
    List<RequestItem> findByWorkspaceId(Long workspaceId);
}