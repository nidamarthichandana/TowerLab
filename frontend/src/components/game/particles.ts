/**
 * particles.ts — Lightweight particle system for muzzle flashes,
 * projectile trails, and impact sparks.
 */

export type ParticleKind = 'spark' | 'trail' | 'frost' | 'explosion';

export interface Particle {
  id:    number;
  kind:  ParticleKind;
  x:     number;
  y:     number;
  vx:    number;
  vy:    number;
  life:  number;   // 0–1 (1 = fresh)
  decay: number;   // per-frame decay rate
  size:  number;
  color: string;
}

let _id = 0;

export function burst(
  particles: Particle[],
  x: number, y: number,
  kind: ParticleKind,
  count: number,
  color: string,
  speed = 2.5,
  size  = 3,
): Particle[] {
  const next = [...particles];
  for (let i = 0; i < count; i++) {
    const angle = Math.random() * Math.PI * 2;
    const v = speed * (0.5 + Math.random() * 0.5);
    next.push({
      id: _id++,
      kind, x, y, color, size,
      vx:    Math.cos(angle) * v,
      vy:    Math.sin(angle) * v,
      life:  1,
      decay: 0.07 + Math.random() * 0.08,
    });
  }
  return next;
}

export function stepParticles(particles: Particle[]): Particle[] {
  const next: Particle[] = [];
  for (const p of particles) {
    const life = p.life - p.decay;
    if (life <= 0) continue;
    next.push({
      ...p,
      x:  p.x + p.vx,
      y:  p.y + p.vy,
      vx: p.vx * 0.88,
      vy: p.vy * 0.88 + 0.05, // slight gravity
      life,
    });
  }
  return next;
}

export function drawParticles(ctx: CanvasRenderingContext2D, particles: Particle[]) {
  for (const p of particles) {
    ctx.save();
    ctx.globalAlpha = p.life * p.life;
    ctx.fillStyle   = p.color;
    ctx.shadowColor = p.color;
    ctx.shadowBlur  = p.size * 2;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.size * p.life, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }
}
