/**
 * sprites.ts — Canvas drawing: towers, enemies, terrain, projectiles, effects.
 * Updated: tower rotation, range rings, animated path, projectile arcs.
 */

export type TowerType = 'Cannon' | 'Sniper' | 'Frost';
export type EnemyType = 'Basic' | 'Fast' | 'Armored';

// ─── Utility ──────────────────────────────────────────────────────────────────
function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + r);
  ctx.lineTo(x + w, y + h - r);
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  ctx.lineTo(x + r, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - r);
  ctx.lineTo(x, y + r);
  ctx.quadraticCurveTo(x, y, x + r, y);
  ctx.closePath();
}

// ─── Tower Sprites (angle = radians, barrel points toward target) ─────────────

export function drawCannon(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, angle = -Math.PI / 4) {
  const s = size * 0.42;
  ctx.save();
  // Base platform
  ctx.shadowColor = '#ef4444'; ctx.shadowBlur = 14;
  roundRect(ctx, cx - s, cy - s * 0.7, s * 2, s * 1.5, 4);
  const baseGrad = ctx.createRadialGradient(cx, cy + s * 0.1, 0, cx, cy + s * 0.1, s * 1.4);
  baseGrad.addColorStop(0, '#4a1010'); baseGrad.addColorStop(1, '#1a0404');
  ctx.fillStyle = baseGrad; ctx.fill();
  ctx.strokeStyle = '#ef4444'; ctx.lineWidth = 1.5; ctx.stroke();
  ctx.restore();

  // Rotating barrel
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(angle);
  roundRect(ctx, -s * 0.18, -s * 0.95, s * 0.36, s * 0.95, 2);
  const bGrad = ctx.createLinearGradient(-s * 0.18, 0, s * 0.18, 0);
  bGrad.addColorStop(0, '#1a1a1a'); bGrad.addColorStop(0.5, '#555'); bGrad.addColorStop(1, '#1a1a1a');
  ctx.fillStyle = bGrad; ctx.fill();
  ctx.strokeStyle = '#888'; ctx.lineWidth = 1; ctx.stroke();
  ctx.restore();

  // Red gem
  ctx.save();
  ctx.beginPath(); ctx.arc(cx, cy + s * 0.5, s * 0.18, 0, Math.PI * 2);
  ctx.fillStyle = '#ef4444'; ctx.shadowColor = '#ef4444'; ctx.shadowBlur = 10; ctx.fill();
  ctx.restore();
}

export function drawSniper(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, angle = -Math.PI / 2) {
  const s = size * 0.42;
  ctx.save();
  // Tower body
  ctx.shadowColor = '#eab308'; ctx.shadowBlur = 12;
  roundRect(ctx, cx - s * 0.45, cy - s * 1.0, s * 0.9, s * 1.7, 3);
  const bGrad = ctx.createLinearGradient(cx - s * 0.45, 0, cx + s * 0.45, 0);
  bGrad.addColorStop(0, '#1a1500'); bGrad.addColorStop(0.5, '#3d2e00'); bGrad.addColorStop(1, '#1a1500');
  ctx.fillStyle = bGrad; ctx.fill();
  ctx.strokeStyle = '#eab308'; ctx.lineWidth = 1.5; ctx.stroke();
  ctx.restore();

  // Rotating long barrel
  ctx.save();
  ctx.translate(cx, cy - s * 0.2);
  ctx.rotate(angle);
  roundRect(ctx, -s * 0.1, -s * 1.6, s * 0.2, s * 1.6, 1);
  const barGrad = ctx.createLinearGradient(-s * 0.1, 0, s * 0.1, 0);
  barGrad.addColorStop(0, '#222'); barGrad.addColorStop(0.5, '#666'); barGrad.addColorStop(1, '#222');
  ctx.fillStyle = barGrad; ctx.fill();
  ctx.strokeStyle = '#aaa'; ctx.lineWidth = 0.5; ctx.stroke();
  ctx.restore();

  // Yellow scope
  ctx.save();
  ctx.beginPath(); ctx.arc(cx, cy - s * 0.2, s * 0.22, 0, Math.PI * 2);
  ctx.fillStyle = '#eab308'; ctx.shadowColor = '#eab308'; ctx.shadowBlur = 10; ctx.fill();
  ctx.beginPath(); ctx.arc(cx - s * 0.06, cy - s * 0.26, s * 0.08, 0, Math.PI * 2);
  ctx.fillStyle = 'rgba(255,255,255,0.4)'; ctx.shadowBlur = 0; ctx.fill();
  ctx.restore();
}

