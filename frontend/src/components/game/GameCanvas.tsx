/**
 * GameCanvas.tsx — AAA canvas renderer.
 * New: projectile arcs, tower barrel rotation, range ring on hover,
 *      animated path flow, fixed responsive sizing.
 */
import React, { useEffect, useRef, useCallback } from 'react';
import {
  drawGroundTile, drawPathTile, drawStartTile, drawEndTile,
  drawCannon, drawSniper, drawFrost,
  drawBasic, drawFast, drawArmored,
  drawMuzzleFlash, drawSlowAura,
  drawProjectile, drawRangeRing,
  type TowerType,
} from './sprites';
import { burst, stepParticles, drawParticles, type Particle } from './particles';
import type {
  LevelConfig, CatalogResult, TowerCatalogEntry,
  PlacedTower, FrameState, RecentHit,
} from '../../types';

interface GameCanvasProps {
  level:        LevelConfig;
  catalog:      CatalogResult;
  pathSet:      Set<string>;
  placedTowers: PlacedTower[];
  frame:        FrameState | null;
  phase:        'plan' | 'loading' | 'replay';
  currentTick:  number;
  hoveredCell:  { x: number; y: number } | null;
  selectedTower: TowerType | null;        // for range ring
  onCellClick:  (x: number, y: number) => void;
  onCellHover:  (x: number, y: number) => void;
  onCellLeave:  () => void;
  cellSize:     number;
}

// Projectile record (purely visual)
interface Projectile {
  id:    number;
  type:  TowerType;
  x1:    number; y1: number;  // tower centre px
  x2:    number; y2: number;  // enemy centre px
  spawnTick: number;
}

let _pid = 0;

function towerDrawFn(type: TowerType) {
  if (type === 'Cannon') return drawCannon;
  if (type === 'Sniper') return drawSniper;
  return drawFrost;
}
function enemyDrawFn(type: string) {
  if (type === 'Fast')    return drawFast;
  if (type === 'Armored') return drawArmored;
  return drawBasic;
}

