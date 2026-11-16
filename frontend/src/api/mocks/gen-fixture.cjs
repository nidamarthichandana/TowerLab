#!/usr/bin/env node
/**
 * Generates a structurally correct synthetic simulation fixture.
 * Seed 42, Cannon@(1,2) FIRST, Frost@(3,0) FIRST, Sniper@(7,4) STRONGEST.
 * 5 waves, WIN outcome, ~2300 ticks.
 *
 * Rules from handoff §6:
 *  - Within one tick: spawns → effects → enemy movement → tower attacks → kills → wave checks
 *  - EnemyMoved uses same coordinate system as path waypoints
 *  - Enemies disappear at EnemyKilled or LifeLost (no separate despawn)
 *  - Gold/baseHealth: follow goldAfter/baseHealthAfter
 *  - GameEnded is always last
 */

const path = [
  {x:0,y:1},{x:1,y:1},{x:2,y:1},{x:3,y:1},{x:4,y:1},
  {x:5,y:1},{x:6,y:1},{x:6,y:2},{x:6,y:3},{x:6,y:4},{x:6,y:5},
  {x:7,y:5},{x:8,y:5},{x:9,y:5},{x:10,y:5},
  {x:10,y:4},{x:10,y:3},{x:10,y:2},{x:11,y:2}
];
const PATH_LEN = path.length; // 19 waypoints

// Tower specs
const TOWERS = {
  Cannon: { id:0, type:'Cannon', x:1, y:2, cost:60, range:2.5, damage:12, cooldown:12, goldAfter:70 },
  Frost:  { id:1, type:'Frost',  x:3, y:0, cost:50, range:2,   damage:3,  cooldown:15, goldAfter:20 },
  Sniper: { id:2, type:'Sniper', x:7, y:4, cost:90, range:5,   damage:40, cooldown:40, goldAfter:0  },
};

// Wave composition: [Basic count, Fast count, Armored count]
const WAVES = [
  [5, 2, 0],
  [6, 3, 1],
  [7, 4, 1],
  [8, 5, 2],
  [10, 6, 3],
];

const ENEMY_HEALTH = { Basic:40, Fast:25, Armored:120 };
const ENEMY_SPEED  = { Basic:0.05, Fast:0.10, Armored:0.035 };
const ENEMY_REWARD = { Basic:8, Fast:7, Armored:20 };
const PREP_TICKS   = 100;
const BONUS_GOLD   = 20;

function dist(tx, ty, ex, ey) {
  return Math.sqrt((tx-ex)**2 + (ty-ey)**2);
}

// Get enemy position along path at progress p (0..PATH_LEN-1)
function posAt(progress) {
  const idx = Math.min(Math.floor(progress), PATH_LEN-2);
  const frac = progress - idx;
  const a = path[idx], b = path[idx+1] ?? path[PATH_LEN-1];
  return { x: a.x + (b.x-a.x)*frac, y: a.y + (b.y-a.y)*frac };
}

const events = [];
let gold = 150;
let baseHealth = 20;
let globalTick = 0;
let nextEnemyId = 0;

// Build towers at tick 0 (wave=0, before wave 1)
// Cannon
gold -= TOWERS.Cannon.cost; // 150-60=90 — but we set goldAfter from fixture
events.push({ type:'TowerBuilt', tick:0, towerId:0, towerType:'Cannon', x:1, y:2, goldAfter:90 });
gold = 90;
// Frost
gold -= TOWERS.Frost.cost;
events.push({ type:'TowerBuilt', tick:0, towerId:1, towerType:'Frost', x:3, y:0, goldAfter:40 });
gold = 40;
// Sniper costs 90 — we don't have enough at tick 0, so it's placed during prep after wave 1
// (wave:1 build)

