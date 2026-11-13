package com.towerlab.service;

import com.towerlab.entity.TowerConfigEntity;
import com.towerlab.repository.TowerConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TowerConfigService {

    private final TowerConfigRepository towerConfigRepository;

    public TowerConfigService(TowerConfigRepository towerConfigRepository) {
        this.towerConfigRepository = towerConfigRepository;
    }

    public List<TowerConfigEntity> getAllTowerConfigs() {
        return towerConfigRepository.findAll();
    }
}
