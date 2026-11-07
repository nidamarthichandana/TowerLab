# TowerLab – Game Rules and Mechanics

## 1. Project Overview

TowerLab is a tower defense game combined with a simulation and balance analytics platform. Players defend a base against successive waves of enemies by strategically placing and upgrading towers.

The platform also allows developers to execute multiple headless simulations, compare configurations and evaluate game balance using statistical metrics.

## 2. Game Objective

The player's objective is to prevent enemies from reaching the destination point.

- Players begin with a fixed amount of health and coins.
- Towers are purchased and placed strategically.
- Enemies follow paths towards the destination.
- Towers automatically detect and attack enemies within their range.
- Players win by surviving all waves.
- Players lose when their base health reaches zero.

## 3. Game Environment

- The initial version contains one configurable grid-based map.
- The map contains a starting point, destination and predefined enemy path.
- Towers can only be placed on valid buildable cells.
- Enemies follow a path towards the destination.
- Pathfinding will use A* for configurable routes where applicable.

## 4. Tower Types

### 4.1 Cannon Tower
- Moderate damage.
- Medium attack speed.
- Area-of-effect damage.
- Effective against groups of enemies.

### 4.2 Sniper Tower
- High single-target damage.
- Long attack range.
- Low attack speed.
- Effective against high-health enemies.

### 4.3 Frost Tower
- Low direct damage.
- Applies a movement slow effect.
- Effective for controlling enemy movement.

### 4.4 Rapid Tower
- Low damage per attack.
- Very high attack speed.
- Effective against fast-moving enemies.

Tower attributes include:
- Damage
- Attack speed
- Range
- Purchase cost
- Upgrade level
- Special ability

## 5. Enemy Types

### 5.1 Fast Enemy
- Low health.
- High movement speed.
- Difficult to target.

### 5.2 Armored Enemy
- High health.
- Low movement speed.
- Reduces incoming damage.

### 5.3 Flying Enemy
- Moderate health.
- Medium movement speed.
- May bypass certain ground-only obstacles.
- Requires compatible tower targeting.

### 5.4 Boss Enemy
- Very high health.
- Special resistance.
- Appears in selected later waves.

Enemy attributes include:
- Health
- Movement speed
- Armour
- Reward
- Damage dealt to the base
- Special abilities

## 6. Combat Mechanics

- Towers automatically identify enemies within their attack range.
- Target selection depends on the tower's targeting strategy.
- Available targeting strategies include First, Last, Strongest and Closest.
- Towers attack according to their attack cooldown.
- Damage is calculated after applying enemy armour and applicable effects.
- Enemies are removed when their health reaches zero.
- Defeated enemies reward the player with coins.
- Enemies reaching the destination reduce base health.

## 7. Status Effects

The game supports special effects:

- Slow: Reduces enemy movement speed temporarily.
- Burn: Deals damage over time.
- Armour: Reduces incoming damage.
- Freeze: Temporarily prevents enemy movement.

Effects must have configurable duration and magnitude.

## 8. Tower Upgrades and Selling

- Towers can be upgraded using coins.
- Upgrades improve attributes such as damage, range or attack speed.
- Each upgrade has a cost.
- Towers can be sold for a partial refund.
- Upgrade levels and maximum upgrade limits are configurable.

## 9. Wave System

- The game consists of multiple successive waves.
- Each wave contains a predefined or generated enemy composition.
- Difficulty increases with wave progression.
- Later waves introduce stronger enemies and bosses.
- A short preparation period may be provided between waves.

## 10. Game States

The game supports the following states:

- READY
- PLAYING
- PAUSED
- WAVE_COMPLETED
- WON
- LOST

Invalid actions must be rejected according to the current game state.

## 11. Simulation Engine

The simulation engine executes game sessions without graphical rendering.

Features:

- Run multiple simulations for one configuration.
- Support deterministic random seeds for reproducibility.
- Reuse the same combat, movement and wave mechanics as the playable game.
- Collect results from every simulation.
- Compare different configurations.

## 12. Analytics Metrics

The platform calculates:

- Win rate.
- Average damage dealt.
- Average enemies defeated.
- Average enemy leakage.
- Average remaining base health.
- Tower damage contribution.
- Tower cost efficiency.
- Average game duration.

## 13. Balance Analysis

The analytics module evaluates game configurations and identifies potential balance issues.

Examples:

- Towers with unusually high damage-per-cost ratios.
- Towers with consistently low utilization.
- Configurations with extremely high or low win rates.
- Enemy types that cause disproportionate base damage.

Initial balance recommendations will be rule-based.

## 14. Winning and Losing Conditions

WIN:
The player survives all configured waves and no active enemies remain.

LOSS:
The player's base health reaches zero.

## 15. Technical Constraints

- The core game engine must be independent of the frontend.
- The playable game and simulation engine must reuse the same game mechanics.
- Simulation results must be reproducible using fixed random seeds.
- Game configuration must be separated from game execution.
- The system must validate player actions and game configurations.