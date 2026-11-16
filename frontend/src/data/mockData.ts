import type {
  BalanceReport,
  BalanceWarning,
  DashboardStats,
  EnemyStat,
  GameMap,
  GameSession,
  SimulationReport,
  Tower,
  TowerEfficiency,
  WinRateDataPoint,
} from '../types';

/**
 * TowerLab Mock Data Layer
 * --------------------------------------------------
 * All sample data lives here. Replace these with real
 * API calls once the backend is implemented.
 * See: src/services/api.ts for the service layer.
 */

export const mockDashboardStats: DashboardStats = {
  totalSimulations: 1_247,
  gamesPlayed: 384,
  avgWinRate: 67.4,
  bestTower: 'Cryo Sentinel',
};

export const mockRecentSessions: GameSession[] = [
  { id: 'gs-001', map: 'Crimson Canyon', waves: 20, result: 'victory', winRate: 82, duration: '14m 32s', timestamp: '2 hours ago' },
  { id: 'gs-002', map: 'Iron Bastion',   waves: 15, result: 'defeat',  winRate: 44, duration: '9m 18s',  timestamp: '5 hours ago' },
  { id: 'gs-003', map: 'Void Expanse',   waves: 25, result: 'victory', winRate: 71, duration: '21m 05s', timestamp: 'Yesterday' },
  { id: 'gs-004', map: 'Neon Labyrinth', waves: 18, result: 'victory', winRate: 93, duration: '17m 44s', timestamp: 'Yesterday' },
  { id: 'gs-005', map: 'Glacial Spire',  waves: 12, result: 'defeat',  winRate: 38, duration: '8m 11s',  timestamp: '2 days ago' },
];

export const mockBalanceReports: BalanceReport[] = [
  { id: 'br-001', title: 'Tower DPS Audit',         severity: 'warning', createdAt: '1 hour ago',  issues: 3 },
  { id: 'br-002', title: 'Wave Difficulty Spike',   severity: 'critical', createdAt: '3 hours ago', issues: 7 },
  { id: 'br-003', title: 'Economy Balance Review',  severity: 'info',    createdAt: 'Yesterday',    issues: 1 },
  { id: 'br-004', title: 'Enemy HP Calibration',    severity: 'success', createdAt: '2 days ago',   issues: 0 },
];

export const mockTowers: Tower[] = [
  {
    id: 'tower-001',
    name: 'Plasma Cannon',
    type: 'artillery',
    damage: 85,
    range: 4,
    attackSpeed: 1.2,
    cost: 150,
    description: 'High-damage area explosion tower. Ideal against clustered enemies.',
    color: '#2d8ef5',
    icon: '🔵',
    stats: { kills: 24_892, efficiency: 78, winContrib: 62 },
  },
  {
    id: 'tower-002',
    name: 'Cryo Sentinel',
    type: 'support',
    damage: 30,
    range: 5,
    attackSpeed: 2.0,
    cost: 200,
    description: 'Slows enemy movement. Best performing tower this season.',
    color: '#00d4ff',
    icon: '❄️',
    stats: { kills: 18_441, efficiency: 94, winContrib: 88 },
  },
  {
    id: 'tower-003',
    name: 'Volt Spire',
    type: 'chain',
    damage: 60,
    range: 3,
    attackSpeed: 1.8,
    cost: 120,
    description: 'Chain lightning hits up to 4 enemies. Great for dense waves.',
    color: '#7c3aed',
    icon: '⚡',
    stats: { kills: 31_204, efficiency: 85, winContrib: 71 },
  },
  {
    id: 'tower-004',
    name: 'Inferno Mortar',
    type: 'artillery',
    damage: 110,
    range: 6,
    attackSpeed: 0.6,
    cost: 275,
    description: 'Slow firing, massive AOE burn damage over time.',
    color: '#f59e0b',
    icon: '🔥',
    stats: { kills: 15_883, efficiency: 69, winContrib: 55 },
  },
  {
    id: 'tower-005',
    name: 'Nano Swarm',
    type: 'swarm',
    damage: 25,
    range: 3,
    attackSpeed: 5.0,
    cost: 90,
    description: 'Rapid-fire micro-projectiles. Low cost, high throughput.',
    color: '#10b981',
    icon: '🟢',
    stats: { kills: 42_117, efficiency: 72, winContrib: 60 },
  },
];

