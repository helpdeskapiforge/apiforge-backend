package com.apiplatform.repository;

import com.apiplatform.model.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    // Get latest 50 requests for a user
    List<RequestHistory> findTop50ByUserIdOrderByTimestampDesc(Long userId);
}