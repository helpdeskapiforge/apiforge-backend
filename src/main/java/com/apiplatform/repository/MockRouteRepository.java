package com.apiplatform.repository;

import com.apiplatform.model.MockRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MockRouteRepository extends JpaRepository<MockRoute, Long> {

    List<MockRoute> findByMockServerId(Long mockServerId);

    // Custom query to find the best matching route
    @Query("SELECT r FROM MockRoute r WHERE r.mockServer.id = :serverId AND r.method = :method AND r.path = :path AND r.isEnabled = true")
    Optional<MockRoute> findMatchingRoute(@Param("serverId") Long serverId,
                                          @Param("method") String method,
                                          @Param("path") String path);
}