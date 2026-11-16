import { describe, it, expect } from 'vitest';
import { frameAt } from './frameAt';
import type { GameEvent, EnemyCatalogEntry } from '../types';

// Minimal catalog used in every test
const catalog: EnemyCatalogEntry[] = [
  { type: 'Basic',   health: 40,  speed: 0.05,  armour: 0,   reward: 8,  baseDamage: 1 },
  { type: 'Fast',    health: 25,  speed: 0.10,  armour: 0,   reward: 7,  baseDamage: 1 },
  { type: 'Armored', health: 120, speed: 0.035, armour: 0.4, reward: 20, baseDamage: 2 },
];
const START_GOLD   = 150;
const START_HEALTH = 20;

function frame(events: GameEvent[], tick: number) {
  return frameAt(events, tick, catalog, START_GOLD, START_HEALTH);
}

// ─── Empty events ─────────────────────────────────────────────────────────────

describe('empty event list', () => {
  it('returns start state', () => {
    const f = frame([], 0);
    expect(f.towers).toHaveLength(0);
    expect(f.enemies).toHaveLength(0);
    expect(f.gold).toBe(START_GOLD);
    expect(f.baseHealth).toBe(START_HEALTH);
    expect(f.wave).toBe(0);
    expect(f.recentHits).toHaveLength(0);
  });
});

// ─── TowerBuilt ───────────────────────────────────────────────────────────────

describe('TowerBuilt', () => {
  const events: GameEvent[] = [
    { type: 'TowerBuilt', tick: 0, towerId: 0, towerType: 'Cannon', x: 1, y: 2, goldAfter: 90 },
  ];

  it('tower appears at tick 0', () => {
    const f = frame(events, 0);
    expect(f.towers).toHaveLength(1);
    expect(f.towers[0]).toMatchObject({ towerId: 0, towerType: 'Cannon', x: 1, y: 2 });
  });

  it('gold updates to goldAfter', () => {
    expect(frame(events, 0).gold).toBe(90);
  });

  it('tower does NOT appear before its tick', () => {
    const future: GameEvent[] = [
      { type: 'TowerBuilt', tick: 5, towerId: 0, towerType: 'Cannon', x: 1, y: 2, goldAfter: 90 },
    ];
    expect(frame(future, 4).towers).toHaveLength(0);
    expect(frame(future, 5).towers).toHaveLength(1);
  });
});

// ─── EnemySpawned + EnemyMoved ───────────────────────────────────────────────

describe('enemy tracking', () => {
  const events: GameEvent[] = [
    { type: 'WaveStarted', tick: 0, wave: 1 },
    { type: 'EnemySpawned', tick: 1, enemyId: 0, enemyType: 'Basic', wave: 1 },
    { type: 'EnemyMoved',   tick: 1, enemyId: 0, x: 0.05, y: 1.0 },
    { type: 'EnemyMoved',   tick: 2, enemyId: 0, x: 0.10, y: 1.0 },
  ];

  it('enemy appears on spawn tick', () => {
    expect(frame(events, 1).enemies).toHaveLength(1);
  });

  it('enemy position is latest EnemyMoved', () => {
    const f = frame(events, 2);
    expect(f.enemies[0]).toMatchObject({ x: 0.10, y: 1.0 });
  });

  it('health starts at catalog max', () => {
    expect(frame(events, 1).enemies[0].healthRemaining).toBe(40);
  });

  it('wave is tracked', () => {
    expect(frame(events, 1).wave).toBe(1);
  });
});

// ─── EnemyKilled removes enemy ───────────────────────────────────────────────

describe('EnemyKilled', () => {
  const events: GameEvent[] = [
    { type: 'EnemySpawned', tick: 0, enemyId: 1, enemyType: 'Basic', wave: 1 },
    { type: 'EnemyMoved',   tick: 0, enemyId: 1, x: 1.0, y: 1.0 },
    { type: 'TowerFired',   tick: 1, towerId: 0, towerType: 'Cannon', enemyId: 1, damageDealt: 40 },
    { type: 'EnemyKilled',  tick: 1, enemyId: 1, enemyType: 'Basic', towerId: 0, reward: 8, goldAfter: 158 },
  ];

  it('enemy removed at EnemyKilled', () => {
    expect(frame(events, 1).enemies).toHaveLength(0);
  });

  it('gold updated to goldAfter on kill', () => {
    expect(frame(events, 1).gold).toBe(158);
  });
});

