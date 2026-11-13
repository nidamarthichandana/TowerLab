# TowerLab Game Rules & Mechanics Specification

## 1. Overview
**TowerLab** is a deterministic tower defense simulation engine and balance analysis platform. The engine is responsible for running tick-by-tick combat simulations given a player's strategic defense plan, verifying placement validity, advancing creeps along defined paths, applying tower targeting behaviors, calculating damage and status effects, and recording a chronological event log.

---

## 2. World & Grid Geometry
1. **Grid Dimensions:**
   - Width: 12 tiles (X: `0` to `11`)
   - Height: 8 tiles (Y: `0` to `7`)
   - Total Tiles: 96
2. **Tile Types:**
   - `EMPTY`: Buildable tile where player can place towers.
   - `PATH`: Road tile where creeps traverse. Towers **cannot** be placed on `PATH` tiles.
   - `SPAWNER`: Entrance tile where creeps enter the battlefield.
   - `BASE` (Exit): Defense objective where creeps exit. Reaching the base inflicts damage on the player's base health.
3. **Coordinates & Distance:**
   - Tile coordinates are integer coordinates `(x, y)` where `0 <= x <= 11` and `0 <= y <= 7`.
   - Creep positions during continuous tick movement are represented as floating-point `(double x, double y)`.
   - Distance calculation between tower `(tx, ty)` and creep `(cx, cy)` uses standard Euclidean metric:
     $$\text{distance} = \sqrt{(tx - cx)^2 + (ty - cy)^2}$$

---

## 3. Economy & Base Health
- **Starting Base Health:** Level dependent (typically `100` HP).
- **Starting Gold:** Level dependent (typically `250` - `500` Gold).
- **Gold Mechanics:**
  - Towers cost gold to construct. Placement fails immediately if the player lacks sufficient gold.
  - Creeps yield a gold reward upon death.
- **Victory Condition:** All waves defeated with `baseHealth > 0`.
- **Defeat Condition:** `baseHealth <= 0` at any point during simulation.

---

## 4. Tower Archetypes

All towers occupy exactly 1 tile and have specific combat roles:

| Tower Type | Build Cost | Range (tiles) | Attack Rate (ticks between shots) | Base Damage | Special Effects | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Cannon** | 100 Gold | 2.5 | 10 ticks (1.0 shot/s) | 45 | Physical impact, reliable sustained DPS | Medium range, heavy single-target armor punch. |
| **Sniper** | 150 Gold | 6.0 | 25 ticks (0.4 shot/s) | 140 | High critical burst, global coverage | Huge range, slow fire rate, massive single-target damage. |
| **Frost** | 125 Gold | 2.0 | 15 ticks (0.67 shot/s) | 15 | 40% Movement Slow for 20 ticks (non-stacking, refreshes) | Area crowd control, slows fast runners for other towers. |

---

## 5. Targeting Modes
When a tower is ready to attack and multiple creeps are within its Euclidean range, it selects a target according to its assigned targeting policy:

1. **`FIRST`**:
   - Targets the creep that is farthest along the waypoint path (highest path progress).
   - Priority: `pathProgress DESC`, then `spawnTick ASC`.
2. **`LAST`**:
   - Targets the creep that is least advanced along the waypoint path (closest to spawner).
   - Priority: `pathProgress ASC`.
3. **`STRONGEST`**:
   - Targets the creep within range that has the highest current health.
   - Priority: `currentHealth DESC`, then `pathProgress DESC`.
4. **`CLOSEST`**:
   - Targets the creep with the shortest Euclidean distance to the tower center.
   - Priority: `euclideanDistance ASC`, then `pathProgress DESC`.

---

## 6. Creep Types & Progression

Creeps travel along pre-defined waypoints at a base speed (units per tick).

| Creep Type | Base HP | Speed (tiles/tick) | Gold Bounty | Base Damage Penalty | Role |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Standard** | 80 | 0.10 | 15 | 10 | Balanced basic minion |
| **Runner** | 45 | 0.18 | 20 | 5 | Fast, low HP, vulnerable to Frost/Cannon |
| **Tank** | 260 | 0.05 | 40 | 25 | Slow, high health, vulnerable to Sniper |
| **Boss** | 750 | 0.04 | 100 | 50 | Heavy endgame threat |

### Movement & Waypoint Interpolation
- Path consists of an ordered sequence of discrete waypoints: $W_0, W_1, \dots, W_k$.
- In each tick $t$, creep advances along the current segment $W_i \rightarrow W_{i+1}$ by:
  $$\Delta d = \text{speed} \times (1.0 - \text{slowFactor})$$
- If distance to $W_{i+1}$ is exceeded, remainder is applied to segment $W_{i+1} \rightarrow W_{i+2}$.
- When reaching the final waypoint $W_k$ (the Base):
  - Base health is reduced by creep's `baseDamagePenalty`.
  - Creep is removed from active simulation.
  - Event `CREEP_LEAKED` is logged.

---

## 7. Simulation Tick Cycle (Order of Execution)

Every tick $T$ executes strictly in the following deterministic order:

1. **Player Action Execution:**
   - Process scheduled player actions for tick $T$ (e.g., place tower, change targeting mode).
   - Verify gold and tile availability. Log `TOWER_BUILT` or `ACTION_REJECTED`.
2. **Wave Spawning:**
   - Check if current wave has creeps scheduled to spawn at tick $T$.
   - Initialize creep at $W_0$, log `CREEP_SPAWNED`.
3. **Status Effect Updates:**
   - Decrement duration of slow/freeze debuffs on all active creeps.
   - Remove expired status effects and restore base speed.
4. **Creep Movement:**
   - Advance creeps along path waypoints based on current speed.
   - Detect creeps reaching exit: apply base damage, log `CREEP_LEAKED`, check defeat condition.
5. **Tower Acquisition & Combat:**
   - For each tower whose cooldown has reached 0:
     - Find all alive creeps within Euclidean range.
     - Select target based on tower's `TargetingMode`.
     - Fire at target: reset tower cooldown, log `TOWER_FIRED`.
     - Calculate damage and apply status effects (e.g., Frost slow).
     - Apply damage to creep: log `CREEP_DAMAGED`.
     - If creep health $\le 0$: award gold bounty, log `CREEP_KILLED`, remove creep.
6. **Wave Progression & End Conditions:**
   - If all creeps of active wave are dead or leaked, advance to next wave cooldown or trigger next wave.
   - If `baseHealth <= 0`: mark simulation `DEFEAT`, log `SIMULATION_COMPLETED`.
   - If all waves complete and 0 creeps active: mark simulation `VICTORY`, log `SIMULATION_COMPLETED`.
7. **Advance Tick Counter ($T \leftarrow T + 1$).**

---

## 8. Determinism Guarantees
- Random elements (such as critical strike chances or tie-breaking) must strictly use `java.util.Random` initialized with the input `seed`.
- When two entities tie for targeting priority (same progress/distance/HP), deterministic tie-breakers (e.g., ascending `creepId`) are enforced.
- Identical `(levelId, seed, actions)` will **always** yield the exact same event sequence and outcome.

