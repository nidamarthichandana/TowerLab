# TowerLab – Game Rules and Mechanics (v2)

Every rule has an ID (GR-n) for the SRS and test traceability. Each item is tagged **[MVP]**, **[Stretch]** or **[Later]**.
All numbers are **starting values**. Tuning them with the balance analyzer is part of the project.

## 1. Units and Time

- **GR-1 [MVP]** Distances are in grid cells. Time is in **ticks**. The simulation advances in fixed steps of one tick.
- **GR-2 [MVP]** Speeds are in cells per tick. Cooldowns, durations and delays are in ticks.

## 2. Objective

- **GR-3 [MVP]** The player defends the base from successive waves of enemies.
- **GR-4 [MVP]** The player starts with **150 gold** and **20 base health**.
- **GR-5 [MVP]** The player wins by clearing all waves with base health above 0. The player loses when base health reaches 0.
- **GR-6 [MVP]** A simulation also ends as a LOSS if it reaches the **max tick limit (20,000)**.

## 3. Map

- **GR-7 [MVP]** The first version has one configurable grid map (12 × 8) with a start cell, a destination cell and a predefined path given as waypoints.
- **GR-8 [MVP]** Example path: (0,1) → (6,1) → (6,5) → (10,5) → (10,2) → (11,2). Length is 18 cells.
- **GR-9 [MVP]** Towers can only be placed on buildable cells: not on the path, one tower per cell, inside the grid.
- **GR-10 [MVP]** Paths come from a `PathFinder` interface. The MVP implementation reads the predefined waypoints.
- **GR-11 [Stretch]** An A* implementation computes the path once at map load for grids with obstacles. Flying enemies ignore the path and move in a straight line from start to destination.

## 4. Towers

Level 1 stats. Targeting column: G = ground, A = air.

| Tower | Tier | Cost | Range | Damage | Cooldown | Targets | Special |
|---|---|---|---|---|---|---|---|
| Cannon | MVP | 60 | 2.5 | 12 | 12 | G | Splash: enemies within 1.0 cells of the target take full damage |
| Sniper | MVP | 90 | 5.0 | 40 | 40 | G, A | None |
| Frost | MVP | 50 | 2.0 | 3 | 15 | G, A | Slow 50% for 30 ticks |
| Rapid | Stretch | 70 | 2.0 | 4 | 3 | G, A | None |

- **GR-12 [MVP]** A tower attacks only when its cooldown has elapsed and a valid target is in range.
- **GR-13 [MVP]** A tower can only select enemies it is able to target (ground, air or both).
- **GR-14 [MVP]** Each tower has a targeting strategy that the player can change at any time the player can act.
- **GR-15 [MVP]** Targeting strategies: **First** (furthest along the path), **Last** (least progress), **Strongest** (highest current health), **Closest** (nearest to the tower). Ties are broken by the lowest enemy id.

## 5. Enemies

| Enemy | Tier | Health | Speed | Armour | Reward | Base damage | Notes |
|---|---|---|---|---|---|---|---|
| Basic | MVP | 40 | 0.05 | 0% | 8 | 1 | Ground baseline |
| Fast | MVP | 25 | 0.10 | 0% | 7 | 1 | **20% evasion chance** |
| Armored | MVP | 120 | 0.035 | 40% | 20 | 2 | Ground |
| Flying | Stretch | 60 | 0.06 | 0% | 12 | 1 | Air; straight-line movement |
| Boss | Stretch | 800 | 0.03 | 30% | 150 | 10 | Immune to Freeze; slow effect halved |

- **GR-16 [MVP]** An enemy that reaches the destination is removed and reduces base health by its base damage.
- **GR-17 [MVP]** A defeated enemy gives its reward in gold.

## 6. Combat

- **GR-18 [MVP]** Final damage = `max(1, round(rawDamage × (1 − armour)))`. Burn damage ignores armour.
- **GR-19 [MVP]** When a tower attacks an enemy with an evasion chance, a seeded random roll decides whether the attack misses. A miss emits a `TowerMissed` event.
- **GR-20 [MVP]** Cannon splash applies to all valid enemies within its splash radius of the target, using the same damage formula per enemy.

## 7. Status Effects

