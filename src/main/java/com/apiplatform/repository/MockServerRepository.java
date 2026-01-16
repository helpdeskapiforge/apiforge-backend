package com.apiplatform.repository;

import com.apiplatform.model.MockServer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MockServerRepository extends JpaRepository<MockServer, Long> {
    List<MockServer> findByWorkspaceId(Long workspaceId);
    Optional<MockServer> findByPathPrefix(String pathPrefix);
}