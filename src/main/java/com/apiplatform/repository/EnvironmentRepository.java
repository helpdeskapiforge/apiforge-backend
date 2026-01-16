package com.apiplatform.repository;

import com.apiplatform.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    List<Environment> findByWorkspaceId(Long workspaceId);
}