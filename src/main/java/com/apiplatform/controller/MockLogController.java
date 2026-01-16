package com.apiplatform.controller;

import com.apiplatform.model.MockLog;
import com.apiplatform.repository.MockLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/logs")
public class MockLogController {

    @Autowired MockLogRepository mockLogRepository;

    @GetMapping("/server/{serverId}")
    public List<MockLog> getLogs(@PathVariable Long serverId) {
        return mockLogRepository.findByMockServerIdOrderByTimestampDesc(serverId);
    }
}