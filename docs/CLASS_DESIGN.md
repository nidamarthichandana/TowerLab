# TowerLab – Object-Oriented Class Design

## 1. Architecture Overview

TowerLab follows a layered, modular architecture.

The core game engine is implemented in plain Java and remains independent of Spring Boot, the database and frontend.

Both interactive gameplay and headless simulation reuse the same game engine.

### Major Layers

1. Presentation Layer – React frontend.
2. Controller Layer – Spring Boot REST controllers.
3. Service Layer – application services and orchestration.
4. Game Engine Layer – game mechanics and execution.
5. Simulation Layer – repeated headless game execution.
6. Analytics Layer – performance metrics and balance analysis.
7. Persistence Layer – MySQL database and repositories.

---

## 2. Core Model Classes

### 2.1 Tower (Abstract Class)

Represents the common properties and behaviour of all towers.

Attributes:
- id
- position
- damage
- attackRange
- attackSpeed
- purchaseCost
- upgradeLevel
- attackCooldown

Methods:
- attack(Enemy enemy)
- canAttack(Enemy enemy)
- upgrade()
- calculateDamage(Enemy enemy)
- selectTarget(List<Enemy> enemies)

Concrete subclasses:

**CannonTower**
- Deals area-of-effect damage.
- Effective against groups of enemies.

**SniperTower**
- Deals high single-target damage.
- Has long attack range.

**FrostTower**
- Deals low damage.
- Applies slow effects.

**RapidTower**
- Deals low damage per attack.
- Has a high attack speed.

---

### 2.2 Enemy (Abstract Class)

Represents the common properties and behaviour of enemies.

Attributes:
- id
- position
- health
- maxHealth
- movementSpeed
- armour
- reward
- baseDamage
- currentPathIndex
- statusEffects

Methods:
- move()
- takeDamage(double damage)
- applyStatusEffect(StatusEffect effect)
- isAlive()
- reachDestination()

Concrete subclasses:

**FastEnemy**
- High movement speed.
- Low health.

**ArmoredEnemy**
- High health.
- Damage reduction.

**FlyingEnemy**
- Can bypass certain ground obstacles.
- Requires compatible towers.

**BossEnemy**
- High health.
- Special resistance.

---

## 3. Supporting Model Classes

### Position
Represents coordinates on the game grid.

Attributes:
- x
- y

Methods:
- distanceTo(Position other)

### GameMap
Represents the game environment.

Attributes:
- width
- height
- grid
- startPosition
- destination
- path

Methods:
- isBuildable(Position position)
- isValidPosition(Position position)
- findPath(Position start, Position destination)

### Wave
Represents one enemy wave.

Attributes:
- waveNumber
- enemySpawns
- spawnInterval
- completionStatus

Methods:
- spawnNextEnemy()
- isCompleted()

### GameConfiguration
Stores configurable game parameters.

Attributes:
- mapConfiguration
- towerConfigurations
- enemyConfigurations
- waveConfigurations
- initialCoins
- initialLives

### GameSession
Represents one playable game session.

Attributes:
- sessionId
- gameState
- gameMap
- towers
- activeEnemies
- currentWave
- playerCoins
- playerLives
- randomSeed

Methods:
- placeTower()
- upgradeTower()
- sellTower()
- startNextWave()
- pauseGame()
- resumeGame()

### GameResult
Stores the final result of a game.

Attributes:
- won
- wavesCompleted
- enemiesKilled
- enemiesLeaked
- totalDamage
- remainingLives
- duration

---

## 4. Interfaces

### Attackable

Defines entities capable of attacking.

Method:
- attack(Enemy enemy)

### Upgradable

Defines entities that support upgrades.

Methods:
- upgrade()
- getUpgradeCost()

### TargetingStrategy

Defines how a tower selects an enemy.

Method:
- selectTarget(List<Enemy> enemies, Tower tower)

Implementations:

- FirstTargetStrategy
- LastTargetStrategy
- StrongestTargetStrategy
- ClosestTargetStrategy

### PathfindingStrategy

Defines the algorithm used to calculate enemy paths.

Method:
- findPath(GameMap map, Position start, Position destination)

Implementation:
- AStarPathfindingStrategy

---

## 5. Design Patterns

### 5.1 Strategy Pattern

Purpose: Allow towers to use different targeting algorithms without changing tower classes.

Classes:
- TargetingStrategy
- FirstTargetStrategy
- LastTargetStrategy
- StrongestTargetStrategy
- ClosestTargetStrategy

### 5.2 Factory Pattern

Purpose: Centralise creation of towers and enemies.

Classes:
- TowerFactory
- EnemyFactory

Methods:
- createTower()
- createEnemy()

### 5.3 Observer Pattern

Purpose: Notify interested components when important game events occur.

Classes:
- GameEventListener
- GameEventPublisher

Example events:
- EnemyKilledEvent
- EnemyReachedDestinationEvent
- WaveCompletedEvent
- GameOverEvent