All effects are implemented behind a `StatusEffect` abstraction with configurable duration and magnitude.

- **GR-21 [MVP] Slow:** Effects do not stack. A stronger incoming slow replaces a weaker active one. An equal slow refreshes the duration. A weaker incoming slow is ignored.
- **GR-22 [MVP] Burn:** Burn deals 1 damage at 5, 10, ..., 40 ticks after application (8 procs). Reapplying refreshes the remaining duration to 40 without resetting the interval or stacking.
- **GR-23 [Stretch] Freeze:** stops movement for a duration. Boss enemies are immune.

## 8. Upgrades and Selling

- **GR-24 [MVP]** Towers have one upgrade (level 2) in the MVP. Multi-level upgrades are [Stretch].
- **GR-25 [MVP]** Upgrade effects:

| Tower | Upgrade cost | Level 2 effect |
|---|---|---|
| Cannon | 40 | Damage +50%, range +0.5, applies Burn |
| Sniper | 60 | Damage +50% |
| Frost | 35 | Range +0.5, slow 60% |

- **GR-26 [MVP]** Selling refunds **70%** of the total gold spent on the tower (purchase plus upgrade).
- **GR-27 [MVP]** Insufficient gold rejects the action with `InsufficientFundsException`. Invalid placement is rejected with `InvalidPlacementException`.

## 9. Waves

- **GR-28 [MVP]** The game has **5 waves**. Each wave has spawn groups: `(enemyType, count, intervalTicks, startDelayTicks)`.

| Wave | Spawn groups |
|---|---|
| 1 | 8 Basic, interval 20 |
| 2 | 10 Basic, interval 15; 4 Fast, interval 25, delay 100 |
| 3 | 10 Basic, interval 15; 3 Armored, interval 60 |
| 4 | 12 Fast, interval 12; 6 Basic, interval 18, delay 60 |
| 5 | 6 Armored, interval 40; 10 Basic, interval 12 (Boss added when implemented) |

- **GR-29 [MVP]** Each spawn time gets a seeded random **jitter of ±3 ticks**.
- **GR-30 [MVP]** A wave is completed when all its enemies have spawned and none remain alive. Clearing a wave gives **20 gold**.
- **GR-31 [MVP]** After a wave, a **preparation period of 100 ticks** allows build, upgrade, sell and targeting changes. In headless simulations the next wave starts automatically when the period ends.
- **GR-32 [Later]** Generated waves and additional levels.

## 10. Game States and Player Actions

- **GR-33 [MVP]** States: READY, PLAYING, WAVE_COMPLETED, WON, LOST. PAUSED is [Stretch].
- **GR-34 [MVP]** Transitions:

| From | Event | To |
|---|---|---|
| READY | StartWave | PLAYING |
| PLAYING | Wave cleared (not last) | WAVE_COMPLETED |
| PLAYING | Last wave cleared | WON |
| PLAYING | Base health reaches 0 or max ticks | LOST |
| WAVE_COMPLETED | StartWave or prep period ends | PLAYING |
| PLAYING | Pause [Stretch] | PAUSED |
| PAUSED | Resume [Stretch] | PLAYING |

- **GR-35 [MVP]** Allowed actions by state:

| Action | READY | PLAYING | WAVE_COMPLETED | WON / LOST |
|---|---|---|---|---|
| Build / Upgrade / Sell | Yes | No | Yes | No |
| Change targeting | Yes | Yes | Yes | No |
| StartWave | Yes | No | Yes | No |

- **GR-36 [MVP]** Every player action is a **command object** (`BuildTower`, `UpgradeTower`, `SellTower`, `ChangeTargeting`, `StartWave`) tagged with the wave index it applies to. The same action list drives the playable game and batch simulations.
- **GR-37 [MVP]** Invalid actions are rejected according to the current state.

## 11. Tick Order

1. Apply scheduled player commands for this point in the game.
2. Spawn enemies that are due.
3. Update status effects.
4. Move enemies. An enemy reaching the destination is removed and damages the base.
5. Update towers in order of placement: select target, apply damage and effects, emit events.
6. Remove defeated enemies and award gold.
7. Check wave completion and win/loss conditions, update the state.

