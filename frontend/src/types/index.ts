/**
 * TowerLab — Shared TypeScript Types
 * Field names match the Java engine exactly (handoff spec §4–§7).
 * Do NOT invent new fields — ask the engine owner.
 */

// ─── Enumerations ────────────────────────────────────────────────────────────

export type TowerType = 'Cannon' | 'Sniper' | 'Frost';
export type EnemyType = 'Basic' | 'Fast' | 'Armored';
export type TargetingMode = 'FIRST' | 'LAST' | 'STRONGEST' | 'CLOSEST';
export type Outcome = 'WIN' | 'LOSS';

// ─── Catalog types (from GET /api/catalog) ───────────────────────────────────

export interface TowerCatalogEntry {
  type: TowerType;
  cost: number;
  range: number;       // in grid cells
  damage: number;
  cooldown: number;    // in ticks
  targets: string;     // e.g. "ground", "all"
  special: string | null;
  upgradeCost: number | null;
}

export interface EnemyCatalogEntry {
  type: EnemyType;
  health: number;
  speed: number;       // cells per tick
  armour: number;      // fraction 0–1 (e.g. 0.4 = 40%)
  reward: number;      // gold
  baseDamage: number;  // damage to base per leak
}

export interface CatalogResult {
  towers: TowerCatalogEntry[];
  enemies: EnemyCatalogEntry[];
}

// ─── Level config (from GET /api/levels/:id) ─────────────────────────────────

export interface PathCell {
  x: number;
  y: number;
}

export interface LevelConfig {
  id: string;
  gridWidth: number;
  gridHeight: number;
  path: PathCell[];
  startGold: number;
  startBaseHealth: number;
  waveCount: number;
}

// ─── Actions ─────────────────────────────────────────────────────────────────

export interface BuildAction {
  type: 'BUILD_TOWER';
  wave: number;          // 0 = before wave 1; k = prep after wave k
  towerType: TowerType;
  x: number;
  y: number;
  targetingMode: TargetingMode;
}

export type GameAction = BuildAction;

// ─── API requests ─────────────────────────────────────────────────────────────

export interface SimulationRequest {
  levelId: string;
  seed: number;
  actions: GameAction[];
}

export interface BatchRequest extends SimulationRequest {
  runs: number; // 1–1000
}

// ─── Events ───────────────────────────────────────────────────────────────────
// Every event has `type` and `tick`. Additional fields per type below.

interface BaseEvent {
  tick: number;
}

export interface WaveStartedEvent extends BaseEvent {
  type: 'WaveStarted';
  wave: number;
}

export interface EnemySpawnedEvent extends BaseEvent {
  type: 'EnemySpawned';
  enemyId: number;
  enemyType: EnemyType;
  wave: number;
}

export interface EnemyMovedEvent extends BaseEvent {
  type: 'EnemyMoved';
  enemyId: number;
  x: number;
  y: number;
}

export interface TowerBuiltEvent extends BaseEvent {
  type: 'TowerBuilt';
  towerId: number;
  towerType: TowerType;
  x: number;
  y: number;
  goldAfter: number;
}

export interface TowerFiredEvent extends BaseEvent {
  type: 'TowerFired';
  towerId: number;
  towerType: TowerType;
  enemyId: number;
  damageDealt: number;
}

export interface TowerMissedEvent extends BaseEvent {
  type: 'TowerMissed';
  towerId: number;
  enemyId: number;
}

export interface EffectAppliedEvent extends BaseEvent {
  type: 'EffectApplied';
  enemyId: number;
  effectType: string;    // e.g. "Slow"
  magnitude: number;
  durationTicks: number;
}

export interface EnemyKilledEvent extends BaseEvent {
  type: 'EnemyKilled';
  enemyId: number;
  enemyType: EnemyType;
  towerId: number;
  reward: number;
  goldAfter: number;
}

export interface LifeLostEvent extends BaseEvent {
  type: 'LifeLost';
  enemyId: number;
  damage: number;
  baseHealthAfter: number;
}

export interface WaveCompletedEvent extends BaseEvent {
  type: 'WaveCompleted';
  wave: number;
  bonusGold: number;
  goldAfter: number;
}

export interface ActionRejectedEvent extends BaseEvent {
  type: 'ActionRejected';
  /** Note: engine sends "BuildTower", NOT "BUILD_TOWER" */
  actionType: string;
  reason: string;
}

export interface GameEndedEvent extends BaseEvent {
  type: 'GameEnded';
  outcome: Outcome;
}

