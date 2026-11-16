/**
 * src/services/api.ts — COMPATIBILITY SHIM
 *
 * Keeps Dashboard and Reports working with their existing call patterns.
 * All new code (Battlefield, BalanceAnalyzer) uses src/api/ instead.
 * Remove once Dashboard/Reports are migrated to src/api/.
 */

import {
  mockDashboardStats,
  mockRecentSessions,
  mockBalanceReports,
  mockTowers,
  mockWinRateData,
  mockTowerEfficiency,
  mockEnemyStats,
  mockBalanceWarnings,
  mockSimulationReports,
  mockMaps,
} from '../data/mockData';

import type {
  DashboardStats,
  GameSession,
  BalanceReport,
  Tower,
  WinRateDataPoint,
  TowerEfficiency,
  EnemyStat,
  BalanceWarning,
  SimulationReport,
  GameMap,
} from '../types';

const delay = (ms = 200) => new Promise<void>(r => setTimeout(r, ms));

export const api = {
  async getDashboardStats(): Promise<DashboardStats> {
    await delay();
    return mockDashboardStats;
  },
  async getRecentSessions(): Promise<GameSession[]> {
    await delay();
    return mockRecentSessions;
  },
  async getBalanceReports(): Promise<BalanceReport[]> {
    await delay();
    return mockBalanceReports;
  },
  async getTowers(): Promise<Tower[]> {
    await delay();
    return mockTowers;
  },
  async getWinRateData(): Promise<WinRateDataPoint[]> {
    await delay();
    return mockWinRateData;
  },
  async getTowerEfficiency(): Promise<TowerEfficiency[]> {
    await delay();
    return mockTowerEfficiency;
  },
  async getEnemyStats(): Promise<EnemyStat[]> {
    await delay();
    return mockEnemyStats;
  },
  async getBalanceWarnings(): Promise<BalanceWarning[]> {
    await delay();
    return mockBalanceWarnings;
  },
  async getSimulationReports(): Promise<SimulationReport[]> {
    await delay();
    return mockSimulationReports;
  },
  async getMaps(): Promise<GameMap[]> {
    await delay();
    return mockMaps;
  },
};
