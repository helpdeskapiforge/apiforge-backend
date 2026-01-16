package com.apiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Get User Details"

    @Column(nullable = false)
    private String method; // GET, POST, PUT, DELETE, etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    // Storing Headers as a JSON string for flexibility
    // Example: {"Content-Type": "application/json", "Authorization": "Bearer ..."}
    @Column(columnDefinition = "TEXT")
    private String headers; 

    // Storing Body (JSON/XML/Text)
    @Column(columnDefinition = "TEXT")
    private String body;

    // Storing Auth Config as JSON
    // Example: {"type": "bearer", "token": "xyz"}
    @Column(columnDefinition = "TEXT")
    private String authConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    @JsonIgnore
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;
}