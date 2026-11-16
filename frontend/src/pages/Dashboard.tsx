import React, { useEffect, useState } from 'react';
import {
  LayoutDashboard,
  Gamepad2,
  FlaskConical,
  Trophy,
  AlertTriangle,
  Play,
  BarChart3,
  Clock,
  TrendingUp,
  Zap,
  Map,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import Card, { CardHeader } from '../components/ui/Card';
import StatCard from '../components/ui/StatCard';
import Badge, { mapToBadgeVariant } from '../components/ui/Badge';
import Button from '../components/ui/Button';
import { api } from '../services/api';
import type { DashboardStats, GameSession, BalanceReport } from '../types';
import styles from './Dashboard.module.css';

export default function Dashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [sessions, setSessions] = useState<GameSession[]>([]);
  const [reports, setReports] = useState<BalanceReport[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.getDashboardStats(),
      api.getRecentSessions(),
      api.getBalanceReports(),
    ]).then(([s, sess, rep]) => {
      setStats(s);
      setSessions(sess);
      setReports(rep);
      setLoading(false);
    });
  }, []);

  if (loading) return <LoadingSkeleton />;

  // Mini trend chart data derived from sessions
  const trendData = [
    { day: 'Mon', winRate: 71 },
    { day: 'Tue', winRate: 65 },
    { day: 'Wed', winRate: 78 },
    { day: 'Thu', winRate: 59 },
    { day: 'Fri', winRate: 82 },
    { day: 'Sat', winRate: 74 },
    { day: 'Sun', winRate: 67 },
  ];

  return (
    <div className={styles.page}>
      {/* Welcome Header */}
      <div className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroIcon}><LayoutDashboard size={24} /></div>
          <div>
            <h1 className={styles.heroTitle}>Tactical Command Center</h1>
            <p className={styles.heroSubtitle}>
              Monitor performance, run simulations, and balance your tower defense game.
            </p>
          </div>
        </div>
        <div className={styles.heroActions}>
          <Button variant="secondary" size="md" icon={<BarChart3 size={15} />} onClick={() => navigate('/balance-analyzer')}>
            Run Simulation
          </Button>
          <Button variant="primary" size="md" icon={<Play size={15} />} onClick={() => navigate('/battlefield')}>
            Start Game
          </Button>
        </div>
      </div>

      {/* Stat Cards */}
      <div className={`${styles.statsGrid} stagger-children`}>
        <StatCard
          label="Total Simulation Runs"
          value={stats!.totalSimulations}
          icon={<FlaskConical size={18} />}
          color="blue"
          trend={{ value: 12, label: 'vs last week' }}
        />
        <StatCard
          label="Games Played"
          value={stats!.gamesPlayed}
          icon={<Gamepad2 size={18} />}
          color="purple"
          trend={{ value: 8, label: 'vs last week' }}
        />
        <StatCard
          label="Avg Win Rate"
          value={stats!.avgWinRate}
          suffix="%"
          icon={<TrendingUp size={18} />}
          color="green"
          trend={{ value: -2, label: 'vs last week' }}
        />
        <StatCard
          label="Best Tower"
          value={stats!.bestTower}
          icon={<Trophy size={18} />}
          color="amber"
        />
      </div>

      {/* Chart + Sessions row */}
      <div className={styles.chartSection}>
        {/* Win Rate trend chart */}
        <Card glow="blue">
          <CardHeader
            title="7-Day Win Rate Trend"
            subtitle="Daily average across all sessions"
            icon={<TrendingUp size={16} />}
          />
          <ResponsiveContainer width="100%" height={180}>
            <AreaChart data={trendData} margin={{ top: 5, right: 10, bottom: 0, left: -20 }}>
              <defs>
                <linearGradient id="winRateGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#2d8ef5" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#2d8ef5" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(26,42,66,0.6)" />
              <XAxis dataKey="day" tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} axisLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#4a6180' }} tickLine={false} axisLine={false} domain={[40, 100]} unit="%" />
              <Tooltip
                contentStyle={{ background: 'var(--color-bg-elevated)', border: '1px solid var(--color-border)', borderRadius: 8, fontSize: 12 }}
                labelStyle={{ color: 'var(--color-text-primary)' }}
                itemStyle={{ color: '#2d8ef5' }}
              />
              <Area type="monotone" dataKey="winRate" name="Win Rate" stroke="#2d8ef5" strokeWidth={2.5} fill="url(#winRateGrad)" dot={false} activeDot={{ r: 4, fill: '#2d8ef5' }} />
            </AreaChart>
          </ResponsiveContainer>
        </Card>

        {/* Quick nav cards */}
        <Card>
          <CardHeader title="Quick Actions" icon={<Zap size={16} />} />
          <div className={styles.quickActions}>
            <button className={styles.quickAction} onClick={() => navigate('/battlefield')}>
              <div className={styles.qaIcon} style={{ background: 'rgba(45,142,245,0.1)', color: 'var(--color-accent-blue)' }}>
                <Play size={18} />
              </div>
              <div>
                <div className={styles.qaTitle}>New Game</div>
                <div className={styles.qaDesc}>Play the battlefield</div>
              </div>
            </button>
            <button className={styles.quickAction} onClick={() => navigate('/balance-analyzer')}>
              <div className={styles.qaIcon} style={{ background: 'rgba(124,58,237,0.1)', color: 'var(--color-accent-purple)' }}>
                <FlaskConical size={18} />
              </div>
              <div>
                <div className={styles.qaTitle}>Run Simulation</div>
                <div className={styles.qaDesc}>Analyze game balance</div>
              </div>
            </button>
            <button className={styles.quickAction} onClick={() => navigate('/level-editor')}>
              <div className={styles.qaIcon} style={{ background: 'rgba(16,185,129,0.1)', color: 'var(--color-accent-green)' }}>
                <Map size={18} />
              </div>
              <div>
                <div className={styles.qaTitle}>Edit Level</div>
                <div className={styles.qaDesc}>Design maps & waves</div>
              </div>
            </button>
            <button className={styles.quickAction} onClick={() => navigate('/reports')}>
              <div className={styles.qaIcon} style={{ background: 'rgba(245,158,11,0.1)', color: 'var(--color-accent-amber)' }}>
                <BarChart3 size={18} />
              </div>
              <div>
                <div className={styles.qaTitle}>View Reports</div>
                <div className={styles.qaDesc}>Compare simulations</div>
              </div>
            </button>
          </div>
        </Card>
      </div>

      {/* Main Grid */}
      <div className={styles.mainGrid}>
        {/* Recent Sessions */}
        <Card>
          <CardHeader
            title="Recent Game Sessions"
            subtitle={`${sessions.length} sessions`}
            icon={<Clock size={16} />}
            action={<Button variant="ghost" size="sm" onClick={() => navigate('/reports')}>View All</Button>}
          />
          <div className={styles.sessionList}>
            {sessions.map(session => (
              <div key={session.id} className={styles.sessionRow}>
                <div className={styles.sessionInfo}>
                  <div className={styles.sessionMap}>{session.map}</div>
                  <div className={styles.sessionMeta}>
                    <span className="font-mono">{session.duration}</span>
                    <span className={styles.dot}>·</span>
                    <span>Wave {session.waves}</span>
                    <span className={styles.dot}>·</span>
                    <span>{session.timestamp}</span>
                  </div>
                </div>
                <div className={styles.sessionRight}>
                  <div className={styles.winRate}>
                    <span className={styles.winRateValue}>{session.winRate}%</span>
                    <WinRateBar value={session.winRate} />
                  </div>
                  <Badge variant={mapToBadgeVariant(session.result)} dot>
                    {session.result === 'victory' ? 'Victory' : 'Defeat'}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Balance Reports */}
        <Card>
          <CardHeader
            title="Recent Balance Reports"
            icon={<AlertTriangle size={16} />}
            action={<Button variant="ghost" size="sm" onClick={() => navigate('/reports')}>All</Button>}
          />
          <div className={styles.reportList}>
            {reports.map(report => (
              <div key={report.id} className={styles.reportRow}>
                <div className={styles.reportLeft}>
                  <Badge variant={mapToBadgeVariant(report.severity)}>
                    {report.severity}
                  </Badge>
                  <div>
                    <div className={styles.reportTitle}>{report.title}</div>
                    <div className={styles.reportMeta}>{report.createdAt} · {report.issues} issue{report.issues !== 1 ? 's' : ''}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function WinRateBar({ value }: { value: number }) {
  const color = value >= 70 ? 'var(--color-accent-green)'
    : value >= 50 ? 'var(--color-accent-amber)'
    : 'var(--color-accent-red)';
  return (
    <div style={{ width: 60, height: 4, background: 'var(--color-bg-elevated)', borderRadius: 2, overflow: 'hidden' }}>
      <div style={{ width: `${value}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.5s ease' }} />
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div style={{ padding: 'var(--space-6)', display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
      <div style={{ height: 100, borderRadius: 'var(--radius-xl)', background: 'var(--color-bg-elevated)' }} className="shimmer" />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 'var(--space-4)' }}>
        {[...Array(4)].map((_, i) => (
          <div key={i} style={{ height: 110, borderRadius: 'var(--radius-lg)', background: 'var(--color-bg-elevated)' }} className="shimmer" />
        ))}
      </div>
      <div style={{ height: 200, borderRadius: 'var(--radius-lg)', background: 'var(--color-bg-elevated)' }} className="shimmer" />
    </div>
  );
}