// ─── LifeLost removes enemy and decrements baseHealth ────────────────────────

describe('LifeLost', () => {
  const events: GameEvent[] = [
    { type: 'EnemySpawned', tick: 0, enemyId: 2, enemyType: 'Basic', wave: 1 },
    { type: 'EnemyMoved',   tick: 5, enemyId: 2, x: 11.0, y: 2.0 },
    { type: 'LifeLost',     tick: 5, enemyId: 2, damage: 1, baseHealthAfter: 19 },
  ];

  it('enemy removed on LifeLost', () => {
    expect(frame(events, 5).enemies).toHaveLength(0);
  });

  it('baseHealth updated to baseHealthAfter', () => {
    expect(frame(events, 5).baseHealth).toBe(19);
  });
});

// ─── Health bar computation ───────────────────────────────────────────────────

describe('health bar', () => {
  const events: GameEvent[] = [
    { type: 'EnemySpawned', tick: 0, enemyId: 3, enemyType: 'Armored', wave: 2 },
    { type: 'EnemyMoved',   tick: 0, enemyId: 3, x: 0, y: 1 },
    { type: 'TowerFired',   tick: 1, towerId: 0, towerType: 'Cannon', enemyId: 3, damageDealt: 12 },
    { type: 'TowerFired',   tick: 2, towerId: 0, towerType: 'Cannon', enemyId: 3, damageDealt: 12 },
  ];

  it('healthRemaining = catalogHP − cumulative damage', () => {
    const f = frame(events, 2);
    // Armored HP = 120; 2 hits of 12 = 24 dealt
    expect(f.enemies[0].healthRemaining).toBe(120 - 24);
  });
});

// ─── Slow effect (cosmetic) ───────────────────────────────────────────────────

describe('slow effect', () => {
  const events: GameEvent[] = [
    { type: 'EnemySpawned', tick: 0, enemyId: 4, enemyType: 'Fast', wave: 1 },
    { type: 'EnemyMoved',   tick: 0, enemyId: 4, x: 0, y: 1 },
    { type: 'TowerFired',   tick: 1, towerId: 1, towerType: 'Frost', enemyId: 4, damageDealt: 3 },
    { type: 'EffectApplied',tick: 1, enemyId: 4, effectType: 'Slow', magnitude: 0.5, durationTicks: 30 },
  ];

  it('enemy is slowed after EffectApplied', () => {
    expect(frame(events, 1).enemies[0].isSlowed).toBe(true);
  });

  it('slow expires after durationTicks', () => {
    expect(frame(events, 31).enemies[0].isSlowed).toBe(false);
  });
});

// ─── recentHits ───────────────────────────────────────────────────────────────

describe('recentHits', () => {
  const events: GameEvent[] = [
    { type: 'EnemySpawned', tick: 0, enemyId: 5, enemyType: 'Basic', wave: 1 },
    { type: 'TowerFired',   tick: 10, towerId: 0, towerType: 'Cannon', enemyId: 5, damageDealt: 12 },
    { type: 'TowerMissed',  tick: 11, towerId: 0, enemyId: 5 },
  ];

  it('TowerFired appears in recentHits within 3 ticks', () => {
    const f = frame(events, 10);
    expect(f.recentHits.some(h => h.towerId === 0 && !h.missed)).toBe(true);
  });

  it('TowerMissed is flagged as missed', () => {
    const f = frame(events, 11);
    expect(f.recentHits.some(h => h.missed)).toBe(true);
  });

  it('hit older than 3 ticks is NOT in recentHits', () => {
    const f = frame(events, 14);
    // tick 10 hit is 4 ticks ago — outside HIT_WINDOW=3
    expect(f.recentHits.filter(h => h.tick === 10)).toHaveLength(0);
  });
});

// ─── ActionRejected ──────────────────────────────────────────────────────────

describe('ActionRejected', () => {
  const events: GameEvent[] = [
    { type: 'ActionRejected', tick: 0, actionType: 'BuildTower', reason: 'Cell is on path' },
  ];

  it('collects rejected actions', () => {
    const f = frame(events, 0);
    expect(f.rejectedActions).toHaveLength(1);
    expect(f.rejectedActions[0].reason).toBe('Cell is on path');
  });
});
