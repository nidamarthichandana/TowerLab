/**
 * OutcomeSplash.tsx
 * Full-screen VICTORY / DEFEAT fanfare overlay.
 */
import React, { useEffect, useState } from 'react';

interface OutcomeSplashProps {
  outcome: 'WIN' | 'LOSS' | undefined;
  onDismiss: () => void;
}

export default function OutcomeSplash({ outcome, onDismiss }: OutcomeSplashProps) {
  const [visible, setVisible] = useState(false);
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    if (!outcome) { setVisible(false); return; }
    setExiting(false);
    setVisible(true);
  }, [outcome]);

  if (!visible || !outcome) return null;

  const isWin = outcome === 'WIN';
  const primary = isWin ? '#22c55e' : '#ef4444';
  const secondary = isWin ? '#86efac' : '#fca5a5';
  const title = isWin ? 'VICTORY' : 'DEFEAT';
  const sub   = isWin ? 'All waves repelled. Your defenses held.' : 'The enemy breached your base.';
  const emoji = isWin ? '🏆' : '💀';

  function handleDismiss() {
    setExiting(true);
    setTimeout(() => { setVisible(false); onDismiss(); }, 400);
  }

  return (
    <div
      style={{
        position: 'absolute', inset: 0, zIndex: 100,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: `radial-gradient(ellipse at center, ${isWin ? 'rgba(0,40,20,0.92)' : 'rgba(40,0,0,0.92)'} 0%, rgba(2,8,18,0.96) 100%)`,
        animation: exiting ? 'splashOut 0.4s ease forwards' : 'splashIn 0.5s cubic-bezier(0,0,0.2,1) forwards',
        cursor: 'pointer',
      }}
      onClick={handleDismiss}
    >
      {/* Radial glow */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        background: `radial-gradient(circle at 50% 50%, ${primary}22 0%, transparent 60%)`,
      }} />

      {/* Horizontal sweep lines */}
      {[0.3, 0.5, 0.7].map(y => (
        <div key={y} style={{
          position: 'absolute', top: `${y * 100}%`, left: 0, right: 0,
          height: 1, background: `linear-gradient(90deg, transparent, ${primary}40, transparent)`,
          pointerEvents: 'none',
        }} />
      ))}

      {/* Main card */}
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16,
        padding: '48px 72px',
        border: `1px solid ${primary}44`,
        borderRadius: 12,
        background: `rgba(5,10,20,0.6)`,
        backdropFilter: 'blur(12px)',
        boxShadow: `0 0 60px ${primary}33, inset 0 0 40px rgba(0,0,0,0.5)`,
        animation: exiting ? 'cardOut 0.4s ease forwards' : 'cardIn 0.5s cubic-bezier(0,0,0.2,1) forwards',
      }}>
        {/* Top accent */}
        <div style={{
          position: 'absolute', top: -1, left: '20%', right: '20%', height: 2,
          background: `linear-gradient(90deg, transparent, ${primary}, transparent)`,
        }} />

        <div style={{ fontSize: 'clamp(2.5rem, 6vw, 5rem)', lineHeight: 1 }}>{emoji}</div>

        <div style={{
          fontSize: 'clamp(2.5rem, 7vw, 5.5rem)',
          fontWeight: 900,
          fontFamily: '"Orbitron", monospace',
          letterSpacing: '0.12em',
          color: '#fff',
          textShadow: `0 0 40px ${primary}, 0 0 80px ${primary}66`,
          lineHeight: 1,
        }}>
          {title}
        </div>

        <div style={{
          fontSize: '0.9rem', letterSpacing: '0.08em',
          color: secondary, opacity: 0.8,
          textAlign: 'center', maxWidth: 320,
        }}>
          {sub}
        </div>

        {/* Stats row */}
        <div style={{
          display: 'flex', gap: 24, marginTop: 8,
          padding: '12px 24px', borderRadius: 8,
          background: 'rgba(0,0,0,0.3)',
          border: `1px solid ${primary}22`,
        }}>
          {[
            { label: 'Result', value: isWin ? 'WIN' : 'LOSS' },
            { label: 'Level',  value: '1' },
          ].map(({ label, value }) => (
            <div key={label} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <span style={{ fontSize: '1.1rem', fontWeight: 800, fontFamily: 'monospace', color: primary }}>
                {value}
              </span>
              <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: '#475569' }}>
                {label}
              </span>
            </div>
          ))}
        </div>

        <div style={{ fontSize: '0.68rem', color: '#334155', marginTop: 4, letterSpacing: '0.08em' }}>
          Click anywhere to continue
        </div>

        {/* Bottom accent */}
        <div style={{
          position: 'absolute', bottom: -1, left: '20%', right: '20%', height: 2,
          background: `linear-gradient(90deg, transparent, ${primary}, transparent)`,
        }} />
      </div>

      <style>{`
        @keyframes splashIn  { from { opacity: 0 } to { opacity: 1 } }
        @keyframes splashOut { from { opacity: 1 } to { opacity: 0 } }
        @keyframes cardIn    { from { opacity: 0; transform: scale(0.85) translateY(20px) } to { opacity: 1; transform: scale(1) translateY(0) } }
        @keyframes cardOut   { from { opacity: 1; transform: scale(1) } to { opacity: 0; transform: scale(1.05) } }
      `}</style>
    </div>
  );
}
