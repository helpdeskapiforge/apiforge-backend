package com.apiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "mock_servers")
@Data
public class MockServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int port; // Virtual port (we simulate this using URL path)
    
    // e.g., /mock/123/users -> 123 is the uuid or unique path prefix
    @Column(unique = true)
    private String pathPrefix; 

    @ManyToOne
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    private Workspace workspace;

    @OneToMany(mappedBy = "mockServer", cascade = CascadeType.ALL)
    private List<MockRoute> routes;
}