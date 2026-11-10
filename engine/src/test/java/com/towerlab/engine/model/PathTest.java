package com.towerlab.engine.model;

import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTest {

    // 18-cell path from GR-8:
    // (0,1) -> (6,1) [len 6] -> (6,5) [len 4] -> (10,5) [len 4] -> (10,2) [len 3] -> (11,2) [len 1]
    private final List<Position> waypoints = List.of(
            new Position(0, 1),
            new Position(6, 1),
            new Position(6, 5),
            new Position(10, 5),
            new Position(10, 2),
            new Position(11, 2)
    );

    @Test
    @DisplayName("Total length matches sum of Manhattan distances along segments")
    void testTotalLength() {
        Path path = new Path(waypoints);
        assertEquals(18.0, path.totalLength(), 1e-6);
        assertEquals(6, path.waypoints().size());
    }

    @Test
    @DisplayName("pointAt(0) returns start waypoint position")
    void testPointAtZeroReturnsStart() {
        Path path = new Path(waypoints);
        ContinuousPosition start = path.pointAt(0.0);
        assertEquals(0.0, start.x(), 1e-6);
        assertEquals(1.0, start.y(), 1e-6);
    }

    @Test
    @DisplayName("pointAt(totalLength) returns end waypoint position")
    void testPointAtTotalLengthReturnsEnd() {
        Path path = new Path(waypoints);
        ContinuousPosition end = path.pointAt(path.totalLength());
        assertEquals(11.0, end.x(), 1e-6);
        assertEquals(2.0, end.y(), 1e-6);
    }

    @Test
    @DisplayName("pointAt accurately interpolates positions along intermediate path segments")
    void testIntermediateInterpolation() {
        Path path = new Path(waypoints);

        // Midpoint of segment 1: distance 3.0 along (0,1) -> (6,1)
        ContinuousPosition p1 = path.pointAt(3.0);
        assertEquals(3.0, p1.x(), 1e-6);
        assertEquals(1.0, p1.y(), 1e-6);

        // Exact corner 1: distance 6.0
        ContinuousPosition corner1 = path.pointAt(6.0);
        assertEquals(6.0, corner1.x(), 1e-6);
        assertEquals(1.0, corner1.y(), 1e-6);

        // Midpoint of segment 2: distance 8.0 along (6,1) -> (6,5)
        ContinuousPosition p2 = path.pointAt(8.0);
        assertEquals(6.0, p2.x(), 1e-6);
        assertEquals(3.0, p2.y(), 1e-6);

        // Exact corner 2: distance 10.0
        ContinuousPosition corner2 = path.pointAt(10.0);
        assertEquals(6.0, corner2.x(), 1e-6);
        assertEquals(5.0, corner2.y(), 1e-6);

        // Midpoint of segment 3: distance 12.0 along (6,5) -> (10,5)
        ContinuousPosition p3 = path.pointAt(12.0);
        assertEquals(8.0, p3.x(), 1e-6);
        assertEquals(5.0, p3.y(), 1e-6);

        // Midpoint of segment 4: distance 15.5 along (10,5) -> (10,2) [decreasing Y]
        ContinuousPosition p4 = path.pointAt(15.5);
        assertEquals(10.0, p4.x(), 1e-6);
        assertEquals(3.5, p4.y(), 1e-6);

        // Exact corner 4: distance 17.0
        ContinuousPosition corner4 = path.pointAt(17.0);
        assertEquals(10.0, corner4.x(), 1e-6);
        assertEquals(2.0, corner4.y(), 1e-6);

        // Final segment: distance 17.5 along (10,2) -> (11,2)
        ContinuousPosition p5 = path.pointAt(17.5);
        assertEquals(10.5, p5.x(), 1e-6);
        assertEquals(2.0, p5.y(), 1e-6);
    }

    @Test
    @DisplayName("Distances outside [0, totalLength] are clamped to start and end")
    void testClampingForOutOfBoundsDistances() {
        Path path = new Path(waypoints);

        // Negative distance clamped to start (0.0, 1.0)
        ContinuousPosition negative = path.pointAt(-5.0);
        assertEquals(0.0, negative.x(), 1e-6);
        assertEquals(1.0, negative.y(), 1e-6);

        // Distance beyond total length clamped to end (11.0, 2.0)
        ContinuousPosition beyond = path.pointAt(50.0);
        assertEquals(11.0, beyond.x(), 1e-6);
        assertEquals(2.0, beyond.y(), 1e-6);
    }

    @Test
    @DisplayName("Path defensively copies waypoints and returns an unmodifiable list")
    void testImmutabilityAndDefensiveCopy() {
        List<Position> mutableList = new ArrayList<>(waypoints);
        Path path = new Path(mutableList);

        // Modifying source list does not affect path
        mutableList.add(new Position(12, 2));
        assertEquals(6, path.waypoints().size());

        // Attempting to modify returned waypoints throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> path.waypoints().add(new Position(99, 99)));
    }

    @Test
    @DisplayName("Path rejects invalid waypoint configurations")
    void testInvalidWaypointsRejected() {
        // Null list
        assertThrows(IllegalArgumentException.class, () -> new Path((List<Position>) null));

        // Empty or single waypoint
        assertThrows(IllegalArgumentException.class, () -> new Path(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Path(List.of(new Position(0, 0))));

        // Null element in waypoints
        List<Position> withNull = new ArrayList<>();
        withNull.add(new Position(0, 0));
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new Path(withNull));

        // Null LevelConfig
        assertThrows(IllegalArgumentException.class, () -> new Path((com.towerlab.engine.config.LevelConfig) null));
    }

    @Test
    @DisplayName("Enemy cannot advance beyond path totalLength and hasReachedEnd stays true")
    void testEnemyAdvanceClampingAtEndpoint() {
        Path path = new Path(waypoints);
        BasicEnemy basic = TestConfigHelper.createBasicEnemy(1); // speed 0.05

        // Advance 400 ticks (400 * 0.05 = 20.0 > 18.0)
        for (int i = 0; i < 400; i++) {
            basic.advance(path);
        }

        assertEquals(18.0, basic.progress(), 1e-6);
        assertTrue(basic.hasReachedEnd());
        assertTrue(basic.hasReachedEnd(path));

        ContinuousPosition position = basic.position(path);
        assertEquals(11.0, position.x(), 1e-6);
        assertEquals(2.0, position.y(), 1e-6);
    }
}
