/**
 * TowerLab Frontend Simulation & Balance Engine Client
 */

const API_BASE = 'http://localhost:8080/api';

// State
let currentLevel = null;
let levelsCatalog = [];
let plannedActions = []; // [{tick: 0, type: 'BUILD_TOWER', towerType: 'CANNON', x, y, targetingMode}]
let selectedTowerType = 'CANNON';
let selectedTargetingMode = 'FIRST';
let currentGold = 350;
let currentBaseHealth = 100;

// Replay State
let simulationData = null; // response from /api/simulations/play
let isReplaying = false;
let currentReplayTick = 0;
let replaySpeed = 1.0;
let replayInterval = null;
let hoveredTile = null;

// Tower archetypes metadata
const TOWER_METADATA = {
  CANNON: { name: 'Cannon', cost: 100, range: 2.5, color: '#f59e0b', icon: '💣' },
  SNIPER: { name: 'Sniper', cost: 150, range: 6.0, color: '#3b82f6', icon: '🎯' },
  FROST: { name: 'Frost', cost: 125, range: 2.0, color: '#06b6d4', icon: '❄️' }
};

// Canvas & Context
const canvas = document.getElementById('battleCanvas');
const ctx = canvas.getContext('2d');
const GRID_COLS = 12;
const GRID_ROWS = 8;
let cellWidth = canvas.width / GRID_COLS;
let cellHeight = canvas.height / GRID_ROWS;

// DOM Elements
const levelSelect = document.getElementById('level-select');
const seedInput = document.getElementById('seed-input');
const btnRandomSeed = document.getElementById('btn-random-seed');
const hudHealth = document.getElementById('hud-health');
const hudGold = document.getElementById('hud-gold');
const hudWave = document.getElementById('hud-wave');
const hudTick = document.getElementById('hud-tick');
const btnSimulate = document.getElementById('btn-simulate');
const btnClearPlan = document.getElementById('btn-clear-plan');
const btnReplayPlay = document.getElementById('btn-replay-play');
const btnStepPrev = document.getElementById('btn-step-prev');
const btnStepNext = document.getElementById('btn-step-next');
const btnReplayReset = document.getElementById('btn-replay-reset');
const scrubber = document.getElementById('timeline-scrubber');
const scrubCurrent = document.getElementById('scrub-current');
const scrubTotal = document.getElementById('scrub-total');
const outcomeBadge = document.getElementById('sim-outcome-badge');
const eventsStream = document.getElementById('events-stream');
const eventCount = document.getElementById('event-count');
const gridTooltip = document.getElementById('grid-tooltip');

// Modal Elements
const btnBatchModal = document.getElementById('btn-batch-modal');
const analyticsModal = document.getElementById('analytics-modal');
const btnCloseModal = document.getElementById('btn-close-modal');
const btnRunBatch = document.getElementById('btn-run-batch');
const batchRunsSelect = document.getElementById('batch-runs-select');
const analyticsResults = document.getElementById('analytics-results');

// Initialize
window.addEventListener('DOMContentLoaded', async () => {
  setupEventListeners();
  await loadLevels();
  renderGrid();
});

