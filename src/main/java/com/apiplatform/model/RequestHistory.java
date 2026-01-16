package com.apiplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@Data
public class RequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String url;
    private int status;
    private long durationMs;
    private LocalDateTime timestamp;

    @Column(name = "user_id")
    private Long userId; // Simple user tracking
}