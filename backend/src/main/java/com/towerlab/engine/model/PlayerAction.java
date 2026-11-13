package com.towerlab.engine.model;

public class PlayerAction {
    public enum ActionType {
        BUILD_TOWER,
        CHANGE_TARGETING
    }

    private int tick;
    private ActionType type;
    private TowerType towerType;
    private int x;
    private int y;
    private TargetingMode targetingMode;

    public PlayerAction() {}

    public PlayerAction(int tick, ActionType type, TowerType towerType, int x, int y, TargetingMode targetingMode) {
        this.tick = tick;
        this.type = type != null ? type : ActionType.BUILD_TOWER;
        this.towerType = towerType;
        this.x = x;
        this.y = y;
        this.targetingMode = targetingMode != null ? targetingMode : TargetingMode.FIRST;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public TowerType getTowerType() {
        return towerType;
    }

    public void setTowerType(TowerType towerType) {
        this.towerType = towerType;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public TargetingMode getTargetingMode() {
        return targetingMode;
    }

    public void setTargetingMode(TargetingMode targetingMode) {
        this.targetingMode = targetingMode;
    }
}