Observers can update score, analytics and game progression.

### 5.4 State Pattern

Purpose: Represent different states of a game session.

States:
- ReadyState
- PlayingState
- PausedState
- WonState
- LostState

Each state controls which actions are permitted.

### 5.5 Command Pattern

Purpose: Encapsulate player actions as independent command objects.

Commands:
- PlaceTowerCommand
- UpgradeTowerCommand
- SellTowerCommand

Each command supports execution and validation. Undo can be added where appropriate.

---

## 6. Game Engine Classes

### GameEngine

Responsible for executing game mechanics.

Responsibilities:
- Advance game simulation by time steps.
- Update enemy movement.
- Update tower attacks.
- Process status effects.
- Handle enemy deaths and leakage.
- Detect wave completion.
- Update game state.

Methods:
- initialize(GameConfiguration configuration)
- tick(double deltaTime)
- getGameState()
- getGameResult()

### WaveManager

Responsibilities:
- Manage wave progression.
- Schedule enemy spawning.
- Track active enemies.
- Detect wave completion.

### CombatResolver

Responsibilities:
- Calculate damage.
- Apply armour and resistance.
- Resolve area damage.
- Process status effects.

### GameEventPublisher

Publishes important game events to registered listeners.

---

## 7. Simulation Architecture

### SimulationEngine

Executes multiple game sessions without rendering.

Responsibilities:
- Accept game configurations.
- Generate reproducible random seeds.
- Create game sessions.
- Execute games using the same GameEngine.
- Collect results.

Method:
- runSimulation(GameConfiguration configuration, int runs)

### SimulationRunner

Executes one individual simulation run.

### SimulationResult

Stores:
- runId
- randomSeed
- won
- damageDealt
- enemiesKilled
- enemiesLeaked
- remainingLives
- duration

### SimulationBatchResult

Stores aggregated results from multiple runs.

Attributes:
- totalRuns
- winRate
- averageDamage
- averageEnemiesKilled
- averageEnemyLeakage
- averageRemainingLives
- averageDuration

---

## 8. Analytics Classes

### AnalyticsEngine

Calculates performance metrics from simulation results.

### TowerPerformanceAnalyzer

Evaluates tower performance using:
- Total damage contribution.
- Damage per unit cost.
- Kill contribution.
- Utilization.

### BalanceAnalyzer

Identifies possible balance problems.

Examples:
- Extremely high tower efficiency.
- Very low tower utilization.
- Unusually high game difficulty.
- Excessively easy configurations.

### BalanceRecommendation

Stores a recommendation, severity, explanation and suggested adjustment.

---

## 9. Application and Persistence Classes

### GameController
Handles game-related REST requests.

### SimulationController
Handles simulation requests.

### AnalyticsController
Provides analytics information.

### GameService
Coordinates playable game operations.

### SimulationService
Coordinates simulation execution.

### AnalyticsService
Provides analytics and balance reports.

### Entities and Repositories

Persistence entities:
- GameSessionEntity
- SimulationBatchEntity
- SimulationRunEntity
- GameConfigurationEntity

Spring Data repositories provide database access.

The domain model should remain separate from persistence entities.

---

## 10. Object-Oriented Relationships

### Inheritance
- CannonTower, SniperTower, FrostTower and RapidTower inherit Tower.
- FastEnemy, ArmoredEnemy, FlyingEnemy and BossEnemy inherit Enemy.

### Abstraction
- Tower and Enemy define common contracts while hiding implementation details.

### Encapsulation
- Attributes remain private.
- State changes occur through validated methods.

### Polymorphism
- Different tower subclasses implement different attack behaviours.
- Different enemy subclasses implement specialised abilities.
- Targeting strategies can be exchanged at runtime.

### Association
- Towers interact with enemies.
- GameEngine coordinates GameSession and WaveManager.

### Composition
- GameSession owns its session-specific game state.
- SimulationBatchResult contains individual simulation results.

---

## 11. Important Architectural Principle

The interactive game and the headless simulator must use the same GameEngine.

The simulator must not contain a separate implementation of:
- Enemy movement.
- Tower targeting.
- Combat resolution.
- Damage calculation.
- Wave progression.

The only major difference is that interactive gameplay receives player actions and renders state, whereas headless simulation executes automatically without rendering.

This ensures consistent game behaviour and reliable balance analysis.

---

## 12. Testing Strategy

Unit tests:
- Tower attack calculations.
- Enemy damage and armour.
- Targeting strategies.
- Upgrade validation.
- Pathfinding.
- Wave progression.

Integration tests:
- Complete game execution.
- Game state transitions.
- Simulation execution.
- Analytics aggregation.

Reproducibility tests:
- Identical configurations and random seeds should produce identical simulation results.

Performance tests:
- Measure execution time for batches of simulations.
- Verify that headless execution does not depend on frontend rendering.