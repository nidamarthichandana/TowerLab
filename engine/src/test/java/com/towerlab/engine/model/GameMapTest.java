package com.towerlab.engine.model;

import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.PathSegment;
import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapTest {

    // Sample path from GR-8: (0,1) -> (6,1) -> (6,5) -> (10,5) -> (10,2) -> (11,2) on a 12x8 grid
    private final List<Position> sampleWaypoints = List.of(
            new Position(0, 1),
            new Position(6, 1),
            new Position(6, 5),
            new Position(10, 5),
            new Position(10, 2),
            new Position(11, 2)
    );

    private final LevelConfig sampleLevelConfig = TestLevelHelper.createStandardLevel(
            "SampleLevel", 12, 8, sampleWaypoints, List.of("Cannon"),
            List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
    );

    @Test
    @DisplayName("GameMap properties and dimensions match LevelConfig")
    void testMapProperties() {
        GameMap map = new GameMap(sampleLevelConfig);
        assertEquals(12, map.width());
        assertEquals(8, map.height());
        assertEquals(18.0, map.path().totalLength(), 1e-6);
        assertEquals(19, map.pathCells().size(), "Sample path of length 18 covers 19 discrete cells");
    }

    @Test
    @DisplayName("Waypoint cells and mid-segment cells are on path and not buildable")
    void testPathCellsNotBuildable() {
        GameMap map = new GameMap(sampleLevelConfig);

        // All waypoints are on path and not buildable
        for (Position wp : sampleWaypoints) {
            assertTrue(map.isOnPath(wp), "Waypoint " + wp + " must be on path");
            assertFalse(map.isBuildable(wp), "Waypoint " + wp + " must NOT be buildable");
            assertTrue(map.isInsideGrid(wp), "Waypoint " + wp + " must be inside grid");
        }

        // Mid-segment cells
        List<Position> midSegmentCells = List.of(
                new Position(1, 1),
                new Position(3, 1),
                new Position(5, 1),
                new Position(6, 2),
                new Position(6, 4),
                new Position(7, 5),
                new Position(9, 5),
                new Position(10, 4),
                new Position(10, 3)
        );

        for (Position cell : midSegmentCells) {
            assertTrue(map.isOnPath(cell), "Mid-segment cell " + cell + " must be on path");
            assertFalse(map.isBuildable(cell), "Mid-segment cell " + cell + " must NOT be buildable");
        }
    }

    @Test
    @DisplayName("Ordinary grid cells not on path are buildable")
    void testOrdinaryCellsBuildable() {
        GameMap map = new GameMap(sampleLevelConfig);

        List<Position> ordinaryCells = List.of(
                new Position(0, 0),
                new Position(0, 2),
                new Position(2, 2),
                new Position(5, 5),
                new Position(7, 0),
                new Position(11, 7)
        );

        for (Position cell : ordinaryCells) {
            assertTrue(map.isInsideGrid(cell));
            assertFalse(map.isOnPath(cell));
            assertTrue(map.isBuildable(cell), "Cell " + cell + " should be buildable");
        }
    }

    @Test
    @DisplayName("Cells outside the grid boundaries are not inside grid and not buildable")
    void testOutsideGridCells() {
        GameMap map = new GameMap(sampleLevelConfig);

        List<Position> outsideCells = List.of(
                new Position(-1, 0),
                new Position(0, -1),
                new Position(12, 1),
                new Position(5, 8),
                new Position(-5, -5),
                new Position(20, 20)
        );

        for (Position cell : outsideCells) {
            assertFalse(map.isInsideGrid(cell));
            assertFalse(map.isBuildable(cell), "Out-of-grid cell " + cell + " must NOT be buildable");
        }
    }

    @Test
    @DisplayName("Null position queries reject null arguments")
    void testNullPositionQueries() {
        GameMap map = new GameMap(sampleLevelConfig);
        assertThrows(IllegalArgumentException.class, () -> map.isInsideGrid(null));
        assertThrows(IllegalArgumentException.class, () -> map.isOnPath(null));
        assertThrows(IllegalArgumentException.class, () -> map.isBuildable(null));
    }

    @Test
    @DisplayName("Invalid or null LevelConfig is rejected")
    void testInvalidLevelConfigRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GameMap(null));

        LevelConfig invalidDim = TestLevelHelper.createStandardLevel("Invalid", -1, 5, sampleWaypoints, List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> new GameMap(invalidDim));
    }
}
