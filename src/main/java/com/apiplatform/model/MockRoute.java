package com.apiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty; // <--- 1. Import this
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "mock_routes")
@Data
public class MockRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String path;
    private int statusCode;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    private int delayMs;

    // 👇 2. ADD THIS ANNOTATION
    // This tells Java: "When converting to JSON, keep the name 'isEnabled' exactly"
    @Column(name = "is_enabled")
    @JsonProperty("isEnabled")
    private boolean isEnabled = true;

    @Column(name = "is_chaos_enabled")
    @JsonProperty("chaosEnabled") // Good practice to add this one too
    private boolean chaosEnabled = false;

    private double failureRate = 0.0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_server_id")
    @JsonIgnore
    private MockServer mockServer;
}