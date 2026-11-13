package com.towerlab.controller;

import com.towerlab.dto.LevelDetailDto;
import com.towerlab.dto.LevelSummaryDto;
import com.towerlab.service.LevelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
@CrossOrigin(origins = "*")
public class LevelController {

    private final LevelService levelService;

    public LevelController(LevelService levelService) {
        this.levelService = levelService;
    }

    @GetMapping
    public ResponseEntity<List<LevelSummaryDto>> getLevels() {
        return ResponseEntity.ok(levelService.getAllLevels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LevelDetailDto> getLevelById(@PathVariable String id) {
        return ResponseEntity.ok(levelService.getLevelDetail(id));
    }
}

