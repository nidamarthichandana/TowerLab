import React, { useEffect, useState } from 'react';
import {
  FileText, Download, Trash2, Info,
  MousePointerClick, CheckCircle2, Loader2, Scale,
} from 'lucide-react';
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis,
  PolarRadiusAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';
import Card, { CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Badge, { mapToBadgeVariant } from '../components/ui/Badge';
import { api } from '../services/api';
import type { SimulationReport } from '../types';
import styles from './Reports.module.css';

export default function Reports() {
  const [reports, setReports] = useState<SimulationReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    api.getSimulationReports().then(r => {
      setReports(r);
      setLoading(false);
    });
  }, []);

  function toggleSelect(id: string) {
    setSelected(prev => {
      if (prev.includes(id)) return prev.filter(x => x !== id);
      if (prev.length >= 2) return [prev[1], id]; // max 2
      return [...prev, id];
    });
  }

  function handleDelete(id: string, e: React.MouseEvent) {
    e.stopPropagation();
    setReports(prev => prev.filter(r => r.id !== id));
    setSelected(prev => prev.filter(x => x !== id));
  }

  if (loading) return (
    <div className={styles.loading}>
      <Loader2 size={18} style={{ animation: 'spin 1s linear infinite', marginRight: 8 }} />
      Loading reports...
    </div>
  );

  const [rep1, rep2] = selected.map(id => reports.find(r => r.id === id)!);

  // Build radar data for comparison
  const radarData = rep1 ? [
    { metric: 'Win Rate',    r1: rep1.winRate ?? 0,                           r2: rep2?.winRate ?? 0 },
    { metric: 'Runs (×100)', r1: Math.round((rep1.runs / 2000) * 10),         r2: Math.round(((rep2?.runs ?? 0) / 2000) * 10) },
    { metric: 'Avg Waves',   r1: Math.round(rep1.avgWaves),                   r2: Math.round(rep2?.avgWaves ?? 0) },
    { metric: 'Speed',       r1: 7,                                            r2: 5 },
    { metric: 'Stability',   r1: 8,                                            r2: 6 },
  ] : [];

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <FileText size={20} style={{ color: 'var(--color-accent-blue)' }} />
          <h1 className={styles.title}>Simulation Reports</h1>
          <span className={styles.count}>{reports.length} reports</span>
        </div>
        <div className={styles.headerActions}>
          <Button variant="ghost" size="sm" icon={<Download size={14} />}>Export CSV</Button>
          <Button variant="secondary" size="sm" icon={<Scale size={14} />} disabled={selected.length !== 2}>
            Compare Selected
          </Button>
        </div>
      </div>

      <div className={styles.mainGrid}>
        {/* Report List */}
        <div>
          <div className={styles.hint}>
            <MousePointerClick size={12} />
            Select up to 2 reports to compare side-by-side
          </div>
          <div className={styles.reportList}>
            {reports.map(report => {
              const isSelected = selected.includes(report.id);
              const isRunning = report.status === 'running';
              return (
                <div
                  key={report.id}
                  className={[
                    styles.reportCard,
                    isSelected ? styles.reportSelected : '',
                    isRunning  ? styles.reportRunning  : '',
                  ].join(' ')}
                  onClick={() => !isRunning && toggleSelect(report.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => e.key === 'Enter' && !isRunning && toggleSelect(report.id)}
                  aria-pressed={isSelected}
                >
                  {/* Header */}
                  <div className={styles.reportHeader}>
                    <div>
                      <div className={styles.reportTitle}>{report.title}</div>
                      <div className={styles.reportDate}>{report.date}</div>
                    </div>
                    <div className={styles.reportHeaderRight}>
                      <Badge variant={mapToBadgeVariant(report.status)}>
                        {report.status}
                      </Badge>
                      {isSelected && <span className={styles.checkmark}>✓</span>}
                    </div>
                  </div>

                  {/* Running bar */}
                  {isRunning && (
                    <div className={styles.runningBar}>
                      <Loader2 size={12} style={{ animation: 'spin 1s linear infinite', flexShrink: 0 }} />
                      <span>Simulation in progress…</span>
                      <div className={styles.runningPulse} />
                    </div>
                  )}

                  {/* Stats */}
                  <div className={styles.reportStats}>
                    <div className={styles.reportStat}>
                      <span className={`${styles.reportStatVal} ${report.winRate >= 50 ? styles.valGreen : styles.valRed}`}>
                        {report.winRate > 0 ? `${report.winRate}%` : '—'}
                      </span>
                      <span className={styles.reportStatLabel}>Win Rate</span>
                    </div>
                    <div className={styles.reportStat}>
                      <span className={styles.reportStatVal}>{report.runs.toLocaleString()}</span>
                      <span className={styles.reportStatLabel}>Runs</span>
                    </div>
                    <div className={styles.reportStat}>
                      <span className={`${styles.reportStatVal} ${report.avgWaves > 0 ? styles.valGreen : ''}`}>
                        {report.avgWaves > 0 ? report.avgWaves.toFixed(1) : '—'}
                      </span>
                      <span className={styles.reportStatLabel}>Avg Waves</span>
                    </div>
                    <div className={styles.reportStat}>
                      <span className={styles.reportStatVal}>{report.topTower}</span>
                      <span className={styles.reportStatLabel}>Top Tower</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className={styles.reportActions}>
                    <Button variant="secondary" size="sm" icon={<Download size={12} />}>Export</Button>
                    <Button variant="ghost" size="sm" icon={<Info size={12} />}>Details</Button>
                    <Button
                      variant="danger"
                      size="sm"
                      icon={<Trash2 size={12} />}
                      onClick={(e) => handleDelete(report.id, e)}
                    >Delete</Button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Compare Panel */}
        <div className={styles.comparePanel}>
          <Card glow="blue">
            <CardHeader
              title={selected.length === 2 ? 'Side-by-Side Comparison' : 'Compare Reports'}
              icon={<Scale size={16} />}
              subtitle={selected.length < 2 ? `Select ${2 - selected.length} more report${selected.length === 0 ? 's' : ''}` : 'Head-to-head analysis'}
            />
            {selected.length < 2 ? (
              <div className={styles.emptyCompare}>
                <CheckCircle2 size={32} style={{ opacity: 0.15 }} />
                <p>
                  {selected.length === 0
                    ? 'Click on any two reports in the list to compare them here.'
                    : 'Select one more report to start comparison.'}
                </p>
              </div>
            ) : (
              <>
                {/* Radar chart */}
                <ResponsiveContainer width="100%" height={220}>
                  <RadarChart data={radarData}>
                    <PolarGrid stroke="rgba(26,42,66,0.8)" />
                    <PolarAngleAxis dataKey="metric" tick={{ fontSize: 11, fill: '#4a6180' }} />
                    <PolarRadiusAxis domain={[0, 10]} tick={false} axisLine={false} />
                    <Tooltip
                      contentStyle={{ background: 'var(--color-bg-elevated)', border: '1px solid var(--color-border)', borderRadius: 8, fontSize: 12 }}
                    />
                    <Radar name={rep1.title} dataKey="r1" stroke="#2d8ef5" fill="#2d8ef5" fillOpacity={0.15} strokeWidth={2} />
                    {rep2 && <Radar name={rep2.title} dataKey="r2" stroke="#10b981" fill="#10b981" fillOpacity={0.1} strokeWidth={2} />}
                    <Legend wrapperStyle={{ fontSize: 10, color: 'var(--color-text-secondary)' }} />
                  </RadarChart>
                </ResponsiveContainer>

                {/* Head to head rows */}
                <div className={styles.h2hGrid}>
                  {[
                    { label: 'Win Rate',  v1: `${rep1.winRate}%`, v2: `${rep2?.winRate ?? 0}%`, win: rep1.winRate > (rep2?.winRate ?? 0) ? 1 : 2 },
                    { label: 'Sim Runs', v1: rep1.runs.toLocaleString(), v2: (rep2?.runs ?? 0).toLocaleString(), win: rep1.runs > (rep2?.runs ?? 0) ? 1 : 2 },
                    { label: 'Avg Waves', v1: rep1.avgWaves.toFixed(1), v2: (rep2?.avgWaves ?? 0).toFixed(1), win: rep1.avgWaves > (rep2?.avgWaves ?? 0) ? 1 : 2 },
                    { label: 'Top Tower', v1: rep1.topTower, v2: rep2?.topTower ?? '—', win: 0 },
                  ].map(({ label, v1, v2, win }) => (
                    <div key={label} className={styles.h2hRow}>
                      <span className={`${styles.h2hVal} ${win === 1 ? styles.h2hWinner : ''}`}>{v1}</span>
                      <span className={styles.h2hLabel}>{label}</span>
                      <span className={`${styles.h2hVal} ${win === 2 ? styles.h2hWinner : ''}`}>{v2}</span>
                    </div>
                  ))}
                  <div className={styles.h2hNames}>
                    <span>Report A</span>
                    <span>Report B</span>
                  </div>
                </div>
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
