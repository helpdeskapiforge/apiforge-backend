package com.apiplatform.model;

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

    private String password; // Hash this!
    private String fullName;
    private String avatarUrl;

    // A user can own multiple workspaces
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Workspace> workspaces;
}