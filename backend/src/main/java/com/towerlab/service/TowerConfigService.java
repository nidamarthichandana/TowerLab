package com.towerlab.service;

import com.towerlab.engine.model.TowerType;
import com.towerlab.entity.TowerConfigEntity;
import com.towerlab.repository.TowerConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TowerConfigService {

    private final TowerConfigRepository towerConfigRepository;

    public TowerConfigService(TowerConfigRepository towerConfigRepository) {
        this.towerConfigRepository = towerConfigRepository;
    }

    @PostConstruct
    @Transactional
    public void initTowerConfigs() {
        if (towerConfigRepository.count() == 0) {
            for (TowerType type : TowerType.values()) {
                TowerConfigEntity entity = new TowerConfigEntity(
                        type.name(),
                        type.getCost(),
                        type.getRange(),
                        type.getFireRateTicks(),
                        type.getBaseDamage(),
                        type.getSlowFactor(),
                        type.getSlowDurationTicks(),
                        type.getDescription()
                );
                towerConfigRepository.save(entity);
            }
        }
    }

    public List<TowerConfigEntity> getAllTowerConfigs() {
        return towerConfigRepository.findAll();
    }
}

