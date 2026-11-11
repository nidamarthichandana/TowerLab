package com.towerlab.api.controller;

import com.towerlab.api.dto.CatalogResponse;
import com.towerlab.api.dto.LevelResponse;
import com.towerlab.api.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controller exposing level definitions and unit/tower catalogs.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService must not be null");
    }

    @GetMapping("/levels/{id}")
    public ResponseEntity<LevelResponse> getLevel(@PathVariable String id) {
        return ResponseEntity.ok(catalogService.getLevel(id));
    }

    @GetMapping("/catalog")
    public ResponseEntity<CatalogResponse> getCatalog() {
        return ResponseEntity.ok(catalogService.getCatalog());
    }
}
