/**
 * GameHUD.tsx
 * Cinematic floating HUD overlay — renders over the canvas.
 * Top-left: HP. Top-center: Wave. Top-right: Gold.
 * Bottom: scrub bar + controls (replay mode).
 */
import React from 'react';
import { Heart, Coins, Zap, Play, Pause, SkipForward, RotateCcw } from 'lucide-react';

interface GameHUDProps {
  gold:      number;
  health:    number;
  maxHealth: number;
  wave:      number;
  waveCount: number;
  phase:     'plan' | 'loading' | 'replay';
  playing:   boolean;
  tickIdx:   number;
  tickCount: number;
  currentTick: number;
  maxTick:     number;
  speedIdx:    number;
  speedOptions: Array<{ label: string; ms: number }>;
  outcome:   'WIN' | 'LOSS' | undefined;
  onPlay:    () => void;
  onScrub:   (idx: number) => void;
  onSpeed:   (idx: number) => void;
  onSkip:    () => void;
  onReset:   () => void;
}

function HUDPanel({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{
      background: 'rgba(5,10,20,0.85)',
      border: '1px solid rgba(255,255,255,0.1)',
      borderRadius: 8,
      backdropFilter: 'blur(8px)',
      padding: '8px 14px',
      display: 'flex', alignItems: 'center', gap: 8,
      boxShadow: '0 4px 24px rgba(0,0,0,0.6)',
      ...style,
    }}>
      {children}
    </div>
  );
}

function HUDStat({ icon, value, label, color, warning }: {
  icon: React.ReactNode; value: string; label: string; color: string; warning?: boolean;
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <span style={{ color, filter: warning ? 'drop-shadow(0 0 6px currentColor)' : undefined }}>
        {icon}
      </span>
      <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.1 }}>
        <span style={{
          fontSize: '1.1rem', fontWeight: 800, fontFamily: 'monospace',
          color: warning ? color : '#f1f5f9',
          textShadow: warning ? `0 0 12px ${color}` : undefined,
        }}>{value}</span>
        <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', letterSpacing: '0.08em', color: '#475569' }}>
          {label}
        </span>
      </div>
    </div>
  );
}

const divider = (
  <div style={{ width: 1, height: 32, background: 'rgba(255,255,255,0.08)' }} />
);

