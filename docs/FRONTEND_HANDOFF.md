# TowerLab Frontend Handoff Specification

## 1. Executive Summary
This document provides full guidance for developing the TowerLab frontend application. The frontend is designed as a **Plan & Replay** system. The frontend does **not** simulate game physics or battle logic locally; it presents interactive planning tools, posts the plan to the backend Java simulation engine, and animates the resulting tick-by-tick event stream with scrubbing, play/pause controls, and balance visualizations.

---

## 2. Core Frontend Workflows

### 2.1 Plan Phase
- **Grid View:** Interactive $12 \times 8$ grid ($96$ tiles).
  - Path tiles are clearly visually designated with waypoint paths drawn connecting:
    - Entrance ($W_0$) to Base ($W_k$).
  - Buildable tiles allow the user to click and place towers:
    - **Cannon** (Cost: 100 Gold, Range: 2.5 tiles)
    - **Sniper** (Cost: 150 Gold, Range: 6.0 tiles)
    - **Frost** (Cost: 125 Gold, Range: 2.0 tiles)
  - Selecting a tower allows changing its **Targeting Mode**:
    - `FIRST`: Targets creep farthest along path.
    - `LAST`: Targets creep closest to entrance.
    - `STRONGEST`: Targets creep with highest HP.
    - `CLOSEST`: Targets creep closest to tower.
  - Hovering over a placed tower or palette item displays its Euclidean range circle on the grid.
  - Starting resources counter (e.g., Gold: 350, Base Health: 100) updates live as towers are placed.

### 2.2 Run Phase
- **Submission:**
  - When the user clicks **"Start Simulation"**, send:
    ```http
    POST /api/simulations/play
    Content-Type: application/json

    {
      "levelId": "level-1",
      "seed": 133742,
      "actions": [
        {"tick": 0, "type": "BUILD_TOWER", "towerType": "CANNON", "x": 3, "y": 2, "targetingMode": "FIRST"},
        {"tick": 0, "type": "BUILD_TOWER", "towerType": "FROST", "x": 5, "y": 2, "targetingMode": "CLOSEST"}
      ]
    }
    ```
  - Receives response payload:
    - `outcome`: `"VICTORY"` or `"DEFEAT"`
    - `baseHealth`: Remaining base health (e.g. `85`)
    - `gold`: Remaining gold
    - `ticks`: Total simulation ticks elapsed
    - `events`: Array of discrete chronological tick events.

### 2.3 Replay Phase
- **Interactive Controls:**
  - **Play / Pause** toggle.
  - **Playback Speed Selector:** `0.5x`, `1.0x`, `2.0x`, `5.0x`, `10.0x`.
  - **Timeline Scrubber:** Slider from tick `0` to `totalTicks`. Scrubbing instantly updates the visual state of all creeps, active towers, and HUD stats.
  - **Step Forward / Step Backward:** Inspect critical combat moments frame-by-frame.
- **HUD & Visual Elements:**
  - Base Health Bar (animated damage flash on `BASE_DAMAGED`).
  - Current Gold indicator.
  - Wave indicator (`WAVE_STARTED`).
  - Creeps rendered at `(creepX, creepY)` with health bars overhead and blue tint when `CREEP_SLOWED`.
  - Projectile / muzzle flash animations connecting `(towerX, towerY)` to target on `TOWER_FIRED`.
  - Event Log sidebar streaming tick events in real time with filtering options (Combat, Economy, Spawns).

### 2.4 Balance Analytics Phase
- Clicking **"Run Balance Analysis"** triggers:
  ```http
  POST /api/simulations/batch
  Content-Type: application/json

  {
    "levelId": "level-1",
    "runs": 100,
    "actions": [...]
  }
  ```
- **Analytics Visualizations:**
  - **Overall Summary Card:** Win Rate %, Total Wins vs Losses, Average Base Health, Average Ticks.
  - **Damage Share Pie / Bar Chart:** Percentage of total damage dealt by Cannon vs Sniper vs Frost.
  - **Cost Efficiency Chart:** Damage dealt per gold spent ($\frac{\text{Damage}}{\text{Cost}}$).
  - **Tower Utilization Gauge:** Percentage of active battle ticks each tower spent actively firing vs idle.

---

## 3. API Contract Quick Reference

| Endpoint | Method | Purpose |
| :--- | :--- | :--- |
| `/api/levels` | `GET` | List all available maps with grid size & waves |
| `/api/levels/{id}` | `GET` | Full map waypoint coordinates and wave specs |
| `/api/simulations/play` | `POST` | Execute single run, return ordered event log |
| `/api/simulations/batch` | `POST` | Execute Monte Carlo balance analysis |
| `/api/simulations/{id}` | `GET` | Fetch saved historical run |
| `/api/tower-configs` | `GET` | Fetch canonical stats for towers |