export function drawFrost(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, _angle = 0) {
  const s = size * 0.42;
  // Glow aura
  ctx.save();
  const auraGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, s * 1.6);
  auraGrad.addColorStop(0, 'rgba(59,130,246,0.15)'); auraGrad.addColorStop(1, 'rgba(59,130,246,0)');
  ctx.fillStyle = auraGrad; ctx.beginPath(); ctx.arc(cx, cy, s * 1.6, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
  // Crystal spire
  ctx.save();
  ctx.shadowColor = '#3b82f6'; ctx.shadowBlur = 16;
  ctx.beginPath();
  ctx.moveTo(cx,       cy - s * 1.4);
  ctx.lineTo(cx + s * 0.5, cy - s * 0.2);
  ctx.lineTo(cx + s * 0.35, cy + s * 0.7);
  ctx.lineTo(cx - s * 0.35, cy + s * 0.7);
  ctx.lineTo(cx - s * 0.5, cy - s * 0.2);
  ctx.closePath();
  const cGrad = ctx.createLinearGradient(cx - s * 0.5, cy - s * 1.4, cx + s * 0.5, cy + s * 0.7);
  cGrad.addColorStop(0, '#93c5fd'); cGrad.addColorStop(0.4, '#3b82f6'); cGrad.addColorStop(1, '#1e3a8a');
  ctx.fillStyle = cGrad; ctx.fill();
  ctx.strokeStyle = '#bfdbfe'; ctx.lineWidth = 1; ctx.stroke();
  ctx.restore();
  // Facet highlight
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(cx, cy - s * 1.4); ctx.lineTo(cx + s * 0.2, cy - s * 0.5); ctx.lineTo(cx, cy - s * 0.3);
  ctx.closePath(); ctx.fillStyle = 'rgba(219,234,254,0.4)'; ctx.fill();
  ctx.restore();
}

// ─── Enemy Sprites ────────────────────────────────────────────────────────────

export function drawBasic(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, _tick: number) {
  const r = size * 0.28;
  ctx.save(); ctx.shadowColor = '#94a3b8'; ctx.shadowBlur = 6;
  ctx.beginPath();
  for (let i = 0; i < 6; i++) {
    const a = (Math.PI / 3) * i - Math.PI / 6;
    i === 0 ? ctx.moveTo(cx + r * Math.cos(a), cy + r * Math.sin(a))
            : ctx.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
  }
  ctx.closePath();
  const g = ctx.createRadialGradient(cx - r * 0.3, cy - r * 0.3, 0, cx, cy, r);
  g.addColorStop(0, '#cbd5e1'); g.addColorStop(1, '#475569');
  ctx.fillStyle = g; ctx.fill(); ctx.strokeStyle = '#94a3b8'; ctx.lineWidth = 1; ctx.stroke();
  ctx.beginPath(); ctx.arc(cx, cy - r * 0.1, r * 0.2, 0, Math.PI * 2);
  ctx.fillStyle = '#ef4444'; ctx.shadowColor = '#ef4444'; ctx.shadowBlur = 4; ctx.fill();
  ctx.restore();
}

export function drawFast(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, tick: number) {
  const r = size * 0.22;
  const wobble = Math.sin(tick * 0.4) * 1.5;
  ctx.save(); ctx.translate(cx, cy + wobble);
  ctx.shadowColor = '#22c55e'; ctx.shadowBlur = 8;
  ctx.beginPath();
  ctx.moveTo(0, -r * 1.4); ctx.lineTo(r, 0); ctx.lineTo(0, r * 1.4); ctx.lineTo(-r, 0);
  ctx.closePath();
  const g = ctx.createLinearGradient(-r, -r, r, r);
  g.addColorStop(0, '#86efac'); g.addColorStop(1, '#15803d');
  ctx.fillStyle = g; ctx.fill(); ctx.strokeStyle = '#4ade80'; ctx.lineWidth = 1; ctx.stroke();
  ctx.strokeStyle = 'rgba(134,239,172,0.4)'; ctx.lineWidth = 1;
  [-0.3, 0, 0.3].forEach(dy => {
    ctx.beginPath(); ctx.moveTo(-r * 1.8, dy * r); ctx.lineTo(-r * 0.8, dy * r); ctx.stroke();
  });
  ctx.restore();
}