function setupEventListeners() {
  // Tower selection
  document.querySelectorAll('.tower-card').forEach(card => {
    card.addEventListener('click', () => {
      document.querySelectorAll('.tower-card').forEach(c => c.classList.remove('selected'));
      card.classList.add('selected');
      selectedTowerType = card.dataset.type;
      renderGrid();
    });
  });

  // Targeting mode
  document.querySelectorAll('.target-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.target-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedTargetingMode = btn.dataset.mode;
    });
  });

  // Level select
  levelSelect.addEventListener('change', async (e) => {
    await selectLevel(e.target.value);
  });

  // Seed randomize
  btnRandomSeed.addEventListener('click', () => {
    seedInput.value = Math.floor(Math.random() * 900000) + 100000;
  });

  // Plan actions
  btnSimulate.addEventListener('click', runSimulation);
  btnClearPlan.addEventListener('click', clearPlan);

  // Replay controls
  btnReplayPlay.addEventListener('click', toggleReplay);
  btnStepPrev.addEventListener('click', () => seekTick(Math.max(0, currentReplayTick - 1)));
  btnStepNext.addEventListener('click', () => {
    if (simulationData) {
      seekTick(Math.min(simulationData.ticks, currentReplayTick + 1));
    }
  });
  btnReplayReset.addEventListener('click', () => seekTick(0));

  scrubber.addEventListener('input', (e) => {
    seekTick(parseInt(e.target.value, 10));
  });

  // Speed controls
  document.querySelectorAll('.speed-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.speed-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      replaySpeed = parseFloat(btn.dataset.speed);
      if (isReplaying) {
        pauseReplay();
        startReplay();
      }
    });
  });

  // Filter tabs
  document.querySelectorAll('.filter-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      filterEventLog(tab.dataset.filter);
    });
  });

  // Modal controls
  btnBatchModal.addEventListener('click', () => analyticsModal.classList.remove('hidden'));
  btnCloseModal.addEventListener('click', () => analyticsModal.classList.add('hidden'));
  document.querySelector('.modal-backdrop').addEventListener('click', () => analyticsModal.classList.add('hidden'));
  btnRunBatch.addEventListener('click', runBatchAnalytics);

  // Canvas interaction
  canvas.addEventListener('mousemove', onCanvasMouseMove);
  canvas.addEventListener('mouseleave', () => {
    hoveredTile = null;
    gridTooltip.classList.add('hidden');
    renderGrid();
  });
  canvas.addEventListener('click', onCanvasClick);
}

// Load Levels from Backend
async function loadLevels() {
  try {
    const res = await fetch(`${API_BASE}/levels`);
    levelsCatalog = await res.json();
    levelSelect.innerHTML = '';
    levelsCatalog.forEach(lvl => {
      const opt = document.createElement('option');
      opt.value = lvl.id;
      opt.textContent = `${lvl.name} (${lvl.totalWaves} Waves)`;
      levelSelect.appendChild(opt);
    });

    if (levelsCatalog.length > 0) {
      await selectLevel(levelsCatalog[0].id);
    }
  } catch (err) {
    console.error('Failed to load levels:', err);
  }
}

async function selectLevel(levelId) {
  try {
    const res = await fetch(`${API_BASE}/levels/${levelId}`);
    currentLevel = await res.json();

    document.getElementById('arena-level-title').textContent = `${currentLevel.name} (${currentLevel.gridWidth}×${currentLevel.gridHeight})`;
    document.getElementById('arena-level-desc').textContent = currentLevel.description;

    clearPlan();
  } catch (err) {
    console.error('Failed to load level detail:', err);
  }
}

function clearPlan() {
  pauseReplay();
  simulationData = null;
  plannedActions = [];
  currentReplayTick = 0;
  currentGold = currentLevel ? currentLevel.initialGold : 350;
  currentBaseHealth = currentLevel ? currentLevel.initialHealth : 100;
  outcomeBadge.classList.add('hidden');
  scrubber.max = 0;
  scrubber.value = 0;
  scrubCurrent.textContent = 'Tick: 0';
  scrubTotal.textContent = 'Total: 0';
  updateHud();
  eventsStream.innerHTML = '<div class="empty-events">Place towers and click <b>Run Simulation</b> to generate the tick event log.</div>';
  eventCount.textContent = '0 Events';
  renderGrid();
}

function updateHud() {
  hudHealth.textContent = currentBaseHealth;
  hudGold.textContent = currentGold;
  hudWave.textContent = currentLevel ? `1/${currentLevel.waves.length}` : '1/1';
  hudTick.textContent = currentReplayTick;
}