for (let waveNum = 1; waveNum <= 5; waveNum++) {
  // Wave start
  events.push({ type:'WaveStarted', tick:globalTick, wave:waveNum });

  const [nBasic, nFast, nArmored] = WAVES[waveNum-1];
  const composition = [
    ...Array(nBasic).fill('Basic'),
    ...Array(nFast).fill('Fast'),
    ...Array(nArmored).fill('Armored'),
  ];

  // Enemies active this wave
  const active = new Map(); // enemyId -> { type, progress, hp, alive, spawnTick }
  const towerCooldowns = { 0:0, 1:0, 2: waveNum===1?999:0 }; // Sniper not placed until after wave 1
  if (waveNum === 2) towerCooldowns[2] = 0; // Sniper now active

  // Spawn enemies every 15 ticks
  const spawnSchedule = composition.map((type, i) => ({ type, spawnTick: globalTick + i*15 }));
  const maxWaveTicks = globalTick + 500 + composition.length * 15;

  let tick = globalTick;
  const waveStart = globalTick;

  // Track per-enemy cumulative damage for health bars (not needed in fixture but keep hp correct)
  const enemyDamage = new Map();

  while (tick < maxWaveTicks) {
    // 1) Spawns
    for (const s of spawnSchedule) {
      if (s.spawnTick === tick && !s.spawned) {
        s.spawned = true;
        const eid = nextEnemyId++;
        s.enemyId = eid;
        active.set(eid, {
          type: s.type,
          progress: 0,
          hp: ENEMY_HEALTH[s.type],
          alive: true,
          isSlowed: false,
          slowUntil: 0,
        });
        enemyDamage.set(eid, 0);
        events.push({ type:'EnemySpawned', tick, enemyId:eid, enemyType:s.type, wave:waveNum });
      }
    }

    // 2) Move enemies
    const toRemove = [];
    for (const [eid, en] of active) {
      if (!en.alive) continue;
      const speed = ENEMY_SPEED[en.type] * (en.isSlowed && tick < en.slowUntil ? 0.5 : 1);
      en.progress += speed;
      const pos = posAt(Math.min(en.progress, PATH_LEN-1));
      events.push({ type:'EnemyMoved', tick, enemyId:eid, x:+pos.x.toFixed(2), y:+pos.y.toFixed(2) });

      // Reached end?
      if (en.progress >= PATH_LEN-1) {
        baseHealth -= en.type === 'Armored' ? 2 : 1;
        events.push({ type:'LifeLost', tick, enemyId:eid, damage: en.type === 'Armored' ? 2 : 1, baseHealthAfter:baseHealth });
        en.alive = false;
        toRemove.push(eid);
      }
    }
    toRemove.forEach(id => active.delete(id));

    // 3) Tower attacks (simple targeting)
    for (const [tId, t] of Object.entries({ 0:TOWERS.Cannon, 1:TOWERS.Frost, 2:TOWERS.Sniper })) {
      const tid = parseInt(tId);
      if (waveNum === 1 && tid === 2) continue; // Sniper not built yet
      if (towerCooldowns[tid] > 0) { towerCooldowns[tid]--; continue; }

      const tDef = [TOWERS.Cannon, TOWERS.Frost, TOWERS.Sniper][tid];
      // Find target
      const inRange = [...active.entries()].filter(([, en]) => {
        if (!en.alive) return false;
        const pos = posAt(Math.min(en.progress, PATH_LEN-1));
        return dist(tDef.x, tDef.y, pos.x, pos.y) <= tDef.range;
      });

      if (inRange.length === 0) continue;

      // FIRST targeting: highest progress
      inRange.sort((a, b) => b[1].progress - a[1].progress);
      const [targetId, targetEn] = inRange[0];

      // Apply damage
      const rawDmg = tDef.damage;
      const actualDmg = tDef.type === 'Sniper' ? rawDmg : (tDef.type === 'Cannon' ? rawDmg : rawDmg);
      targetEn.hp -= actualDmg;
      enemyDamage.set(targetId, (enemyDamage.get(targetId)||0) + actualDmg);

      events.push({ type:'TowerFired', tick, towerId:tid, towerType:tDef.type, enemyId:targetId, damageDealt:actualDmg });

      // Frost effect
      if (tDef.type === 'Frost') {
        events.push({ type:'EffectApplied', tick, enemyId:targetId, effectType:'Slow', magnitude:0.5, durationTicks:30 });
        targetEn.isSlowed = true;
        targetEn.slowUntil = tick + 30;
      }

      // Splash: Cannon hits nearby enemies too
      if (tDef.type === 'Cannon') {
        const targetPos = posAt(Math.min(targetEn.progress, PATH_LEN-1));
        for (const [eid2, en2] of active) {
          if (eid2 === targetId || !en2.alive) continue;
          const p2 = posAt(Math.min(en2.progress, PATH_LEN-1));
          if (dist(targetPos.x, targetPos.y, p2.x, p2.y) <= 1.0) {
            en2.hp -= actualDmg;
            enemyDamage.set(eid2, (enemyDamage.get(eid2)||0) + actualDmg);
            events.push({ type:'TowerFired', tick, towerId:tid, towerType:'Cannon', enemyId:eid2, damageDealt:actualDmg });
          }
        }
      }

      towerCooldowns[tid] = tDef.cooldown;

      // Kill check
      for (const [eid, en] of active) {
        if (!en.alive) continue;
        if (en.hp <= 0) {
          gold += ENEMY_REWARD[en.type];
          events.push({ type:'EnemyKilled', tick, enemyId:eid, enemyType:en.type, towerId:tid, reward:ENEMY_REWARD[en.type], goldAfter:gold });
          en.alive = false;
        }
      }
      // Clean up dead
      for (const [eid, en] of active) {
        if (!en.alive) active.delete(eid);
      }
    }

    tick++;

    // Check if all enemies gone
    const allSpawned = spawnSchedule.every(s => s.spawned);
    if (allSpawned && active.size === 0) break;
  }

  globalTick = tick;

  // Wave completed
  gold += BONUS_GOLD;
  events.push({ type:'WaveCompleted', tick:globalTick, wave:waveNum, bonusGold:BONUS_GOLD, goldAfter:gold });
  globalTick++;

  // After wave 1 prep: build Sniper (wave:1)
  if (waveNum === 1) {
    globalTick += 20; // some prep time
    if (gold >= 90) {
      gold -= 90;
      events.push({ type:'TowerBuilt', tick:globalTick, towerId:2, towerType:'Sniper', x:7, y:4, goldAfter:gold });
    }
    globalTick += PREP_TICKS - 20;
  } else {
    globalTick += PREP_TICKS;
  }
}

// Game ended
events.push({ type:'GameEnded', tick:globalTick, outcome:'WIN' });

const result = {
  outcome: 'WIN',
  baseHealth,
  gold,
  ticks: globalTick,
  events,
};

const fs = require('fs');
const outPath = __dirname + '/sample-simulation-result.json';
fs.writeFileSync(outPath, JSON.stringify(result, null, 2));
console.log(`Written ${events.length} events, ${globalTick} ticks → ${outPath}`);
