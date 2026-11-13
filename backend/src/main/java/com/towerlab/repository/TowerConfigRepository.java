package com.towerlab.repository;

import com.towerlab.entity.TowerConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TowerConfigRepository extends JpaRepository<TowerConfigEntity, String> {
}

