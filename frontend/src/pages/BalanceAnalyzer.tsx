/**
 * Balance Analyzer — batch analytics screen.
 *
 * Sends POST /api/simulations/batch, displays BatchResult.
 * No game mechanics. All stats come from the API response.
 */
import React, { useEffect, useState } from 'react';
import {
  FlaskConical, TrendingUp, Shield, Loader2, Play, AlertTriangle, Info,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as RTooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';
import Card, { CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import { getCatalog } from '../api/catalog';
import { runBatch } from '../api/simulations';
import type { CatalogResult, BatchResult, TowerType } from '../types';
import styles from './BalanceAnalyzer.module.css';

// ─── Colour palette ───────────────────────────────────────────────────────────
const TOWER_COLORS: Record<TowerType, string> = {
  Cannon: '#ef4444',
  Sniper: '#eab308',
  Frost:  '#3b82f6',
};

function winRateClass(wr: number) {
  if (wr >= 0.75) return styles.winRateHigh;
  if (wr >= 0.4)  return styles.winRateMed;
  return styles.winRateLow;
}

function fmt(n: number, decimals = 1) {
  return n.toFixed(decimals);
}

// ─── Custom tooltip ───────────────────────────────────────────────────────────
function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className={styles.tooltip}>
      <div className={styles.tooltipLabel}>{label}</div>
      {payload.map((p: any) => (
        <div key={p.dataKey} className={styles.tooltipRow}>
          <span style={{ color: p.color }}>{p.name}:</span>
          <span>{typeof p.value === 'number' ? fmt(p.value) : p.value}</span>
        </div>
      ))}
    </div>
  );
}

