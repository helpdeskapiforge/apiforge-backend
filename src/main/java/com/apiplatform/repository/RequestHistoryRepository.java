package com.apiplatform.repository;

import com.apiplatform.model.RequestHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    Page<RequestHistory> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}