export function drawArmored(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, _tick: number) {
  const r = size * 0.35;
  ctx.save(); ctx.shadowColor = '#f97316'; ctx.shadowBlur = 10;
  roundRect(ctx, cx - r, cy - r * 0.85, r * 2, r * 1.7, 4);
  const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r * 1.5);
  g.addColorStop(0, '#7c2d12'); g.addColorStop(1, '#431407');
  ctx.fillStyle = g; ctx.fill(); ctx.strokeStyle = '#f97316'; ctx.lineWidth = 2; ctx.stroke();
  ctx.strokeStyle = 'rgba(249,115,22,0.5)'; ctx.lineWidth = 1;
  [-0.2, 0.3].forEach(dy => {
    ctx.beginPath(); ctx.moveTo(cx - r, cy + dy * r); ctx.lineTo(cx + r, cy + dy * r); ctx.stroke();
  });
  ctx.save();
  roundRect(ctx, cx - r * 0.5, cy - r * 0.65, r, r * 0.35, 2);
  ctx.fillStyle = 'rgba(249,115,22,0.3)'; ctx.fill();
  ctx.strokeStyle = '#f97316'; ctx.lineWidth = 1; ctx.stroke();
  ctx.restore(); ctx.restore();
}

// ─── Terrain ──────────────────────────────────────────────────────────────────

export function drawGroundTile(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, hover: boolean) {
  const g = ctx.createLinearGradient(x, y, x, y + h);
  g.addColorStop(0, hover ? '#1e3a2a' : '#0f2518');
  g.addColorStop(1, hover ? '#152b1f' : '#0a1c10');
  ctx.fillStyle = g; ctx.fillRect(x, y, w, h);
  ctx.strokeStyle = hover ? 'rgba(34,197,94,0.3)' : 'rgba(34,197,94,0.07)';
  ctx.lineWidth = 0.5; ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
  if (hover) {
    ctx.strokeStyle = 'rgba(34,197,94,0.6)'; ctx.lineWidth = 2;
    ctx.strokeRect(x + 1.5, y + 1.5, w - 3, h - 3);
  }
}

export function drawPathTile(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, flowOffset = 0) {
  const g = ctx.createLinearGradient(x, y, x + w, y + h);
  g.addColorStop(0, '#2d1f0a'); g.addColorStop(0.5, '#3d2a0e'); g.addColorStop(1, '#2d1f0a');
  ctx.fillStyle = g; ctx.fillRect(x, y, w, h);
  // Dirt dots
  ctx.fillStyle = 'rgba(255,200,100,0.04)';
  for (let i = 0; i < 5; i++) {
    ctx.beginPath(); ctx.arc(x + (i * 137 % w), y + (i * 97 % h), 1.5, 0, Math.PI * 2); ctx.fill();
  }
  // Animated flow dash
  ctx.save();
  ctx.strokeStyle = 'rgba(161,117,53,0.22)'; ctx.lineWidth = 1;
  ctx.setLineDash([6, 10]); ctx.lineDashOffset = -flowOffset;
  ctx.beginPath(); ctx.moveTo(x + w / 2, y); ctx.lineTo(x + w / 2, y + h); ctx.stroke();
  ctx.restore();
  ctx.strokeStyle = 'rgba(161,117,53,0.2)'; ctx.lineWidth = 0.5;
  ctx.setLineDash([]); ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
}

export function drawStartTile(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, flowOffset = 0) {
  drawPathTile(ctx, x, y, w, h, flowOffset);
  ctx.fillStyle = 'rgba(239,68,68,0.12)'; ctx.fillRect(x, y, w, h);
  ctx.strokeStyle = 'rgba(239,68,68,0.5)'; ctx.lineWidth = 1.5;
  ctx.setLineDash([]); ctx.strokeRect(x + 1, y + 1, w - 2, h - 2);
  ctx.fillStyle = '#fca5a5'; ctx.font = `bold ${Math.round(w * 0.22)}px monospace`;
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
  ctx.fillText('IN', x + w / 2, y + h / 2);
}

