package com.towerlab.repository;

import com.towerlab.entity.BatchExperimentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchExperimentRepository extends JpaRepository<BatchExperimentEntity, String> {
    List<BatchExperimentEntity> findByLevelIdOrderByCreatedAtDesc(String levelId);
}

