package com.apiplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_logs")
@Data
public class MockLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;      // GET, POST
    private String path;        // /users/1
    private int statusCode;     // 200, 500
    private long durationMs;    // Execution time
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String requestBody; // What they sent

    @Column(columnDefinition = "TEXT")
    private String responseBody; // What we sent back

    private boolean isChaosTriggered; // Did we intentionally break it?

    // Link to the server so we can filter logs by server
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_server_id")
    private MockServer mockServer;
}