/**
 * WaveAnnouncement.tsx — fixed: no spurious wave-0 trigger.
 */
import React, { useEffect, useRef, useState } from 'react';

interface WaveAnnouncementProps {
  wave:  number;   // wave number being announced (1-based)
  total: number;
  show:  boolean;  // parent pulses this true for one render
}

export default function WaveAnnouncement({ wave, total, show }: WaveAnnouncementProps) {
  const [visible, setVisible] = useState(false);
  const [exiting, setExiting] = useState(false);
  const shownFor = useRef(-1);

  useEffect(() => {
    // Guard: only show for valid waves (>0) and only once per wave number
    if (!show || wave <= 0 || wave === shownFor.current) return;
    shownFor.current = wave;
    setExiting(false);
    setVisible(true);
    const hide = setTimeout(() => setExiting(true),  1600);
    const done = setTimeout(() => setVisible(false), 2100);
    return () => { clearTimeout(hide); clearTimeout(done); };
  }, [show, wave]);

  if (!visible) return null;

  const isLast = wave === total;
  const color  = isLast ? '#f97316' : '#ef4444';

  return (
    <div style={{
      position: 'absolute', inset: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      pointerEvents: 'none', zIndex: 50,
      animation: exiting ? 'waveOut 0.5s ease forwards' : 'waveIn 0.4s cubic-bezier(0,0,0.2,1) forwards',
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(90deg, transparent 0%, rgba(0,0,0,0.75) 25%, rgba(0,0,0,0.75) 75%, transparent 100%)',
      }} />

      <div style={{
        position: 'relative', display: 'flex', flexDirection: 'column',
        alignItems: 'center', gap: 4, padding: '18px 60px',
        borderTop: `2px solid ${color}80`,
        borderBottom: `2px solid ${color}80`,
        background: 'rgba(5,5,10,0.85)',
      }}>
        <div style={{
          position: 'absolute', top: -5, left: '5%', right: '5%', height: 2,
          background: `linear-gradient(90deg, transparent, ${color}, transparent)`,
        }} />

        <span style={{
          fontSize: '0.65rem', letterSpacing: '0.4em', textTransform: 'uppercase',
          color, fontFamily: 'monospace', fontWeight: 700,
        }}>
          {isLast ? '⚠ FINAL ASSAULT' : '⚠ INCOMING WAVE'}
        </span>

        <div style={{
          fontSize: 'clamp(1.8rem, 4vw, 3rem)',
          fontWeight: 900,
          fontFamily: '"Orbitron", "Share Tech Mono", monospace',
          letterSpacing: '0.1em', color: '#fff', lineHeight: 1,
          textShadow: `0 0 24px ${color}cc, 0 0 48px ${color}66`,
        }}>
          WAVE {wave} / {total}
        </div>

        <span style={{
          fontSize: '0.6rem', letterSpacing: '0.25em',
          color: '#94a3b8', textTransform: 'uppercase', fontFamily: 'monospace',
        }}>
          {isLast ? 'Last stand — hold the line' : 'Prepare your defenses'}
        </span>

        <div style={{
          position: 'absolute', bottom: -5, left: '5%', right: '5%', height: 2,
          background: `linear-gradient(90deg, transparent, ${color}, transparent)`,
        }} />
      </div>

      <style>{`
        @keyframes waveIn  { from { opacity:0; transform:scale(0.9) translateY(8px) } to { opacity:1; transform:scale(1) translateY(0) } }
        @keyframes waveOut { from { opacity:1; transform:scale(1) } to { opacity:0; transform:scale(1.04) translateY(-6px) } }
      `}</style>
    </div>
  );
}
