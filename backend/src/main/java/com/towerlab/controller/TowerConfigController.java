package com.towerlab.controller;

import com.towerlab.entity.TowerConfigEntity;
import com.towerlab.service.TowerConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tower-configs")
@CrossOrigin(origins = "*")
public class TowerConfigController {

    private final TowerConfigService towerConfigService;

    public TowerConfigController(TowerConfigService towerConfigService) {
        this.towerConfigService = towerConfigService;
    }

    @GetMapping
    public ResponseEntity<List<TowerConfigEntity>> getAllConfigs() {
        return ResponseEntity.ok(towerConfigService.getAllTowerConfigs());
    }
}

