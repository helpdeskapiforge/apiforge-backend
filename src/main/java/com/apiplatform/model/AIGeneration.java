package com.apiplatform.model;

import com.apiplatform.ai.AIFeature;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row per AI tool invocation: what was asked, what came back, which provider served
 * it, and how long it took. Backs the "AI Tools > History" sidebar and gives us an audit
 * trail for prompt/latency/provider/token logging (see README.md > AI Providers > Logging).
 */
@Entity
@Table(name = "ai_generations")
@Data
@NoArgsConstructor
public class AIGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AIFeature feature;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(length = 120)
    private String model;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String result;

    private Integer tokensUsed;

    private Long latencyMs;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
