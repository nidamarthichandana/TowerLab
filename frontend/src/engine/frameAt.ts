/**
 * frameAt — pure replay engine.
 *
 * Given the full ordered event list and a target tick, returns the
 * complete frame state at that tick. This is the ONLY source of truth
 * for the visual replay — no game mechanics are implemented here,
 * only event interpretation.
 *
 * Rules implemented (from handoff §6):
 *  1. Towers: appear at TowerBuilt; no sell/upgrade yet.
 *  2. Enemies: appear at EnemySpawned, positioned by latest EnemyMoved,
 *     disappear at EnemyKilled or LifeLost.
 *  3. Enemy x/y use the same coordinate system as path waypoints.
 *  4. Health bars = catalog HP − cumulative damageDealt for that enemy.
 *  5. Gold and baseHealth follow goldAfter / baseHealthAfter.
 *  6. Slow effect (EffectApplied effectType="Slow") is cosmetic —
 *     tint the enemy for durationTicks. We track slowUntilTick.
 *  7. recentHits: TowerFired / TowerMissed events within last 3 ticks.
 */

import type {
  GameEvent,
  FrameState,
  ReplayTower,
  ReplayEnemy,
  RecentHit,
  ActionRejectedEvent,
  EnemyCatalogEntry,
} from '../types';

const HIT_WINDOW_TICKS = 3;

/**
 * @param events     Full ordered event list from SimulationResult.
 * @param tick       Target tick to compute state at.
 * @param catalog    Enemy catalog entries (for max HP per type).
 * @param startGold  Level startGold value.
 * @param startHealth Level startBaseHealth value.
 */
export function frameAt(
  events: GameEvent[],
  tick: number,
  catalog: EnemyCatalogEntry[],
  startGold: number,
  startHealth: number,
): FrameState {
  // Build a lookup: enemyType → maxHP
  const maxHp = new Map<string, number>(
    catalog.map(e => [e.type, e.health])
  );

  const towers = new Map<number, ReplayTower>();
  const enemies = new Map<number, ReplayEnemy>();
  const enemyDamage = new Map<number, number>(); // enemyId → cumulative damage

  let gold = startGold;
  let baseHealth = startHealth;
  let wave = 0;
  const recentHits: RecentHit[] = [];
  const rejectedActions: ActionRejectedEvent[] = [];

  for (const ev of events) {
    if (ev.tick > tick) break; // events are ordered

    switch (ev.type) {
      case 'WaveStarted':
        wave = ev.wave;
        break;

      case 'TowerBuilt':
        towers.set(ev.towerId, {
          towerId: ev.towerId,
          towerType: ev.towerType,
          x: ev.x,
          y: ev.y,
        });
        gold = ev.goldAfter;
        break;

      case 'EnemySpawned': {
        const hp = maxHp.get(ev.enemyType) ?? 0;
        enemies.set(ev.enemyId, {
          enemyId: ev.enemyId,
          enemyType: ev.enemyType,
          x: 0,
          y: 0,
          healthRemaining: hp,
          isSlowed: false,
          slowUntilTick: 0,
        });
        enemyDamage.set(ev.enemyId, 0);
        break;
      }

      case 'EnemyMoved': {
        const en = enemies.get(ev.enemyId);
        if (en) {
          en.x = ev.x;
          en.y = ev.y;
        }
        break;
      }

      case 'TowerFired': {
        // Accumulate damage for health bar
        const prevDmg = enemyDamage.get(ev.enemyId) ?? 0;
        const newDmg = prevDmg + ev.damageDealt;
        enemyDamage.set(ev.enemyId, newDmg);
        const en = enemies.get(ev.enemyId);
        if (en) {
          const max = maxHp.get(en.enemyType) ?? 0;
          en.healthRemaining = Math.max(0, max - newDmg);
        }
        // Track hit for muzzle flash / attack line
        if (ev.tick >= tick - HIT_WINDOW_TICKS) {
          recentHits.push({ towerId: ev.towerId, enemyId: ev.enemyId, tick: ev.tick, missed: false });
        }
        break;
      }

      case 'TowerMissed':
        if (ev.tick >= tick - HIT_WINDOW_TICKS) {
          recentHits.push({ towerId: ev.towerId, enemyId: ev.enemyId, tick: ev.tick, missed: true });
        }
        break;

      case 'EffectApplied': {
        if (ev.effectType === 'Slow') {
          const en = enemies.get(ev.enemyId);
          if (en) {
            en.isSlowed = true;
            en.slowUntilTick = ev.tick + ev.durationTicks;
          }
        }
        break;
      }

      case 'EnemyKilled':
        enemies.delete(ev.enemyId);
        enemyDamage.delete(ev.enemyId);
        gold = ev.goldAfter;
        break;

      case 'LifeLost':
        enemies.delete(ev.enemyId);
        enemyDamage.delete(ev.enemyId);
        baseHealth = ev.baseHealthAfter;
        break;

      case 'WaveCompleted':
        gold = ev.goldAfter;
        break;

      case 'ActionRejected':
        rejectedActions.push(ev);
        break;

      // GameEnded — no state change needed
    }
  }

  // Expire slow effects that have run out by this tick
  for (const en of enemies.values()) {
    if (en.isSlowed && tick >= en.slowUntilTick) {
      en.isSlowed = false;
    }
  }

  return {
    towers: [...towers.values()],
    enemies: [...enemies.values()],
    gold,
    baseHealth,
    wave,
    recentHits,
    rejectedActions,
  };
}
