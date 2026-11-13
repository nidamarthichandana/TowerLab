package com.towerlab.engine.model;

import java.util.*;

public class Grid {
    public static final int DEFAULT_WIDTH = 12;
    public static final int DEFAULT_HEIGHT = 8;

    private final int width;
    private final int height;
    private final Set<Coordinate> pathTiles;
    private final Map<Coordinate, Tower> placedTowers;

    public Grid(int width, int height, List<PathWaypoint> waypoints) {
        this.width = width;
        this.height = height;
        this.pathTiles = new HashSet<>();
        this.placedTowers = new HashMap<>();
        computePathTiles(waypoints);
    }

    private void computePathTiles(List<PathWaypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        for (int i = 0; i < waypoints.size() - 1; i++) {
            PathWaypoint from = waypoints.get(i);
            PathWaypoint to = waypoints.get(i + 1);

            int x1 = from.getX();
            int y1 = from.getY();
            int x2 = to.getX();
            int y2 = to.getY();

            // Trace Manhattan or direct grid segments
            int dx = Integer.compare(x2, x1);
            int dy = Integer.compare(y2, y1);

            int cx = x1;
            int cy = y1;
            pathTiles.add(new Coordinate(cx, cy));

            while (cx != x2 || cy != y2) {
                if (cx != x2) cx += dx;
                if (cy != y2) cy += dy;
                pathTiles.add(new Coordinate(cx, cy));
            }
        }
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isPathTile(int x, int y) {
        return pathTiles.contains(new Coordinate(x, y));
    }

    public boolean isOccupied(int x, int y) {
        return placedTowers.containsKey(new Coordinate(x, y));
    }

    public boolean canPlaceTower(int x, int y) {
        return isWithinBounds(x, y) && !isPathTile(x, y) && !isOccupied(x, y);
    }

    public void placeTower(Tower tower) {
        Coordinate coord = new Coordinate(tower.getX(), tower.getY());
        placedTowers.put(coord, tower);
    }

    public Tower getTowerAt(int x, int y) {
        return placedTowers.get(new Coordinate(x, y));
    }

    public Collection<Tower> getAllTowers() {
        return placedTowers.values();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Set<Coordinate> getPathTiles() {
        return Collections.unmodifiableSet(pathTiles);
    }
}