export default function GameCanvas({
  level, catalog, pathSet, placedTowers,
  frame, phase, currentTick,
  hoveredCell, selectedTower,
  onCellClick, onCellHover, onCellLeave,
  cellSize,
}: GameCanvasProps) {
  const canvasRef    = useRef<HTMLCanvasElement>(null);
  const particleRef  = useRef<Particle[]>([]);
  const rafRef       = useRef<number>(0);
  const prevHitsRef  = useRef<RecentHit[]>([]);
  const shakeRef     = useRef(0);
  const projectilesRef = useRef<Projectile[]>([]);
  // towerId → angle in radians (barrel points toward last target)
  const towerAnglesRef = useRef<Map<number, number>>(new Map());
  const flowOffsetRef  = useRef(0); // animated path dash offset

  const { gridWidth, gridHeight, path } = level;
  const startCell = path[0];
  const endCell   = path[path.length - 1];
  const cp = cellSize;
  const W  = gridWidth  * cp;
  const H  = gridHeight * cp;

  // ── Detect new hits → spawn projectiles + particles + shake ──────────────
  useEffect(() => {
    if (!frame) return;
    const prevIds = new Set(prevHitsRef.current.map(h => `${h.towerId}-${h.enemyId}-${h.tick}`));

    for (const hit of frame.recentHits) {
      const id = `${hit.towerId}-${hit.enemyId}-${hit.tick}`;
      if (prevIds.has(id)) continue;

      const towerDef = frame.towers.find(t => t.towerId === hit.towerId);
      const enemyDef = frame.enemies.find(e => e.enemyId === hit.enemyId);
      if (!towerDef) continue;

      const tx = (towerDef.x + 0.5) * cp;
      const ty = (towerDef.y + 0.5) * cp;
      const type = towerDef.towerType as TowerType;

      if (enemyDef) {
        const ex = (enemyDef.x + 0.5) * cp;
        const ey = (enemyDef.y + 0.5) * cp;

        // Update tower barrel angle
        const angle = Math.atan2(ey - ty, ex - tx);
        towerAnglesRef.current.set(hit.towerId, angle);

        if (!hit.missed) {
          // Spawn projectile
          projectilesRef.current = [
            ...projectilesRef.current,
            { id: _pid++, type, x1: tx, y1: ty, x2: ex, y2: ey, spawnTick: currentTick },
          ];
          // Particles at impact
          const color = type === 'Cannon' ? '#ef4444' : type === 'Sniper' ? '#fde047' : '#93c5fd';
          const kind  = type === 'Frost' ? 'frost' : type === 'Cannon' ? 'explosion' : 'spark';
          particleRef.current = burst(particleRef.current, ex, ey, kind, type === 'Cannon' ? 10 : 6, color, type === 'Cannon' ? 3.5 : 2);
          shakeRef.current = type === 'Cannon' ? 4 : 2;
        }
      }
    }
    prevHitsRef.current = [...frame.recentHits];
  }, [frame, cp, currentTick]);

  // ── Main draw loop ─────────────────────────────────────────────────────────
  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Screen shake
    const shake = shakeRef.current;
    if (shake > 0) shakeRef.current = Math.max(0, shake - 0.6);
    const sx = shake > 0 ? (Math.random() - 0.5) * shake * 1.5 : 0;
    const sy = shake > 0 ? (Math.random() - 0.5) * shake * 1.5 : 0;

    // Animated path flow
    flowOffsetRef.current = (flowOffsetRef.current + 0.4) % 16;
    const fo = flowOffsetRef.current;

    ctx.clearRect(0, 0, W, H);
    ctx.save();
    ctx.translate(sx, sy);

    // 1. Terrain
    for (let row = 0; row < gridHeight; row++) {
      for (let col = 0; col < gridWidth; col++) {
        const key = `${col},${row}`;
        const isPath  = pathSet.has(key);
        const isStart = col === startCell.x && row === startCell.y;
        const isEnd   = col === endCell.x   && row === endCell.y;
        const isHover = hoveredCell?.x === col && hoveredCell?.y === row && phase === 'plan';
        const px = col * cp, py = row * cp;

        if      (isStart) drawStartTile(ctx, px, py, cp, cp, fo);
        else if (isEnd)   drawEndTile(ctx, px, py, cp, cp, fo);
        else if (isPath)  drawPathTile(ctx, px, py, cp, cp, fo);
        else              drawGroundTile(ctx, px, py, cp, cp, !isPath && !!isHover);
      }
    }

    // 2. Path direction line
    if (path.length > 1) {
      ctx.save();
      ctx.strokeStyle = 'rgba(161,117,53,0.18)';
      ctx.lineWidth = 2; ctx.setLineDash([8, 8]); ctx.lineDashOffset = -fo;
      ctx.beginPath();
      path.forEach((pt, i) => {
        const px = pt.x * cp + cp / 2;
        const py = pt.y * cp + cp / 2;
        i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
      });
      ctx.stroke(); ctx.setLineDash([]); ctx.restore();
    }

    // 3. Range ring (plan mode hover)
    if (phase === 'plan' && hoveredCell && selectedTower) {
      const tDef = catalog.towers.find(t => t.type === selectedTower) as TowerCatalogEntry | undefined;
      if (tDef && !pathSet.has(`${hoveredCell.x},${hoveredCell.y}`)) {
        const color = selectedTower === 'Cannon' ? '#ef4444'
                    : selectedTower === 'Sniper' ? '#eab308' : '#3b82f6';
        const pulse = Math.sin(Date.now() * 0.004) * 0.5 + 0.5;
        drawRangeRing(ctx,
          (hoveredCell.x + 0.5) * cp,
          (hoveredCell.y + 0.5) * cp,
          tDef.range, cp, color, pulse,
        );
      }
    }

    // 4a. Plan-phase: ghost + range ring for already-placed towers
    if (phase === 'plan') {
      for (const pt of placedTowers) {
        const px = pt.x * cp + cp / 2;
        const py = pt.y * cp + cp / 2;
        towerDrawFn(pt.towerType as TowerType)(ctx, px, py, cp, -Math.PI / 2);
      }
    }

    // 4b. Replay-phase towers
    if (phase === 'replay' && frame) {
      for (const t of frame.towers) {
        const px = t.x * cp + cp / 2;
        const py = t.y * cp + cp / 2;
        const angle = towerAnglesRef.current.get(t.towerId) ?? -Math.PI / 2;
        towerDrawFn(t.towerType as TowerType)(ctx, px, py, cp, angle);

        // Muzzle flash
        const hit = frame.recentHits.find(h => h.towerId === t.towerId && !h.missed);
        if (hit) drawMuzzleFlash(ctx, px, py, t.towerType as TowerType, currentTick - hit.tick);
      }

      // 4c. Projectile arcs
      const nextProjectiles: Projectile[] = [];
      for (const proj of projectilesRef.current) {
        const age = currentTick - proj.spawnTick;
        if (age < 4) {
          drawProjectile(ctx, proj.x1, proj.y1, proj.x2, proj.y2, proj.type, age);
          nextProjectiles.push(proj);
        }
      }
      projectilesRef.current = nextProjectiles;

      // 4d. Enemies (always on top)
      for (const en of frame.enemies) {
        const px = (en.x + 0.5) * cp;
        const py = (en.y + 0.5) * cp;
        if (en.isSlowed) drawSlowAura(ctx, px, py, cp, currentTick);
        enemyDrawFn(en.enemyType)(ctx, px, py, cp, currentTick);

        // Health bar
        const maxHp = catalog.enemies.find(c => c.type === en.enemyType)?.health ?? 1;
        const pct   = Math.max(0, en.healthRemaining / maxHp);
        const barW  = cp * 0.7, barH = 4;
        const bx = px - barW / 2, by = py - cp * 0.5 + 3;
        ctx.fillStyle = 'rgba(0,0,0,0.65)'; ctx.fillRect(bx - 1, by - 1, barW + 2, barH + 2);
        const hpColor = pct > 0.6 ? '#22c55e' : pct > 0.3 ? '#f59e0b' : '#ef4444';
        ctx.fillStyle = hpColor;
        ctx.shadowColor = hpColor; ctx.shadowBlur = 4;
        ctx.fillRect(bx, by, barW * pct, barH);
        ctx.shadowBlur = 0;
      }
    }

    // 5. Particles
    drawParticles(ctx, particleRef.current);
    particleRef.current = stepParticles(particleRef.current);

    // 6. Scanlines
    ctx.save(); ctx.globalAlpha = 0.025; ctx.fillStyle = '#000';
    for (let y = 0; y < H; y += 2) ctx.fillRect(0, y, W, 1);
    ctx.restore();

    // 7. Vignette
    const vg = ctx.createRadialGradient(W / 2, H / 2, H * 0.25, W / 2, H / 2, H * 0.9);
    vg.addColorStop(0, 'rgba(0,0,0,0)'); vg.addColorStop(1, 'rgba(0,0,0,0.55)');
    ctx.fillStyle = vg; ctx.fillRect(0, 0, W, H);

    ctx.restore();
    rafRef.current = requestAnimationFrame(draw);
  }, [
    W, H, gridWidth, gridHeight, cp, pathSet,
    startCell, endCell, path, hoveredCell, selectedTower,
    phase, placedTowers, frame, currentTick, catalog,
  ]);

  useEffect(() => {
    rafRef.current = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafRef.current);
  }, [draw]);

  // ── Pointer events ────────────────────────────────────────────────────────
  function getCell(e: React.MouseEvent<HTMLCanvasElement>) {
    const rect = canvasRef.current!.getBoundingClientRect();
    const scaleX = W / rect.width;
    const scaleY = H / rect.height;
    return {
      x: Math.floor(((e.clientX - rect.left) * scaleX) / cp),
      y: Math.floor(((e.clientY - rect.top)  * scaleY) / cp),
    };
  }

  return (
    <canvas
      ref={canvasRef}
      width={W}
      height={H}
      style={{
        display: 'block',
        width: '100%', height: '100%',
        cursor: phase === 'plan' ? 'crosshair' : 'default',
        borderRadius: 8,
        boxShadow: '0 0 60px rgba(0,0,0,0.9), 0 0 120px rgba(0,0,0,0.5)',
      }}
      onClick={e => { const c = getCell(e); onCellClick(c.x, c.y); }}
      onMouseMove={e => { const c = getCell(e); onCellHover(c.x, c.y); }}
      onMouseLeave={onCellLeave}
    />
  );
}