// Canvas & Grid Interaction
function onCanvasMouseMove(e) {
  const rect = canvas.getBoundingClientRect();
  const scaleX = canvas.width / rect.width;
  const scaleY = canvas.height / rect.height;
  const mouseX = (e.clientX - rect.left) * scaleX;
  const mouseY = (e.clientY - rect.top) * scaleY;

  const col = Math.floor(mouseX / cellWidth);
  const row = Math.floor(mouseY / cellHeight);

  if (col >= 0 && col < GRID_COLS && row >= 0 && row < GRID_ROWS) {
    hoveredTile = { x: col, y: row };
    const isPath = isPathTile(col, row);
    const existingTower = plannedActions.find(a => a.x === col && a.y === row);

    gridTooltip.classList.remove('hidden');
    gridTooltip.style.left = `${e.clientX + 15}px`;
    gridTooltip.style.top = `${e.clientY + 15}px`;

    if (existingTower) {
      gridTooltip.innerHTML = `<b>${existingTower.towerType} Tower</b><br>Target: ${existingTower.targetingMode}<br>Click to remove`;
    } else if (isPath) {
      gridTooltip.innerHTML = `<b>Path Tile (${col}, ${row})</b><br><span style="color:#ef4444">Cannot build on path</span>`;
    } else {
      const towerInfo = TOWER_METADATA[selectedTowerType];
      gridTooltip.innerHTML = `<b>Empty Tile (${col}, ${row})</b><br>Build ${towerInfo.name} (🪙 ${towerInfo.cost})`;
    }
    renderGrid();
  }
}

function onCanvasClick(e) {
  if (!hoveredTile || !currentLevel) return;

  const { x, y } = hoveredTile;
  const existingIndex = plannedActions.findIndex(a => a.x === x && a.y === y);

  if (existingIndex >= 0) {
    // Remove tower and refund gold
    const removed = plannedActions.splice(existingIndex, 1)[0];
    const cost = TOWER_METADATA[removed.towerType].cost;
    currentGold += cost;
    updateHud();
    renderGrid();
    return;
  }

  if (isPathTile(x, y)) {
    return; // Cannot build on path
  }

  const cost = TOWER_METADATA[selectedTowerType].cost;
  if (currentGold < cost) {
    alert(`Insufficient gold! Needed: ${cost}, Current: ${currentGold}`);
    return;
  }

  currentGold -= cost;
  plannedActions.push({
    tick: 0,
    type: 'BUILD_TOWER',
    towerType: selectedTowerType,
    x: x,
    y: y,
    targetingMode: selectedTargetingMode
  });

  updateHud();
  renderGrid();
}

function isPathTile(x, y) {
  if (!currentLevel || !currentLevel.pathWaypoints) return false;
  const wps = currentLevel.pathWaypoints;
  for (let i = 0; i < wps.length - 1; i++) {
    const from = wps[i];
    const to = wps[i + 1];
    const minX = Math.min(from.x, to.x);
    const maxX = Math.max(from.x, to.x);
    const minY = Math.min(from.y, to.y);
    const maxY = Math.max(from.y, to.y);

    if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
      if (from.x === to.x && x === from.x) return true;
      if (from.y === to.y && y === from.y) return true;
    }
  }
  return false;
}

