import React, { useState } from 'react';
import { PenTool, Grid3X3, Route, Waves, Save, RotateCcw, Plus, Trash2 } from 'lucide-react';
import Card, { CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import styles from './LevelEditor.module.css';

interface WaveConfig {
  id: number;
  enemies: string;
  count: number;
  interval: number;
}

const ENEMY_TYPES = ['Grunt', 'Brute', 'Speeder', 'Tank', 'Hivemind', 'Void Reaper'];

const defaultWaves: WaveConfig[] = [
  { id: 1, enemies: 'Grunt', count: 20, interval: 0.5 },
  { id: 2, enemies: 'Grunt', count: 25, interval: 0.4 },
  { id: 3, enemies: 'Brute', count: 10, interval: 0.8 },
  { id: 4, enemies: 'Speeder', count: 30, interval: 0.3 },
  { id: 5, enemies: 'Grunt,Brute', count: 20, interval: 0.4 },
];

export default function LevelEditor() {
  const [mapName, setMapName] = useState('New Map');
  const [gridW, setGridW] = useState(20);
  const [gridH, setGridH] = useState(14);
  const [pathType, setPathType] = useState<'straight' | 'spiral' | 'zigzag' | 'custom'>('zigzag');
  const [waves, setWaves] = useState<WaveConfig[]>(defaultWaves);
  const [selectedWave, setSelectedWave] = useState<number | null>(null);

  function addWave() {
    const id = Math.max(...waves.map(w => w.id), 0) + 1;
    setWaves(w => [...w, { id, enemies: 'Grunt', count: 20, interval: 0.5 }]);
  }

  function removeWave(id: number) {
    setWaves(w => w.filter(wave => wave.id !== id));
  }

  function updateWave(id: number, field: keyof WaveConfig, value: string | number) {
    setWaves(w => w.map(wave => wave.id === id ? { ...wave, [field]: value } : wave));
  }

  return (
    <div className={styles.page}>
      {/* Page header */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <PenTool size={18} style={{ color: 'var(--color-accent-green)' }} />
          <h1 className={styles.title}>Level Editor</h1>
          <Badge variant="info">Draft</Badge>
        </div>
        <div className={styles.headerActions}>
          <Button variant="ghost" size="sm" icon={<RotateCcw size={14} />}>Reset</Button>
          <Button variant="primary" size="sm" icon={<Save size={14} />}>Save Level</Button>
        </div>
      </div>

      <div className={styles.mainGrid}>
        {/* Left: Config panels */}
        <div className={styles.configPanel}>
          {/* Map Config */}
          <Card>
            <CardHeader title="Map Configuration" icon={<Grid3X3 size={16} />} />
            <div className={styles.formGroup}>
              <label className={styles.label}>Map Name</label>
              <input
                className={styles.input}
                value={mapName}
                onChange={e => setMapName(e.target.value)}
                placeholder="Enter map name..."
              />
            </div>
            <div className={styles.row2}>
              <div className={styles.formGroup}>
                <label className={styles.label}>Grid Width</label>
                <div className={styles.rangeRow}>
                  <input type="range" min={10} max={30} value={gridW}
                    onChange={e => setGridW(+e.target.value)} className={styles.slider} />
                  <span className={`${styles.rangeVal} font-mono`}>{gridW}</span>
                </div>
              </div>
              <div className={styles.formGroup}>
                <label className={styles.label}>Grid Height</label>
                <div className={styles.rangeRow}>
                  <input type="range" min={8} max={20} value={gridH}
                    onChange={e => setGridH(+e.target.value)} className={styles.slider} />
                  <span className={`${styles.rangeVal} font-mono`}>{gridH}</span>
                </div>
              </div>
            </div>
          </Card>

          {/* Enemy Path */}
          <Card>
            <CardHeader title="Enemy Path" icon={<Route size={16} />} />
            <div className={styles.pathOptions}>
              {(['straight', 'spiral', 'zigzag', 'custom'] as const).map(type => (
                <button
                  key={type}
                  className={`${styles.pathOption} ${pathType === type ? styles.pathOptionActive : ''}`}
                  onClick={() => setPathType(type)}
                >
                  <span className={styles.pathIcon}>{
                    type === 'straight' ? '→' :
                    type === 'spiral' ? '🌀' :
                    type === 'zigzag' ? '⚡' : '✏️'
                  }</span>
                  <span className={styles.pathLabel}>{type.charAt(0).toUpperCase() + type.slice(1)}</span>
                </button>
              ))}
            </div>
            {pathType === 'custom' && (
              <p className={styles.hint}>Draw the path directly on the preview map by clicking cells.</p>
            )}
            <div className={styles.pathStats}>
              <div className={styles.pathStat}>
                <span className={styles.pathStatVal}>48</span>
                <span className={styles.pathStatLabel}>Path length</span>
              </div>
              <div className={styles.pathStat}>
                <span className={styles.pathStatVal}>3</span>
                <span className={styles.pathStatLabel}>Turns</span>
              </div>
              <div className={styles.pathStat}>
                <span className={styles.pathStatVal}>~2.4s</span>
                <span className={styles.pathStatLabel}>Avg transit</span>
              </div>
            </div>
          </Card>

          {/* Wave Config */}
          <Card>
            <CardHeader
              title="Wave Configuration"
              icon={<Waves size={16} />}
              action={<Button variant="secondary" size="sm" icon={<Plus size={12} />} onClick={addWave}>Add Wave</Button>}
            />
            <div className={styles.waveList}>
              {waves.map(wave => (
                <div
                  key={wave.id}
                  className={`${styles.waveRow} ${selectedWave === wave.id ? styles.waveRowActive : ''}`}
                  onClick={() => setSelectedWave(w => w === wave.id ? null : wave.id)}
                >
                  <div className={styles.waveHeader}>
                    <span className={styles.waveNum}>Wave {wave.id}</span>
                    <div className={styles.waveTags}>
                      {wave.enemies.split(',').map(e => (
                        <span key={e} className={styles.enemyTag}>{e.trim()}</span>
                      ))}
                    </div>
                    <span className={`font-mono ${styles.waveCount}`}>×{wave.count}</span>
                    <button
                      className={styles.removeBtn}
                      onClick={e => { e.stopPropagation(); removeWave(wave.id); }}
                      aria-label="Remove wave"
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>

                  {selectedWave === wave.id && (
                    <div className={styles.waveEditor} onClick={e => e.stopPropagation()}>
                      <div className={styles.waveField}>
                        <label className={styles.label}>Enemy Types</label>
                        <select
                          className={styles.select}
                          value={wave.enemies}
                          onChange={e => updateWave(wave.id, 'enemies', e.target.value)}
                        >
                          {ENEMY_TYPES.map(et => <option key={et} value={et}>{et}</option>)}
                          <option value="Grunt,Brute">Grunt + Brute</option>
                          <option value="Speeder,Grunt">Speeder + Grunt</option>
                        </select>
                      </div>
                      <div className={styles.row2}>
                        <div className={styles.waveField}>
                          <label className={styles.label}>Count</label>
                          <input
                            type="number" min={1} max={100} className={styles.input}
                            value={wave.count}
                            onChange={e => updateWave(wave.id, 'count', +e.target.value)}
                          />
                        </div>
                        <div className={styles.waveField}>
                          <label className={styles.label}>Interval (s)</label>
                          <input
                            type="number" min={0.1} max={5} step={0.1} className={styles.input}
                            value={wave.interval}
                            onChange={e => updateWave(wave.id, 'interval', +e.target.value)}
                          />
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </Card>
        </div>

        {/* Map Preview */}
        <div className={styles.previewPanel}>
          <Card style={{ height: '100%' }}>
            <CardHeader title="Map Preview" subtitle={`${gridW} × ${gridH} · ${pathType} path`} icon={<Grid3X3 size={16} />} />
            <div className={styles.previewGrid}
              style={{ gridTemplateColumns: `repeat(${gridW}, 1fr)` }}
            >
              {Array.from({ length: gridH }, (_, row) =>
                Array.from({ length: gridW }, (_, col) => {
                  // Simple zigzag path simulation
                  const isPath =
                    (pathType === 'straight' && row === Math.floor(gridH / 2)) ||
                    (pathType === 'zigzag' && (
                      (row === 2 && col < gridW * 0.4) ||
                      (col === Math.floor(gridW * 0.4) && row <= 2) ||
                      (row === Math.floor(gridH * 0.5) && col >= Math.floor(gridW * 0.4) && col <= Math.floor(gridW * 0.7)) ||
                      (col === Math.floor(gridW * 0.7) && row >= Math.floor(gridH * 0.5)) ||
                      (row === gridH - 2 && col >= Math.floor(gridW * 0.7))
                    )) ||
                    (pathType === 'spiral' && (
                      (row === 1 && col < gridW - 1) || (col === gridW - 2 && row < gridH - 1) ||
                      (row === gridH - 2 && col > 1) || (col === 1 && row > 2 && row < gridH - 2)
                    ));

                  return (
                    <div
                      key={`${col},${row}`}
                      className={`${styles.previewCell} ${isPath ? styles.previewPath : styles.previewGround}`}
                    />
                  );
                })
              )}
            </div>
            <div className={styles.previewLegend}>
              <div className={styles.legendItem}>
                <div className={styles.legendSwatch} style={{ background: 'rgba(100,70,20,0.5)' }} />
                <span>Enemy path</span>
              </div>
              <div className={styles.legendItem}>
                <div className={styles.legendSwatch} style={{ background: 'var(--color-bg-elevated)' }} />
                <span>Tower zone</span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
