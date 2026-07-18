package com.apiplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // Defense in depth: even though no controller currently serializes User directly,
    // a future change returning this entity should never leak the bcrypt hash.
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String fullName;
    private String avatarUrl;

    // A user can own multiple workspaces
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Workspace> workspaces;
}