// Render Grid, Paths, Towers, and Active Replay Entities
function renderGrid() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Background Grid Lines
  ctx.strokeStyle = '#1e293b';
  ctx.lineWidth = 1;
  for (let c = 0; c <= GRID_COLS; c++) {
    ctx.beginPath();
    ctx.moveTo(c * cellWidth, 0);
    ctx.lineTo(c * cellWidth, canvas.height);
    ctx.stroke();
  }
  for (let r = 0; r <= GRID_ROWS; r++) {
    ctx.beginPath();
    ctx.moveTo(0, r * cellHeight);
    ctx.lineTo(canvas.width, r * cellHeight);
    ctx.stroke();
  }

  // Draw Path
  if (currentLevel && currentLevel.pathWaypoints && currentLevel.pathWaypoints.length > 1) {
    const wps = currentLevel.pathWaypoints;

    // Path segments
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = cellHeight * 0.7;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(wps[0].x * cellWidth + cellWidth / 2, wps[0].y * cellHeight + cellHeight / 2);
    for (let i = 1; i < wps.length; i++) {
      ctx.lineTo(wps[i].x * cellWidth + cellWidth / 2, wps[i].y * cellHeight + cellHeight / 2);
    }
    ctx.stroke();

    // Inner path track
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 4;
    ctx.setLineDash([8, 8]);
    ctx.stroke();
    ctx.setLineDash([]);

    // Entrance Portal
    const start = wps[0];
    ctx.fillStyle = '#a855f7';
    ctx.beginPath();
    ctx.arc(start.x * cellWidth + cellWidth / 2, start.y * cellHeight + cellHeight / 2, 18, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#fff';
    ctx.font = '12px Outfit';
    ctx.textAlign = 'center';
    ctx.fillText('ENTRY', start.x * cellWidth + cellWidth / 2, start.y * cellHeight + cellHeight / 2 + 4);

    // Citadel Base Exit
    const end = wps[wps.length - 1];
    ctx.fillStyle = '#ef4444';
    ctx.beginPath();
    ctx.arc(end.x * cellWidth + cellWidth / 2, end.y * cellHeight + cellHeight / 2, 20, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 12px Outfit';
    ctx.textAlign = 'center';
    ctx.fillText('BASE', end.x * cellWidth + cellWidth / 2, end.y * cellHeight + cellHeight / 2 + 4);
  }

  // Draw Hover Range Preview
  if (hoveredTile && !isPathTile(hoveredTile.x, hoveredTile.y)) {
    const existing = plannedActions.find(a => a.x === hoveredTile.x && a.y === hoveredTile.y);
    const type = existing ? existing.towerType : selectedTowerType;
    const meta = TOWER_METADATA[type];

    ctx.strokeStyle = meta.color;
    ctx.lineWidth = 1.5;
    ctx.fillStyle = `${meta.color}22`;
    ctx.beginPath();
    ctx.arc(
      hoveredTile.x * cellWidth + cellWidth / 2,
      hoveredTile.y * cellHeight + cellHeight / 2,
      meta.range * cellWidth,
      0, Math.PI * 2
    );
    ctx.fill();
    ctx.stroke();
  }

  // Draw Placed Towers
  plannedActions.forEach(tower => {
    const cx = tower.x * cellWidth + cellWidth / 2;
    const cy = tower.y * cellHeight + cellHeight / 2;
    const meta = TOWER_METADATA[tower.towerType];

    // Tower Base
    ctx.fillStyle = '#0f172a';
    ctx.strokeStyle = meta.color;
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.roundRect(tower.x * cellWidth + 6, tower.y * cellHeight + 6, cellWidth - 12, cellHeight - 12, 8);
    ctx.fill();
    ctx.stroke();

    // Tower Icon & Badge
    ctx.font = '22px Outfit';
    ctx.textAlign = 'center';
    ctx.fillText(meta.icon, cx, cy + 4);

    // Targeting mode badge
    ctx.fillStyle = meta.color;
    ctx.font = 'bold 10px JetBrains Mono';
    ctx.fillText(tower.targetingMode[0], cx + cellWidth * 0.3, cy + cellHeight * 0.35);
  });

  // Render Replay Entities at currentReplayTick
  if (simulationData && simulationData.events) {
    renderReplayEntities(currentReplayTick);
  }
}

// Replay Entities Calculation & Rendering
function renderReplayEntities(tick) {
  const events = simulationData.events;

  // Active creeps calculation up to current tick
  const creepsMap = new Map();
  let baseHp = currentLevel.initialHealth;
  let gold = currentLevel.initialGold;
  let waveNum = 1;

  for (const ev of events) {
    if (ev.tick > tick) break;

    if (ev.type === 'CREEP_SPAWNED') {
      creepsMap.set(ev.creepId, {
        id: ev.creepId,
        type: ev.creepType,
        x: ev.creepX,
        y: ev.creepY,
        health: ev.health,
        maxHealth: ev.health,
        alive: true,
        slowed: false
      });
    } else if (ev.type === 'CREEP_DAMAGED') {
      const c = creepsMap.get(ev.creepId);
      if (c) {
        c.health = ev.remainingHealth;
      }
    } else if (ev.type === 'CREEP_SLOWED') {
      const c = creepsMap.get(ev.creepId);
      if (c) c.slowed = true;
    } else if (ev.type === 'CREEP_KILLED') {
      const c = creepsMap.get(ev.creepId);
      if (c) c.alive = false;
      if (ev.goldRemaining !== undefined) gold = ev.goldRemaining;
    } else if (ev.type === 'CREEP_LEAKED') {
      const c = creepsMap.get(ev.creepId);
      if (c) c.alive = false;
    } else if (ev.type === 'BASE_DAMAGED') {
      if (ev.baseHealth !== undefined) baseHp = ev.baseHealth;
    } else if (ev.type === 'WAVE_STARTED') {
      waveNum = ev.waveNumber;
    }
  }

  // Update HUD
  hudHealth.textContent = baseHp;
  hudGold.textContent = gold;
  hudWave.textContent = `${waveNum}/${currentLevel.waves.length}`;
  hudTick.textContent = tick;

  // Render Active Creeps
  creepsMap.forEach(creep => {
    if (creep.alive) {
      // Find latest position for this creep up to tick
      // Interpolate along path
      const creepPos = calculateCreepInterpolatedPosition(creep.id, tick);
      const cx = creepPos.x * cellWidth + cellWidth / 2;
      const cy = creepPos.y * cellHeight + cellHeight / 2;

      // Creep Body
      ctx.fillStyle = creep.slowed ? '#38bdf8' : '#f43f5e';
      ctx.beginPath();
      ctx.arc(cx, cy, 12, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Health bar overhead
      const barW = 28;
      const barH = 4;
      const hpPct = Math.max(0, creep.health / creep.maxHealth);
      ctx.fillStyle = '#334155';
      ctx.fillRect(cx - barW / 2, cy - 20, barW, barH);
      ctx.fillStyle = hpPct > 0.5 ? '#10b981' : hpPct > 0.25 ? '#fbbf24' : '#ef4444';
      ctx.fillRect(cx - barW / 2, cy - 20, barW * hpPct, barH);
    }
  });

  // Render Shot Projectile Rays for events occurring at this exact tick
  const tickEvents = events.filter(e => e.tick === tick);
  tickEvents.forEach(ev => {
    if (ev.type === 'TOWER_FIRED') {
      const tower = plannedActions.find((_, idx) => idx + 1 === ev.towerId) || plannedActions[0];
      const creep = creepsMap.get(ev.creepId);
      if (tower && creep) {
        const tx = tower.x * cellWidth + cellWidth / 2;
        const ty = tower.y * cellHeight + cellHeight / 2;
        const creepPos = calculateCreepInterpolatedPosition(creep.id, tick);
        const cx = creepPos.x * cellWidth + cellWidth / 2;
        const cy = creepPos.y * cellHeight + cellHeight / 2;

        ctx.strokeStyle = tower.towerType === 'SNIPER' ? '#60a5fa' : tower.towerType === 'FROST' ? '#38bdf8' : '#fbbf24';
        ctx.lineWidth = tower.towerType === 'SNIPER' ? 3 : 2;
        ctx.beginPath();
        ctx.moveTo(tx, ty);
        ctx.lineTo(cx, cy);
        ctx.stroke();
      }
    }
  });
}

// Approximate position along waypoints
function calculateCreepInterpolatedPosition(creepId, tick) {
  if (!currentLevel || !currentLevel.pathWaypoints) return { x: 0, y: 0 };
  const wps = currentLevel.pathWaypoints;

  // Find spawn tick of creep
  const spawnEvent = simulationData.events.find(e => e.type === 'CREEP_SPAWNED' && e.creepId === creepId);
  if (!spawnEvent) return { x: wps[0].x, y: wps[0].y };

  const elapsed = tick - spawnEvent.tick;
  const speed = spawnEvent.creepType === 'RUNNER' ? 0.18 : spawnEvent.creepType === 'TANK' ? 0.05 : 0.10;
  let dist = elapsed * speed;

  let currX = wps[0].x;
  let currY = wps[0].y;

  for (let i = 0; i < wps.length - 1; i++) {
    const from = wps[i];
    const to = wps[i + 1];
    const segDist = Math.hypot(to.x - from.x, to.y - from.y);

    if (dist <= segDist) {
      const r = dist / segDist;
      return { x: from.x + (to.x - from.x) * r, y: from.y + (to.y - from.y) * r };
    }
    dist -= segDist;
    currX = to.x;
    currY = to.y;
  }
  return { x: currX, y: currY };
}

// Run Single Play Simulation
async function runSimulation() {
  if (!currentLevel) return;

  btnSimulate.disabled = true;
  btnSimulate.innerHTML = '<span>⏳</span> Simulating...';

  try {
    const payload = {
      levelId: currentLevel.id,
      seed: parseInt(seedInput.value, 10) || 133742,
      actions: plannedActions
    };

    const res = await fetch(`${API_BASE}/simulations/play`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      alert(`Simulation Error: ${err.message || 'Failed to simulate'}`);
      return;
    }

    simulationData = await res.json();

    // Setup timeline
    scrubber.max = simulationData.ticks;
    scrubber.value = 0;
    scrubTotal.textContent = `Total: ${simulationData.ticks}`;

    // Outcome Badge
    outcomeBadge.textContent = simulationData.outcome;
    outcomeBadge.className = `outcome-badge ${simulationData.outcome}`;

    // Populate event stream
    populateEventLog(simulationData.events);

    // Start auto replay
    seekTick(0);
    startReplay();
  } catch (err) {
    console.error('Run simulation failure:', err);
    alert('Failed to connect to backend simulation engine.');
  } finally {
    btnSimulate.disabled = false;
    btnSimulate.innerHTML = '<span>▶</span> Run Simulation';
  }
}

// Replay Loop
function toggleReplay() {
  if (isReplaying) {
    pauseReplay();
  } else {
    startReplay();
  }
}

function startReplay() {
  if (!simulationData) return;
  isReplaying = true;
  btnReplayPlay.textContent = '⏸';

  const intervalMs = Math.max(10, 50 / replaySpeed);
  replayInterval = setInterval(() => {
    if (currentReplayTick >= simulationData.ticks) {
      pauseReplay();
      return;
    }
    seekTick(currentReplayTick + 1);
  }, intervalMs);
}

function pauseReplay() {
  isReplaying = false;
  btnReplayPlay.textContent = '▶';
  if (replayInterval) {
    clearInterval(replayInterval);
    replayInterval = null;
  }
}

function seekTick(tick) {
  currentReplayTick = tick;
  scrubber.value = tick;
  scrubCurrent.textContent = `Tick: ${tick}`;
  renderGrid();
}

// Populate Events Log
function populateEventLog(events) {
  eventCount.textContent = `${events.length} Events`;
  eventsStream.innerHTML = '';

  events.forEach(ev => {
    const row = document.createElement('div');
    row.className = `event-row ${ev.type}`;
    row.dataset.category = getEventCategory(ev.type);

    let msg = `[${ev.type}]`;
    if (ev.type === 'TOWER_BUILT') msg = `Built ${ev.towerType} at (${ev.x}, ${ev.y}) [${ev.targetingMode}]`;
    else if (ev.type === 'WAVE_STARTED') msg = `Wave ${ev.waveNumber} commenced`;
    else if (ev.type === 'CREEP_SPAWNED') msg = `${ev.creepType} spawned (HP: ${ev.health})`;
    else if (ev.type === 'TOWER_FIRED') msg = `Tower #${ev.towerId} fired (Dmg: ${ev.damage})`;
    else if (ev.type === 'CREEP_DAMAGED') msg = `Creep #${ev.creepId} took ${ev.damage} dmg (HP: ${ev.remainingHealth})`;
    else if (ev.type === 'CREEP_SLOWED') msg = `Creep #${ev.creepId} slowed by ${(ev.slowFactor * 100).toFixed(0)}%`;
    else if (ev.type === 'CREEP_KILLED') msg = `Creep #${ev.creepId} killed (+🪙 ${ev.goldBounty})`;
    else if (ev.type === 'BASE_DAMAGED') msg = `BASE HIT! (-${ev.damage} HP, Remaining: ${ev.baseHealth})`;
    else if (ev.type === 'SIMULATION_COMPLETED') msg = `FINISHED: ${ev.outcome} (Health: ${ev.baseHealth}, Gold: ${ev.goldRemaining})`;
    else if (ev.type === 'ACTION_REJECTED') msg = `REJECTED: ${ev.message}`;

    row.innerHTML = `<span class="tick-tag">T:${ev.tick}</span><span>${msg}</span>`;
    eventsStream.appendChild(row);
  });
}

function getEventCategory(type) {
  if (['TOWER_FIRED', 'CREEP_DAMAGED', 'CREEP_SLOWED', 'CREEP_KILLED', 'BASE_DAMAGED'].includes(type)) return 'COMBAT';
  if (['TOWER_BUILT', 'ACTION_REJECTED'].includes(type)) return 'ECONOMY';
  if (['WAVE_STARTED', 'CREEP_SPAWNED', 'CREEP_LEAKED'].includes(type)) return 'WAVES';
  return 'ALL';
}

function filterEventLog(category) {
  const rows = eventsStream.querySelectorAll('.event-row');
  rows.forEach(r => {
    if (category === 'ALL' || r.dataset.category === category) {
      r.style.display = 'flex';
    } else {
      r.style.display = 'none';
    }
  });
}

// Run Monte Carlo Batch Balance Analysis
async function runBatchAnalytics() {
  if (!currentLevel) return;

  btnRunBatch.disabled = true;
  btnRunBatch.innerHTML = '<span>⏳</span> Executing Batch Runs...';

  try {
    const runs = parseInt(batchRunsSelect.value, 10);
    const payload = {
      levelId: currentLevel.id,
      runs: runs,
      baseSeed: parseInt(seedInput.value, 10) || 54321,
      actions: plannedActions
    };

    const res = await fetch(`${API_BASE}/simulations/batch`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      alert(`Batch Error: ${err.message || 'Failed to run batch analysis'}`);
      return;
    }

    const data = await res.json();
    displayBatchResults(data);
  } catch (err) {
    console.error('Batch analytics failed:', err);
    alert('Failed to execute batch analytics.');
  } finally {
    btnRunBatch.disabled = false;
    btnRunBatch.innerHTML = '<span>⚡</span> Execute Batch Analysis';
  }
}

function displayBatchResults(data) {
  analyticsResults.classList.remove('hidden');

  document.getElementById('metric-winrate').textContent = `${data.winRate}%`;
  document.getElementById('metric-winloss').textContent = `${data.wins} Wins / ${data.losses} Losses`;
  document.getElementById('metric-health').textContent = data.avgBaseHealth;
  document.getElementById('metric-ticks').textContent = data.avgTicks;
  document.getElementById('metric-gold').textContent = data.avgGold;

  const tbody = document.getElementById('tower-metrics-tbody');
  tbody.innerHTML = '';

  const metrics = data.towerMetrics;
  for (const [key, stats] of Object.entries(metrics)) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><b>${stats.towerType}</b></td>
      <td>${stats.count}</td>
      <td>${stats.totalDamageDealt.toLocaleString()}</td>
      <td><span style="color:#06b6d4;font-weight:700">${stats.damageSharePercentage}%</span></td>
      <td>${stats.totalKills.toLocaleString()}</td>
      <td>${stats.killSharePercentage}%</td>
      <td>${stats.costEfficiency}</td>
      <td>${stats.utilizationPercentage}%</td>
    `;
    tbody.appendChild(tr);
  }
}