export type GameEvent =
  | WaveStartedEvent
  | EnemySpawnedEvent
  | EnemyMovedEvent
  | TowerBuiltEvent
  | TowerFiredEvent
  | TowerMissedEvent
  | EffectAppliedEvent
  | EnemyKilledEvent
  | LifeLostEvent
  | WaveCompletedEvent
  | ActionRejectedEvent
  | GameEndedEvent;

// ─── Simulation result (POST /api/simulations/play) ──────────────────────────

export interface SimulationResult {
  outcome: Outcome;
  baseHealth: number;
  gold: number;
  ticks: number;
  events: GameEvent[];
}

// ─── Batch result (POST /api/simulations/batch) ───────────────────────────────

export interface Finding {
  rule: string;
  metricValue: number;
  suggestedAction: string;
}

export interface TowerStat {
  towerType: TowerType;
  /** fraction 0–1 */
  damageContribution: number;
  /** damage per gold spent */
  costEfficiency: number;
  /** fraction 0–1 */
  utilization: number;
}

export interface BatchAverages {
  damageDealt: number;
  enemiesDefeated: number;
  leakage: number;
  baseHealthRemaining: number;
  durationTicks: number;
}

export interface BatchResult {
  runs: number;
  /** fraction 0–1 */
  winRate: number;
  averages: BatchAverages;
  /** Only tower types that were built, sorted by name */
  towerStats: TowerStat[];
  findings: Finding[];
}

// ─── UI-only types (not from engine) ─────────────────────────────────────────

export type SeverityLevel = 'info' | 'warning' | 'critical' | 'success';
export type SimStatus = 'running' | 'complete' | 'failed';

/** A tower the player has placed during the plan phase (pre-simulation). */
export interface PlacedTower {
  towerType: TowerType;
  x: number;
  y: number;
  targetingMode: TargetingMode;
  wave: number;
}

/** Live tower state during replay (from TowerBuilt events). */
export interface ReplayTower {
  towerId: number;
  towerType: TowerType;
  x: number;
  y: number;
}

/** Live enemy state during replay (from EnemyMoved events). */
export interface ReplayEnemy {
  enemyId: number;
  enemyType: EnemyType;
  x: number;
  y: number;
  /** Catalog max HP minus cumulative damageDealt for this enemy. */
  healthRemaining: number;
  /** Whether the Slow effect is currently active (cosmetic). */
  isSlowed: boolean;
  slowUntilTick: number;
}

/** A recent attack flash shown on the grid (cleared after ~3 ticks). */
export interface RecentHit {
  towerId: number;
  enemyId: number;
  tick: number;
  missed: boolean;
}

/** Snapshot returned by frameAt(). */
export interface FrameState {
  towers: ReplayTower[];
  enemies: ReplayEnemy[];
  gold: number;
  baseHealth: number;
  wave: number;
  recentHits: RecentHit[];
  rejectedActions: ActionRejectedEvent[];
}

// ─── Legacy UI types (Dashboard, Reports pages) ───────────────────────────────
// These are display-only and not connected to the engine spec.

export type SessionResult = 'victory' | 'defeat';
export type ThreatLevel = 'low' | 'medium' | 'high' | 'critical';

export interface DashboardStats {
  totalSimulations: number;
  gamesPlayed: number;
  avgWinRate: number;
  bestTower: string;
}

export interface GameSession {
  id: string;
  map: string;
  waves: number;
  result: SessionResult;
  winRate: number;
  duration: string;
  timestamp: string;
}

export interface BalanceReport {
  id: string;
  title: string;
  severity: SeverityLevel;
  createdAt: string;
  issues: number;
}

export interface BalanceWarning {
  id: string;
  type: ThreatLevel | 'warning' | 'info';
  title: string;
  description: string;
  tower: string | null;
}

export interface WinRateDataPoint {
  wave: number;
  winRate: number;
  difficulty: number;
}

export interface TowerEfficiency {
  name: string;
  efficiency: number;
  kills: number;
  cost: number;
}

export interface EnemyStat {
  enemy: string;
  hp: number;
  speed: number;
  spawnRate: string;
  threat: ThreatLevel;
}

export interface SimulationReport {
  id: string;
  title: string;
  date: string;
  runs: number;
  winRate: number;
  avgWaves: number;
  topTower: string;
  status: SimStatus;
}

export interface GameMap {
  id: string;
  name: string;
  gridW: number;
  gridH: number;
  pathLength: number;
}

export interface Tower {
  id: string;
  name: string;
  type: string;
  damage: number;
  range: number;
  attackSpeed: number;
  cost: number;
  description: string;
  color: string;
  icon: string;
  stats: { kills: number; efficiency: number; winContrib: number };
}