export default function GameHUD({
  gold, health, maxHealth, wave, waveCount,
  phase, playing, tickIdx, tickCount, currentTick, maxTick,
  speedIdx, speedOptions, outcome,
  onPlay, onScrub, onSpeed, onSkip, onReset,
}: GameHUDProps) {
  const hpWarn   = health <= maxHealth * 0.3;
  const goldWarn = gold < 60;
  const hpPct    = Math.max(0, health / maxHealth);

  return (
    <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 20 }}>

      {/* ── Top bar ─────────────────────────────────────────────────── */}
      <div style={{
        position: 'absolute', top: 12, left: 12, right: 12,
        display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
        gap: 8, pointerEvents: 'auto',
      }}>
        {/* HP — top left */}
        <HUDPanel>
          <HUDStat
            icon={<Heart size={16} fill={hpWarn ? '#ef4444' : 'none'} />}
            value={String(health)}
            label="Base HP"
            color="#ef4444"
            warning={hpWarn}
          />
          {/* HP bar */}
          <div style={{ width: 60, height: 4, background: 'rgba(255,255,255,0.1)', borderRadius: 2, overflow: 'hidden' }}>
            <div style={{
              height: '100%', width: `${hpPct * 100}%`, borderRadius: 2,
              background: hpPct > 0.6 ? '#22c55e' : hpPct > 0.3 ? '#f59e0b' : '#ef4444',
              transition: 'width 0.3s ease',
              boxShadow: `0 0 6px ${hpPct > 0.6 ? '#22c55e' : '#ef4444'}`,
            }} />
          </div>
        </HUDPanel>

        {/* Wave — top center */}
        <HUDPanel style={{ flexDirection: 'column', gap: 2, padding: '6px 20px' }}>
          <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', letterSpacing: '0.3em', color: '#475569', fontFamily: 'monospace' }}>
            {phase === 'plan' ? 'PLANNING' : phase === 'loading' ? 'SIMULATING' : 'WAVE'}
          </span>
          <span style={{
            fontSize: '1.4rem', fontWeight: 900, fontFamily: 'monospace',
            color: '#f1f5f9', letterSpacing: '0.1em',
            textShadow: '0 0 20px rgba(99,102,241,0.6)',
          }}>
            {phase === 'replay' ? `${wave} / ${waveCount}` : '—'}
          </span>
          {outcome && (
            <span style={{
              fontSize: '0.75rem', fontWeight: 800, letterSpacing: '0.15em',
              color: outcome === 'WIN' ? '#22c55e' : '#ef4444',
              textShadow: `0 0 12px ${outcome === 'WIN' ? '#22c55e' : '#ef4444'}`,
            }}>
              {outcome === 'WIN' ? '✓ VICTORY' : '✗ DEFEAT'}
            </span>
          )}
        </HUDPanel>

        {/* Gold — top right */}
        <HUDPanel>
          <HUDStat
            icon={<Coins size={16} />}
            value={String(gold)}
            label="Gold"
            color="#eab308"
            warning={goldWarn}
          />
          {divider}
          <button
            onClick={onReset}
            style={{
              background: 'none', border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: 6, padding: '4px 10px',
              color: '#94a3b8', fontSize: '0.72rem', cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: 4,
              transition: 'all 0.15s',
            }}
            onMouseEnter={e => (e.currentTarget.style.borderColor = 'rgba(255,255,255,0.3)')}
            onMouseLeave={e => (e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)')}
          >
            <RotateCcw size={12} /> Reset
          </button>
        </HUDPanel>
      </div>

      {/* ── Bottom: replay controls ──────────────────────────────────── */}
      {phase === 'replay' && (
        <div style={{
          position: 'absolute', bottom: 14, left: 14, right: 14,
          display: 'flex', alignItems: 'center', gap: 8,
          pointerEvents: 'auto',
        }}>
          <HUDPanel style={{ flex: 1, gap: 10 }}>
            {/* Play/Pause */}
            <button
              onClick={onPlay}
              style={{
                width: 32, height: 32, borderRadius: '50%',
                background: playing ? 'rgba(99,102,241,0.3)' : 'rgba(99,102,241,0.6)',
                border: '1px solid rgba(99,102,241,0.6)',
                color: '#c7d2fe', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              {playing ? <Pause size={14} /> : <Play size={14} />}
            </button>

            {/* Skip wave */}
            <button
              onClick={onSkip}
              title="Skip to next wave"
              style={{
                width: 28, height: 28, borderRadius: 6,
                background: 'rgba(255,255,255,0.05)',
                border: '1px solid rgba(255,255,255,0.1)',
                color: '#94a3b8', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <SkipForward size={13} />
            </button>

            {/* Scrub bar */}
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8 }}>
              <input
                type="range"
                min={0} max={Math.max(0, tickCount - 1)}
                value={tickIdx}
                onChange={e => onScrub(+e.target.value)}
                style={{ flex: 1, accentColor: '#6366f1', height: 4, cursor: 'pointer' }}
              />
              <span style={{ fontSize: '0.68rem', fontFamily: 'monospace', color: '#475569', minWidth: 70, textAlign: 'right' }}>
                {currentTick} / {maxTick}
              </span>
            </div>

            {/* Speed buttons */}
            <div style={{ display: 'flex', gap: 3 }}>
              {speedOptions.map((s, i) => (
                <button
                  key={s.label}
                  onClick={() => onSpeed(i)}
                  style={{
                    padding: '3px 8px', borderRadius: 4,
                    background: i === speedIdx ? 'rgba(99,102,241,0.5)' : 'rgba(255,255,255,0.05)',
                    border: `1px solid ${i === speedIdx ? 'rgba(99,102,241,0.7)' : 'rgba(255,255,255,0.1)'}`,
                    color: i === speedIdx ? '#c7d2fe' : '#64748b',
                    fontSize: '0.7rem', fontWeight: 700, cursor: 'pointer',
                    fontFamily: 'monospace',
                  }}
                >
                  {s.label}
                </button>
              ))}
            </div>

            {/* Tick indicator */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <Zap size={12} style={{ color: '#6366f1' }} />
              <span style={{ fontSize: '0.65rem', color: '#475569', fontFamily: 'monospace' }}>t{currentTick}</span>
            </div>
          </HUDPanel>
        </div>
      )}

      {/* ── Plan mode hint ───────────────────────────────────────────── */}
      {phase === 'plan' && (
        <div style={{
          position: 'absolute', bottom: 14, left: '50%', transform: 'translateX(-50%)',
          pointerEvents: 'none',
        }}>
          <HUDPanel style={{ gap: 6 }}>
            <span style={{ fontSize: '0.72rem', color: '#475569' }}>
              Select a tower from the sidebar · Click the grid to place · Run when ready
            </span>
          </HUDPanel>
        </div>
      )}
    </div>
  );
}