// ─── Component ────────────────────────────────────────────────────────────────
export default function BalanceAnalyzer() {
  const [catalog, setCatalog]     = useState<CatalogResult | null>(null);
  const [runs, setRuns]           = useState(200);
  const [seed, setSeed]           = useState(42);
  const [enabledTowers, setEnabled] = useState<Set<TowerType>>(new Set(['Cannon', 'Sniper', 'Frost']));
  const [result, setResult]       = useState<BatchResult | null>(null);
  const [simRunning, setRunning]  = useState(false);
  const [simProgress, setProgress] = useState(0);
  const [error, setError]         = useState<string | null>(null);

  useEffect(() => {
    getCatalog().then(setCatalog).catch(e => setError(String(e)));
  }, []);

  function toggleTower(type: TowerType) {
    setEnabled(prev => {
      const next = new Set(prev);
      next.has(type) ? next.delete(type) : next.add(type);
      return next;
    });
  }

  async function handleRun() {
    setRunning(true);
    setProgress(0);
    setError(null);

    // Fake progress bar (real API has no streaming progress)
    const timer = setInterval(() => setProgress(p => Math.min(p + 2, 90)), 80);

    try {
      const res = await runBatch({
        levelId: 'level1',
        seed,
        runs,
        actions: [], // Towers are set by the engine from loadout — extend when API supports loadout param
      });
      clearInterval(timer);
      setProgress(100);
      setResult(res);
    } catch (e) {
      setError(String(e));
    } finally {
      clearInterval(timer);
      setRunning(false);
    }
  }

  // ── Derived chart data ────────────────────────────────────────────────────
  const contribData = result?.towerStats.map(ts => ({
    name: ts.towerType,
    contribution: +(ts.damageContribution * 100).toFixed(1),
    efficiency:   +ts.costEfficiency.toFixed(2),
    utilization:  +(ts.utilization * 100).toFixed(1),
  })) ?? [];

  const avgData = result ? [
    { name: 'Dmg Dealt',    value: +fmt(result.averages.damageDealt) },
    { name: 'Enemies',      value: +fmt(result.averages.enemiesDefeated) },
    { name: 'Leakage',      value: +fmt(result.averages.leakage) },
    { name: 'HP Remaining', value: +fmt(result.averages.baseHealthRemaining) },
  ] : [];

  // ─── Render ──────────────────────────────────────────────────────────────
  return (
    <div className={styles.page}>
      {/* ── Page Header ──────────────────────────────────────────────── */}
      <div className={styles.pageHeader}>
        <div className={styles.titleGroup}>
          <h1 className={styles.title}>
            <FlaskConical size={18} className={styles.titleIcon} />
            Balance Analyzer
          </h1>
          <span className={styles.subtitle}>
            Run hundreds of simulations and analyse tower performance
          </span>
        </div>
        <div className={styles.pills}>
          {result && (
            <Badge variant={result.winRate >= 0.75 ? 'success' : result.winRate >= 0.4 ? 'warning' : 'critical'}>
              {(result.winRate * 100).toFixed(0)}% Win Rate
            </Badge>
          )}
          <Badge variant="info">{runs} runs</Badge>
        </div>
      </div>

      {/* ── Top row: config + findings ──────────────────────────────── */}
      <div className={styles.topRow}>
        {/* Sim config */}
        <Card glow="purple">
          <CardHeader title="Simulation Config" icon={<FlaskConical size={16} />} />
          <div className={styles.simConfig}>
            <div className={styles.simField}>
              <label className={styles.label}>Level</label>
              <select className={styles.select}><option>level1</option></select>
            </div>

            <div className={styles.simField}>
              <label className={styles.label}>Seed</label>
              <input
                type="number" min={0} max={99999}
                value={seed}
                onChange={e => setSeed(+e.target.value)}
                className={styles.select}
              />
            </div>

            <div className={styles.simField}>
              <label className={styles.label}>Simulation Runs (1–1000)</label>
              <div className={styles.rangeRow}>
                <input
                  type="range" min={1} max={1000} step={1}
                  value={runs}
                  onChange={e => setRuns(+e.target.value)}
                  className={styles.slider}
                />
                <span className={styles.rangeVal}>{runs}</span>
              </div>
            </div>

            {/* Tower loadout */}
            <div className={styles.simField}>
              <div className={styles.loadoutTitle}>Tower Loadout</div>
              <div className={styles.loadoutRow}>
                {catalog?.towers.map(t => (
                  <label key={t.type} className={styles.loadoutItem}>
                    <input
                      type="checkbox"
                      className={styles.loadoutCheck}
                      checked={enabledTowers.has(t.type)}
                      onChange={() => toggleTower(t.type)}
                    />
                    <span className={styles.loadoutLabel}>{t.type}</span>
                    <span className={styles.loadoutCost}>{t.cost}g</span>
                  </label>
                ))}
              </div>
            </div>

            {simRunning && (
              <div className={styles.simProgress}>
                <div
                  className={styles.simProgressBar}
                  style={{ width: `${simProgress}%`, transition: 'width 0.15s ease' }}
                />
              </div>
            )}

            <div id="run-batch-btn">
            <Button
              variant="primary"
              fullWidth
              icon={simRunning
                ? <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} />
                : <Play size={14} />}
              onClick={handleRun}
              disabled={simRunning || enabledTowers.size === 0}
            >
              {simRunning
                ? `Simulating… ${Math.min(Math.round(simProgress), 100)}%`
                : `Run Batch (${runs} runs)`}
            </Button>
            </div>

            {error && (
              <div style={{ color: 'var(--color-accent-red)', fontSize: '0.75rem' }}>{error}</div>
            )}
          </div>
        </Card>

        {/* Findings */}
        <Card>
          <CardHeader
            title="Findings"
            icon={<AlertTriangle size={16} />}
            subtitle={result ? `${result.findings.length} recommendations` : 'Run a batch to generate findings'}
          />
          <div className={styles.findingsList}>
            {!result && (
              <span className={styles.emptyFindings}>No results yet — run a simulation above.</span>
            )}
            {result?.findings.length === 0 && (
              <span className={styles.emptyFindings}>No findings — the loadout looks balanced.</span>
            )}
            {result?.findings.map((f, i) => (
              <div key={i} className={styles.findingRow}>
                <span className={styles.findingRule}>{f.rule}</span>
                <span className={styles.findingMetric}>metric: {f.metricValue}</span>
                <span className={styles.findingAction}>→ {f.suggestedAction}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* ── Results ──────────────────────────────────────────────────── */}
      {result && (
        <>
          {/* Summary + averages */}
          <div className={styles.resultsRow}>
            <Card glow="blue">
              <CardHeader title="Run Summary" icon={<TrendingUp size={16} />} />
              <div className={styles.summaryGrid}>
                <div className={[styles.summaryCell, styles.summaryCellFull].join(' ')}>
                  <span className={styles.summaryLabel}>Win Rate</span>
                  <span className={[styles.winRatePill, winRateClass(result.winRate)].join(' ')}>
                    {(result.winRate * 100).toFixed(1)}%
                  </span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{result.runs}</span>
                  <span className={styles.summaryLabel}>Total Runs</span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{fmt(result.averages.durationTicks)}</span>
                  <span className={styles.summaryLabel}>Avg Ticks</span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{fmt(result.averages.enemiesDefeated)}</span>
                  <span className={styles.summaryLabel}>Avg Enemies Defeated</span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{fmt(result.averages.leakage)}</span>
                  <span className={styles.summaryLabel}>Avg Leakage</span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{fmt(result.averages.baseHealthRemaining)}</span>
                  <span className={styles.summaryLabel}>Avg HP Remaining</span>
                </div>
                <div className={styles.summaryCell}>
                  <span className={styles.summaryValue}>{fmt(result.averages.damageDealt)}</span>
                  <span className={styles.summaryLabel}>Avg Dmg Dealt</span>
                </div>
              </div>
            </Card>

            {/* Averages bar chart */}
            <Card>
              <CardHeader title="Averages" subtitle="Per-run metrics" icon={<TrendingUp size={16} />} />
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={avgData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(26,42,66,0.8)" />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} />
                  <RTooltip content={<ChartTooltip />} />
                  <Bar dataKey="value" fill="var(--color-accent-purple)" radius={[3,3,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </div>

          {/* Tower stats */}
          <Card>
            <CardHeader
              title="Tower Performance"
              icon={<Shield size={16} />}
              subtitle="Only built tower types are shown, sorted by name"
            />
            <div className={styles.tableWrap}>
              <table className={styles.table} id="tower-stats-table">
                <thead>
                  <tr>
                    <th>Tower</th>
                    <th>Damage Contribution</th>
                    <th>Cost Efficiency (dmg/gold)</th>
                    <th>Utilization</th>
                  </tr>
                </thead>
                <tbody>
                  {result.towerStats.map(ts => {
                    const color = TOWER_COLORS[ts.towerType] ?? '#94a3b8';
                    return (
                      <tr key={ts.towerType} className={styles.towerRow}>
                        <td>
                          <span style={{ background: `${color}22`, color, border: `1px solid ${color}44`, padding: '2px 8px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 600 }}>
                            {ts.towerType}
                          </span>
                        </td>
                        <td>
                          <div className={styles.contribBar}>
                            <div className={styles.barBg}>
                              <div
                                className={styles.barFill}
                                style={{ width: `${ts.damageContribution * 100}%`, background: color }}
                              />
                            </div>
                            <span className={styles.barLabel}>
                              {(ts.damageContribution * 100).toFixed(1)}%
                            </span>
                          </div>
                        </td>
                        <td>{ts.costEfficiency.toFixed(2)}</td>
                        <td>
                          <div className={styles.contribBar}>
                            <div className={styles.barBg}>
                              <div
                                className={styles.barFill}
                                style={{ width: `${ts.utilization * 100}%`, background: 'var(--color-accent-cyan)' }}
                              />
                            </div>
                            <span className={styles.barLabel}>
                              {(ts.utilization * 100).toFixed(1)}%
                            </span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className={styles.helpText}>
              <Info size={12} style={{ display:'inline', marginRight:4, verticalAlign:'middle' }} />
              Frost's cost efficiency (damage per gold) appears low because its primary value is the
              slow effect, which this metric does not measure. Consider it an enabler for Cannon and Sniper.
            </div>
          </Card>

          {/* Charts row */}
          <div className={styles.chartsRow}>
            {/* Damage contribution pie */}
            <Card>
              <CardHeader title="Damage Contribution" subtitle="% share by tower type" icon={<Shield size={16} />} />
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={contribData} dataKey="contribution"
                    nameKey="name" cx="50%" cy="50%" outerRadius={80}
                    label={({ name, contribution }) => `${name} ${contribution}%`}
                    labelLine={false}
                  >
                    {contribData.map(d => (
                      <Cell key={d.name} fill={TOWER_COLORS[d.name as TowerType] ?? '#94a3b8'} />
                    ))}
                  </Pie>
                  <Legend wrapperStyle={{ fontSize: 11, color: 'var(--color-text-secondary)' }} />
                  <RTooltip content={<ChartTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            </Card>

            {/* Utilization bar chart */}
            <Card>
              <CardHeader title="Tower Utilization" subtitle="% of time actively attacking" icon={<TrendingUp size={16} />} />
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={contribData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(26,42,66,0.8)" />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} domain={[0,100]} unit="%" />
                  <RTooltip content={<ChartTooltip />} />
                  <Bar dataKey="utilization" radius={[3,3,0,0]}>
                    {contribData.map(d => (
                      <Cell key={d.name} fill={TOWER_COLORS[d.name as TowerType] ?? '#94a3b8'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