export function drawEndTile(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, flowOffset = 0) {
  drawPathTile(ctx, x, y, w, h, flowOffset);
  ctx.fillStyle = 'rgba(234,179,8,0.12)'; ctx.fillRect(x, y, w, h);
  ctx.strokeStyle = 'rgba(234,179,8,0.5)'; ctx.lineWidth = 1.5;
  ctx.setLineDash([]); ctx.strokeRect(x + 1, y + 1, w - 2, h - 2);
  ctx.font = `${Math.round(w * 0.5)}px serif`;
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
  ctx.fillText('🏰', x + w / 2, y + h / 2);
}

// ─── Range Ring ───────────────────────────────────────────────────────────────

export function drawRangeRing(
  ctx: CanvasRenderingContext2D,
  cx: number, cy: number,
  rangeInCells: number, cellSize: number,
  color: string, pulse: number,
) {
  const r = rangeInCells * cellSize;
  ctx.save();
  ctx.globalAlpha = 0.18 + 0.06 * pulse;
  ctx.fillStyle = color;
  ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.fill();
  ctx.globalAlpha = 0.5 + 0.2 * pulse;
  ctx.strokeStyle = color;
  ctx.lineWidth = 1.5;
  ctx.setLineDash([6, 6]);
  ctx.stroke();
  ctx.restore();
}

// ─── Projectile arc ───────────────────────────────────────────────────────────

export function drawProjectile(
  ctx: CanvasRenderingContext2D,
  x1: number, y1: number,
  x2: number, y2: number,
  type: TowerType, age: number,          // age: 0=just fired, 1,2 = older
) {
  const alpha = Math.max(0, 1 - age * 0.45);
  if (alpha <= 0) return;

  const color = type === 'Cannon' ? '#fca5a5'
              : type === 'Sniper' ? '#fde047'
              : '#93c5fd';

  // Travel progress: age 0 → near tower, age 1 → near enemy
  const t = Math.min(1, age * 0.9 + 0.1);
  // Quadratic bezier midpoint (arc upward)
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2 - Math.abs(x2 - x1) * 0.25 - 12;
  // Point at t along bezier
  const px = (1 - t) * (1 - t) * x1 + 2 * (1 - t) * t * mx + t * t * x2;
  const py = (1 - t) * (1 - t) * y1 + 2 * (1 - t) * t * my + t * t * y2;

  ctx.save();
  // Trail line from tower to current position
  ctx.globalAlpha = alpha * 0.5;
  ctx.strokeStyle = color;
  ctx.lineWidth = type === 'Cannon' ? 2 : 1;
  ctx.setLineDash([4, 4]);
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.quadraticCurveTo(mx, my, px, py);
  ctx.stroke();
  ctx.setLineDash([]);

  // Projectile head
  ctx.globalAlpha = alpha;
  ctx.shadowColor = color; ctx.shadowBlur = 8;
  ctx.fillStyle = color;
  ctx.beginPath();
  const radius = type === 'Cannon' ? 5 : type === 'Sniper' ? 3 : 4;
  ctx.arc(px, py, radius * (1 - age * 0.2), 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

// ─── Muzzle flash / slow aura ─────────────────────────────────────────────────

export function drawMuzzleFlash(ctx: CanvasRenderingContext2D, cx: number, cy: number, type: TowerType, age: number) {
  const alpha = Math.max(0, 1 - age / 3);
  const r = (type === 'Cannon' ? 14 : type === 'Sniper' ? 8 : 10) * (1 - age * 0.2);
  const color = type === 'Cannon' ? '#ef4444' : type === 'Sniper' ? '#eab308' : '#3b82f6';
  ctx.save();
  ctx.globalAlpha = alpha;
  ctx.shadowColor = color; ctx.shadowBlur = r * 2;
  ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.fillStyle = color; ctx.fill();
  ctx.restore();
}

export function drawSlowAura(ctx: CanvasRenderingContext2D, cx: number, cy: number, size: number, tick: number) {
  const pulse = 0.7 + 0.3 * Math.sin(tick * 0.3);
  ctx.save();
  ctx.globalAlpha = 0.35 * pulse;
  ctx.shadowColor = '#3b82f6'; ctx.shadowBlur = 10;
  ctx.beginPath(); ctx.arc(cx, cy, size * 0.42, 0, Math.PI * 2);
  ctx.strokeStyle = '#93c5fd'; ctx.lineWidth = 2; ctx.stroke();
  ctx.restore();
}
