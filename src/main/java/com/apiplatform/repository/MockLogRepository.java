package com.apiplatform.repository;

import com.apiplatform.model.MockLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockLogRepository extends JpaRepository<MockLog, Long> {
    Page<MockLog> findByMockServerIdOrderByTimestampDesc(Long mockServerId, Pageable pageable);
}
