package com.apiplatform.repository;

import com.apiplatform.model.MockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MockLogRepository extends JpaRepository<MockLog, Long> {
    // Get latest logs first
    List<MockLog> findByMockServerIdOrderByTimestampDesc(Long mockServerId);
}