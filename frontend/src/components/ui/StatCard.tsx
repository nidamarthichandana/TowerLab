import React from 'react';
import styles from './StatCard.module.css';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: { value: number; label: string };
  color?: 'blue' | 'purple' | 'green' | 'amber' | 'cyan';
  suffix?: string;
}

export default function StatCard({ label, value, icon, trend, color = 'blue', suffix }: StatCardProps) {
  return (
    <div className={`${styles.card} ${styles[color]}`}>
      <div className={styles.top}>
        <div className={styles.iconWrap}>
          {icon}
        </div>
        {trend && (
          <div className={`${styles.trend} ${trend.value >= 0 ? styles.trendUp : styles.trendDown}`}>
            {trend.value >= 0 ? '↑' : '↓'} {Math.abs(trend.value)}%
          </div>
        )}
      </div>
      <div className={styles.value}>
        {typeof value === 'number' ? value.toLocaleString() : value}
        {suffix && <span className={styles.suffix}>{suffix}</span>}
      </div>
      <div className={styles.label}>{label}</div>
      {trend && <div className={styles.trendLabel}>{trend.label}</div>}
    </div>
  );
}