## 12. Events

- **GR-38 [MVP]** Events carry the tick number plus the data needed for replay and analytics.

| Event | Fields |
|---|---|
| WaveStarted | wave |
| EnemySpawned | enemyId, type, wave |
| EnemyMoved | enemyId, position |
| TowerFired | towerId, towerType, enemyId, damageDealt |
| TowerMissed | towerId, enemyId |
| EffectApplied | enemyId, effectType, magnitude, duration |
| EnemyKilled | enemyId, type, towerId, reward |
| LifeLost | enemyId, damage, healthLeft |
| ActionRejected | action, reason |
| GameEnded | outcome |

## 13. Simulation Engine

- **GR-39 [MVP]** The engine runs sessions without graphical rendering using the same mechanics as the playable game.
- **GR-40 [MVP]** One seeded random generator, injected into the simulation, is used for **spawn jitter and evasion rolls**. The order of use is fixed.
- **GR-41 [MVP]** The same seed, configuration and action list always produce an identical event list.
- **GR-42 [MVP]** Batch runs execute N simulations (different seeds) on a thread pool and collect results per run.
- **GR-43 [MVP]** Different configurations can be compared on the same metrics.

## 14. Analytics Metrics

- **GR-44 [MVP]** Win rate = wins ÷ runs.
- **GR-45 [MVP]** Averages per run: damage dealt, enemies defeated, enemy leakage (enemies reaching the destination), remaining base health, game duration in ticks.
- **GR-46 [MVP]** Tower damage contribution = damage by tower type ÷ total damage.
- **GR-47 [MVP]** Tower cost efficiency = total damage by tower type ÷ gold spent on that type (purchases and upgrades).
- **GR-48 [MVP]** Tower utilization = shots taken ÷ maximum possible shots, capped at 1.0. A shot is any tick in which the tower produced at least one TowerFired or TowerMissed event, so a Cannon blast that damages several enemies on one tick counts as one shot. Maximum possible shots = ceil(ticks on map ÷ cooldown ticks), where ticks on map runs from the tower's TowerBuilt tick to the end of the simulation. Across a batch of runs, utilization per tower type is pooled: total shots ÷ total maximum possible shots.

## 15. Balance Analysis (rule-based)

- **GR-49 [MVP]** *Overpowered tower:* cost efficiency above 1.5× the median across tower types.
- **GR-50 [MVP]** *Underused tower:* the pooled utilization of a tower type (GR-48) is below 20%.
- **GR-51 [MVP]** *Unbalanced configuration:* win rate above 90% or below 10%.
- **GR-52 [Stretch]** *Disproportionate enemy:* one enemy type causes over 50% of leakage damage.
- **GR-53 [MVP]** Each finding includes the rule, the metric value and a short suggested action.

## 16. Configuration and Validation

- **GR-54 [MVP]** Towers, enemies, waves, economy and the map are loaded from JSON files, separate from execution code.
- **GR-55 [MVP]** Invalid configurations are rejected with `InvalidConfigException`: negative or zero stats, waypoints outside the grid, non-connected path segments, unknown enemy or tower types, and waves with no spawn groups.

## 17. Technical Constraints

- **GR-56 [MVP]** The core engine is independent of the frontend and of Spring.
- **GR-57 [MVP]** The playable game and the simulator reuse the same mechanics classes.
- **GR-58 [MVP]** Results are reproducible using fixed seeds.
- **GR-59 [MVP]** Configuration is separate from execution.
- **GR-60 [MVP]** Player actions and configurations are validated.

## Changes from v1

| Change | Reason |
|---|---|
| Added Basic enemy | Baseline for comparison |
| Added evasion and spawn jitter | Gives the seed a real purpose and makes batch results a distribution |
| Removed Armour as a status effect | Armour is an enemy attribute with one formula |
| Burn from Cannon L2, Freeze as Stretch | Every effect now has a source |
| Player actions as commands, with an actions-by-state table | Same input drives game and simulation |
| A* moved to Stretch behind `PathFinder` | A fixed path doesn't need it |
| Events carry tower and damage data | Needed for analytics |
| Rapid, Flying, Boss, Freeze, Pause marked Stretch | Fits the schedule |
