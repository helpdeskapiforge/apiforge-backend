package com.apiplatform.repository;

import com.apiplatform.ai.AIFeature;
import com.apiplatform.model.AIGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIGenerationRepository extends JpaRepository<AIGeneration, Long> {

    Page<AIGeneration> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AIGeneration> findByUserIdAndFeatureOrderByCreatedAtDesc(Long userId, AIFeature feature, Pageable pageable);
}
