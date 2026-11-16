/**
 * Battlefield — AAA v2
 * Fixed: event log by tick, wave-0 guard, responsive canvas sizing.
 * New: OutcomeSplash, range ring prop, projectile/rotation in GameCanvas.
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronRight, Play, X } from 'lucide-react';
import GameCanvas      from '../components/game/GameCanvas';
import GameHUD         from '../components/game/GameHUD';
import WaveAnnouncement from '../components/game/WaveAnnouncement';
import OutcomeSplash   from '../components/game/OutcomeSplash';
import { getLevel }      from '../api/levels';
import { getCatalog }    from '../api/catalog';
import { runSimulation } from '../api/simulations';
import { frameAt }       from '../engine/frameAt';
import { sortedTicks }   from '../engine/groupByTick';
import type {
  LevelConfig, CatalogResult, TowerCatalogEntry,
  PlacedTower, TowerType, TargetingMode,
  GameEvent, SimulationResult,
} from '../types';
import styles from './Battlefield.module.css';

const SPEED_OPTIONS = [
  { label: '1×', ms: 30 },
  { label: '2×', ms: 15 },
  { label: '4×', ms: 7  },
];

const TOWER_COLOR: Record<TowerType, string> = {
  Cannon: '#ef4444', Sniper: '#eab308', Frost: '#3b82f6',
};
const TOWER_ICON: Record<TowerType, string> = {
  Cannon: '💥', Sniper: '🎯', Frost: '❄️',
};

type Phase = 'plan' | 'loading' | 'replay';

function buildPathSet(path: LevelConfig['path']): Set<string> {
  return new Set(path.map(c => `${c.x},${c.y}`));
}

// ─── Log line type ────────────────────────────────────────────────────────────
interface LogLine { key: string; cls: string; text: string; tick: number; }

export default function Battlefield() {
  const [level,   setLevel]   = useState<LevelConfig | null>(null);
  const [catalog, setCatalog] = useState<CatalogResult | null>(null);
  const [pathSet, setPathSet] = useState<Set<string>>(new Set());

  // Plan phase
  const [selectedTower,     setSelectedTower]     = useState<TowerType | null>(null);
  const [selectedTargeting, setSelectedTargeting] = useState<TargetingMode>('FIRST');
  const [placedTowers,      setPlacedTowers]      = useState<PlacedTower[]>([]);
  const [planGold,          setPlanGold]          = useState(150);
  const [hoveredCell,       setHoveredCell]       = useState<{ x: number; y: number } | null>(null);

  const [phase, setPhase] = useState<Phase>('plan');
  const [error, setError] = useState<string | null>(null);

  // Replay
  const [result,   setResult]   = useState<SimulationResult | null>(null);
  const [ticks,    setTicks]    = useState<number[]>([]);
  const [tickIdx,  setTickIdx]  = useState(0);
  const [playing,  setPlaying]  = useState(false);
  const [speedIdx, setSpeedIdx] = useState(0);
  const [logLines, setLogLines] = useState<LogLine[]>([]);
  const [showSplash, setShowSplash] = useState(false);
  const logRef = useRef<HTMLDivElement>(null);
  const rafRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Wave announcement
  const [announceWave, setAnnounceWave] = useState(0);
  const [showAnnounce, setShowAnnounce] = useState(false);
  const lastAnnouncedWave = useRef(-1);

  // Responsive cell size
  const [cellSize, setCellSize] = useState(56);
  const wrapRef = useRef<HTMLDivElement>(null);

  // Load data
  useEffect(() => {
    Promise.all([getLevel('level1'), getCatalog()])
      .then(([lv, cat]) => {
        setLevel(lv); setCatalog(cat);
        setPlanGold(lv.startGold);
        setPathSet(buildPathSet(lv.path));
      })
      .catch(e => setError(String(e)));
  }, []);

  // Responsive cell size — perfectly fits the grid in the wrapper
  useEffect(() => {
    if (!level || !wrapRef.current) return;
    const ob = new ResizeObserver(entries => {
      const { width, height } = entries[0].contentRect;
      const byW = Math.floor((width  - 16) / level.gridWidth);
      const byH = Math.floor((height - 16) / level.gridHeight);
      setCellSize(Math.max(28, Math.min(80, byW, byH)));
    });
    ob.observe(wrapRef.current);
    return () => ob.disconnect();
  }, [level]);

  // ── Plan: grid interactions ─────────────────────────────────────────────
  function handleCellClick(x: number, y: number) {
    if (phase !== 'plan' || !selectedTower || !level || !catalog) return;
    if (pathSet.has(`${x},${y}`)) return;
    if (placedTowers.some(t => t.x === x && t.y === y)) return;
    const tDef = catalog.towers.find(t => t.type === selectedTower);
    if (!tDef || planGold < tDef.cost) return;
    setPlacedTowers(prev => [...prev, { towerType: selectedTower, x, y, targetingMode: selectedTargeting, wave: 0 }]);
    setPlanGold(g => g - tDef.cost);
  }

  function removePlaced(idx: number) {
    if (!catalog) return;
    const t = placedTowers[idx];
    const cost = catalog.towers.find(c => c.type === t.towerType)?.cost ?? 0;
    setPlacedTowers(prev => prev.filter((_, i) => i !== idx));
    setPlanGold(g => g + cost);
  }

  // ── Simulate ────────────────────────────────────────────────────────────
  async function handleRunSimulation() {
    if (!level) return;
    setPhase('loading'); setError(null);
    try {
      const res = await runSimulation({
        levelId: level.id, seed: 42,
        actions: placedTowers.map(t => ({
          type: 'BUILD_TOWER' as const,
          wave: t.wave, towerType: t.towerType,
          x: t.x, y: t.y, targetingMode: t.targetingMode,
        })),
      });
      setResult(res);
      const ts = sortedTicks(res.events);
      setTicks(ts); setTickIdx(0);
      setPhase('replay'); setPlaying(true);
      buildLog(res.events);
      lastAnnouncedWave.current = -1;
      setShowSplash(false);
    } catch (e) {
      setError(String(e)); setPhase('plan');
    }
  }

  // ── Build log (store tick per line for filtering) ───────────────────────
  function buildLog(events: GameEvent[]) {
    const lines: LogLine[] = [];
    for (const ev of events) {
      let cls = styles.evtDefault, text = '';
      switch (ev.type) {
        case 'WaveStarted':    cls = styles.evtWave;   text = `⚡ Wave ${ev.wave} started (t${ev.tick})`; break;
        case 'WaveCompleted':  cls = styles.evtWave;   text = `✅ Wave ${ev.wave} done +${ev.bonusGold}g`; break;
        case 'EnemyKilled':    cls = styles.evtKill;   text = `💀 ${ev.enemyType}#${ev.enemyId} +${ev.reward}g`; break;
        case 'LifeLost':       cls = styles.evtLeak;   text = `❤️ Leaked! HP→${ev.baseHealthAfter}`; break;
        case 'TowerBuilt':     cls = styles.evtFire;   text = `🏗 ${ev.towerType} @ (${ev.x},${ev.y})`; break;
        case 'ActionRejected': cls = styles.evtReject; text = `❌ ${ev.reason}`; break;
        case 'GameEnded':      cls = styles.evtEnd;    text = `🏁 ${ev.outcome}`; break;
        default: continue;
      }
      lines.push({ key: `${ev.type}-${ev.tick}-${Math.random()}`, cls, text, tick: ev.tick });
    }
    setLogLines(lines);
  }

  // ── Replay ticker ──────────────────────────────────────────────────────
  const advanceTick = useCallback(() => {
    setTickIdx(prev => {
      if (prev >= ticks.length - 1) { setPlaying(false); return prev; }
      return prev + 1;
    });
  }, [ticks.length]);

  useEffect(() => {
    if (!playing || phase !== 'replay') return;
    rafRef.current = setTimeout(advanceTick, SPEED_OPTIONS[speedIdx].ms);
    return () => { if (rafRef.current) clearTimeout(rafRef.current); };
  }, [playing, phase, tickIdx, speedIdx, advanceTick]);

  // Auto-scroll log
  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [tickIdx]);

  // ── Skip to next wave ─────────────────────────────────────────────────
  function skipToNextWave() {
    if (!result) return;
    const curTick = ticks[tickIdx] ?? 0;
    const nextEv  = result.events.find(ev => ev.type === 'WaveStarted' && ev.tick > curTick);
    if (!nextEv) return;
    const nextIdx = ticks.findIndex(t => t >= nextEv.tick);
    if (nextIdx !== -1) setTickIdx(nextIdx);
  }

  // ── Reset ─────────────────────────────────────────────────────────────
  function handleReset() {
    if (rafRef.current) clearTimeout(rafRef.current);
    setPhase('plan'); setResult(null); setTicks([]); setTickIdx(0);
    setPlaying(false); setPlacedTowers([]);
    if (level) setPlanGold(level.startGold);
    setLogLines([]); setError(null);
    setShowSplash(false); setShowAnnounce(false);
    lastAnnouncedWave.current = -1;
  }

  // ── Current frame ─────────────────────────────────────────────────────
  const currentTick = ticks[tickIdx] ?? 0;
  const curFrame = (phase === 'replay' && result && catalog)
    ? frameAt(result.events, currentTick, catalog.enemies, level?.startGold ?? 150, level?.startBaseHealth ?? 20)
    : null;

  // Wave announcement — only on wave > 0, once per wave
  useEffect(() => {
    if (!curFrame || phase !== 'replay') return;
    const w = curFrame.wave;
    if (w > 0 && w !== lastAnnouncedWave.current) {
      lastAnnouncedWave.current = w;
      setAnnounceWave(w);
      setShowAnnounce(true);
      const t = setTimeout(() => setShowAnnounce(false), 50);
      return () => clearTimeout(t);
    }
  }, [curFrame?.wave, phase]);

  // Outcome splash — trigger when GameEnded tick is reached
  useEffect(() => {
    if (!result || phase !== 'replay') return;
    const endEv = result.events.find(ev => ev.type === 'GameEnded');
    if (endEv && currentTick >= endEv.tick && !playing) {
      const t = setTimeout(() => setShowSplash(true), 600);
      return () => clearTimeout(t);
    }
  }, [result, phase, currentTick, playing]);

  // ── HUD values ─────────────────────────────────────────────────────────
  const hudGold    = phase === 'replay' ? (curFrame?.gold ?? 0) : planGold;
  const hudHealth  = phase === 'replay' ? (curFrame?.baseHealth ?? level?.startBaseHealth ?? 20) : (level?.startBaseHealth ?? 20);
  const hudWave    = curFrame?.wave ?? 0;
  const outcome    = result?.outcome;

  // ── Tower card ────────────────────────────────────────────────────────
  function renderTowerCard(t: TowerCatalogEntry) {
    const isSelected = selectedTower === t.type;
    const canAfford  = planGold >= t.cost;
    return (
      <div
        key={t.type}
        className={[
          styles.towerCard,
          isSelected ? styles.towerCardSelected : '',
          (!canAfford && !isSelected) ? styles.towerCardDisabled : '',
        ].join(' ')}
        onClick={() => canAfford && setSelectedTower(isSelected ? null : t.type)}
      >
        <div className={styles.towerIcon} style={{ background: `${TOWER_COLOR[t.type]}22` }}>
          {TOWER_ICON[t.type]}
        </div>
        <div className={styles.towerInfo}>
          <span className={styles.towerName}>{t.type}</span>
          <span className={styles.towerStats}>dmg {t.damage} · rng {t.range}</span>
          <span className={styles.towerStats}>cd {t.cooldown}t</span>
        </div>
        <span className={styles.towerCost}>{t.cost}g</span>
      </div>
    );
  }

  // ─── Render ──────────────────────────────────────────────────────────
  if (error) return (
    <div className={styles.loadingOverlay} style={{ position: 'fixed', inset: 0 }}>
      <span style={{ color: '#ef4444' }}>Error: {error}</span>
      <button onClick={handleReset} style={{ color: '#94a3b8', background: 'none', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>Retry</button>
    </div>
  );

  if (!level || !catalog) return (
    <div className={styles.loadingOverlay} style={{ position: 'fixed', inset: 0 }}>
      <div className={styles.spinner} />
      <span className={styles.loadingText}>Loading level…</span>
    </div>
  );

  return (
    <div className={styles.page}>

      {/* ── Left sidebar ──────────────────────────────────────────── */}
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <div className={styles.sidebarTitle}>Tower Arsenal</div>
        </div>

        <div className={styles.sidebarSection}>
          {catalog.towers.map(renderTowerCard)}
          {selectedTower && (
            <div className={styles.targetingRow}>
              <span className={styles.targetingLabel}>Targeting Mode</span>
              <select
                className={styles.targetingSelect}
                value={selectedTargeting}
                onChange={e => setSelectedTargeting(e.target.value as TargetingMode)}
              >
                {(['FIRST','LAST','STRONGEST','CLOSEST'] as TargetingMode[]).map(m => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
            </div>
          )}
        </div>

        <div className={styles.sidebarSection} style={{ flex: 1, overflowY: 'auto' }}>
          <div className={styles.sidebarTitle} style={{ marginBottom: 8 }}>
            Build Queue ({placedTowers.length})
          </div>
          <div className={styles.buildQueue}>
            {placedTowers.length === 0
              ? <span className={styles.emptyQueue}>Hover to preview range · click to place</span>
              : placedTowers.map((t, i) => (
                  <div key={i} className={styles.queueItem}>
                    <span>{TOWER_ICON[t.towerType]}</span>
                    <span>{t.towerType}</span>
                    <span style={{ fontSize: '0.6rem', color: '#334155' }}>({t.x},{t.y})</span>
                    <button className={styles.queueRemove} onClick={() => removePlaced(i)}>
                      <X size={11} />
                    </button>
                  </div>
                ))
            }
          </div>
        </div>

        {phase === 'plan' && (
          <div className={styles.runBtn}>
            <button
              className={styles.runButton}
              onClick={handleRunSimulation}
              disabled={placedTowers.length === 0}
            >
              <Play size={13} /> Run Simulation
            </button>
          </div>
        )}
      </aside>

      {/* ── Centre canvas ─────────────────────────────────────────── */}
      <div ref={wrapRef} className={styles.canvasWrap}>
        {phase === 'loading' ? (
          <div className={styles.loadingOverlay}>
            <div className={styles.spinner} />
            <span className={styles.loadingText}>Simulating…</span>
          </div>
        ) : (
          <div
            className={styles.canvasInner}
            style={{
              width:  level.gridWidth  * cellSize,
              height: level.gridHeight * cellSize,
            }}
          >
            <GameCanvas
              level={level}
              catalog={catalog}
              pathSet={pathSet}
              placedTowers={placedTowers}
              frame={curFrame}
              phase={phase}
              currentTick={currentTick}
              hoveredCell={hoveredCell}
              selectedTower={selectedTower}
              onCellClick={handleCellClick}
              onCellHover={(x, y) => { if (phase === 'plan') setHoveredCell({ x, y }); }}
              onCellLeave={() => setHoveredCell(null)}
              cellSize={cellSize}
            />

            <GameHUD
              gold={hudGold}
              health={hudHealth}
              maxHealth={level.startBaseHealth}
              wave={hudWave}
              waveCount={level.waveCount}
              phase={phase}
              playing={playing}
              tickIdx={tickIdx}
              tickCount={ticks.length}
              currentTick={currentTick}
              maxTick={ticks[ticks.length - 1] ?? 0}
              speedIdx={speedIdx}
              speedOptions={SPEED_OPTIONS}
              outcome={outcome}
              onPlay={() => setPlaying(p => !p)}
              onScrub={idx => { setTickIdx(idx); setPlaying(false); }}
              onSpeed={setSpeedIdx}
              onSkip={skipToNextWave}
              onReset={handleReset}
            />

            <WaveAnnouncement
              wave={announceWave}
              total={level.waveCount}
              show={showAnnounce}
            />

            {showSplash && (
              <OutcomeSplash
                outcome={outcome}
                onDismiss={handleReset}
              />
            )}
          </div>
        )}
      </div>

      {/* ── Right: combat log ─────────────────────────────────────── */}
      <aside className={styles.eventLog}>
        <div className={styles.eventLogHeader}>Combat Log</div>
        <div ref={logRef} className={styles.eventLogList}>
          {phase === 'plan' && (
            <span className={[styles.eventLogItem, styles.evtDefault].join(' ')} style={{ fontStyle: 'italic' }}>
              Events appear during replay…
            </span>
          )}
          {phase === 'replay' &&
            /* Fixed: filter by actual tick instead of slice hack */
            logLines
              .filter(l => l.tick <= currentTick)
              .map(l => (
                <div key={l.key} className={[styles.eventLogItem, l.cls].join(' ')}>
                  {l.text}
                </div>
              ))
          }
        </div>
      </aside>
    </div>
  );
}