export const mockWinRateData: WinRateDataPoint[] = [
  { wave: 1,  winRate: 98, difficulty: 10 },
  { wave: 2,  winRate: 95, difficulty: 15 },
  { wave: 3,  winRate: 91, difficulty: 22 },
  { wave: 4,  winRate: 88, difficulty: 28 },
  { wave: 5,  winRate: 85, difficulty: 35 },
  { wave: 6,  winRate: 79, difficulty: 42 },
  { wave: 7,  winRate: 74, difficulty: 50 },
  { wave: 8,  winRate: 70, difficulty: 55 },
  { wave: 9,  winRate: 65, difficulty: 62 },
  { wave: 10, winRate: 60, difficulty: 70 },
  { wave: 11, winRate: 55, difficulty: 75 },
  { wave: 12, winRate: 51, difficulty: 80 },
  { wave: 13, winRate: 48, difficulty: 85 },
  { wave: 14, winRate: 44, difficulty: 88 },
  { wave: 15, winRate: 40, difficulty: 92 },
];

export const mockTowerEfficiency: TowerEfficiency[] = [
  { name: 'Cryo Sentinel', efficiency: 94, kills: 18441, cost: 200 },
  { name: 'Volt Spire',    efficiency: 85, kills: 31204, cost: 120 },
  { name: 'Plasma Cannon', efficiency: 78, kills: 24892, cost: 150 },
  { name: 'Nano Swarm',    efficiency: 72, kills: 42117, cost: 90  },
  { name: 'Inferno Mortar', efficiency: 69, kills: 15883, cost: 275 },
];

export const mockEnemyStats: EnemyStat[] = [
  { enemy: 'Grunt',      hp: 80,  speed: 2.5, spawnRate: 'High',   threat: 'low'      },
  { enemy: 'Brute',      hp: 400, speed: 1.0, spawnRate: 'Medium', threat: 'medium'   },
  { enemy: 'Speeder',    hp: 50,  speed: 6.0, spawnRate: 'High',   threat: 'medium'   },
  { enemy: 'Tank',       hp: 1200, speed: 0.5, spawnRate: 'Low',   threat: 'high'     },
  { enemy: 'Hivemind',   hp: 300, speed: 1.8, spawnRate: 'Medium', threat: 'high'     },
  { enemy: 'Void Reaper', hp: 2500, speed: 0.8, spawnRate: 'Rare', threat: 'critical' },
];

export const mockBalanceWarnings: BalanceWarning[] = [
  { id: 'bw-001', type: 'critical', title: 'Wave 14 Difficulty Spike', description: 'Win rate drops 11% between wave 13 and 14. Enemy scaling too aggressive.', tower: null },
  { id: 'bw-002', type: 'warning',  title: 'Inferno Mortar Underperforming', description: 'Tower cost/effectiveness ratio is 24% below baseline. Consider reducing cost or increasing AOE.', tower: 'Inferno Mortar' },
  { id: 'bw-003', type: 'warning',  title: 'Cryo Sentinel Dominance', description: 'Tower appears in 88% of winning configurations. May need minor nerf to slow effect.', tower: 'Cryo Sentinel' },
  { id: 'bw-004', type: 'info',     title: 'Nano Swarm Kill Inflation', description: 'High kill count due to swarm mechanic counting individual nano-bots as kills.', tower: 'Nano Swarm' },
];

export const mockSimulationReports: SimulationReport[] = [
  {
    id: 'sim-001',
    title: 'Standard Loadout — Crimson Canyon',
    date: '2026-11-09',
    runs: 500,
    winRate: 67.2,
    avgWaves: 14.8,
    topTower: 'Cryo Sentinel',
    status: 'complete',
  },
  {
    id: 'sim-002',
    title: 'No Cryo Test — Iron Bastion',
    date: '2026-11-08',
    runs: 300,
    winRate: 42.1,
    avgWaves: 10.3,
    topTower: 'Volt Spire',
    status: 'complete',
  },
  {
    id: 'sim-003',
    title: 'High Difficulty Stress Test',
    date: '2026-11-07',
    runs: 1000,
    winRate: 28.9,
    avgWaves: 8.6,
    topTower: 'Plasma Cannon',
    status: 'complete',
  },
  {
    id: 'sim-004',
    title: 'Economy Optimization Run',
    date: '2026-11-10',
    runs: 200,
    winRate: 0,
    avgWaves: 0,
    topTower: '—',
    status: 'running',
  },
];

export const mockMaps: GameMap[] = [
  { id: 'map-001', name: 'Crimson Canyon',  gridW: 20, gridH: 14, pathLength: 48 },
  { id: 'map-002', name: 'Iron Bastion',    gridW: 18, gridH: 12, pathLength: 36 },
  { id: 'map-003', name: 'Void Expanse',    gridW: 24, gridH: 16, pathLength: 64 },
  { id: 'map-004', name: 'Neon Labyrinth',  gridW: 22, gridH: 15, pathLength: 72 },
  { id: 'map-005', name: 'Glacial Spire',   gridW: 16, gridH: 12, pathLength: 32 },
];
