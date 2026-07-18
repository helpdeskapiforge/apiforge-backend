package com.apiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Folder nesting: a "folder" is just a Collection whose parentId points at another
    // Collection in the same workspace. Deliberately a plain Long, not a JPA
    // relationship -- a self-referencing entity association here would mean either
    // infinite JSON recursion (without @JsonIgnore) or awkward lazy-loading chains for
    // what's fundamentally just a foreign key the frontend needs to build a tree from.
    private Long parentId;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    // Link to the Workspace (A collection belongs to a workspace)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion when sending JSON
    private Workspace workspace;

    // A Collection contains multiple Request Items
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> requests;
}