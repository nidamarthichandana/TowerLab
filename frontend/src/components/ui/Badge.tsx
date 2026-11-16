import React from 'react';
import styles from './Badge.module.css';

type BadgeVariant = 'info' | 'success' | 'warning' | 'critical' | 'neutral' | 'running';

interface BadgeProps {
  variant: BadgeVariant;
  children: React.ReactNode;
  dot?: boolean;
}

const labels: Record<string, BadgeVariant> = {
  victory: 'success',
  defeat: 'critical',
  low: 'info',
  medium: 'warning',
  high: 'critical',
  critical: 'critical',
  info: 'info',
  success: 'success',
  warning: 'warning',
  complete: 'success',
  running: 'running',
  failed: 'critical',
};

export function mapToBadgeVariant(val: string): BadgeVariant {
  return labels[val] ?? 'neutral';
}

export default function Badge({ variant, children, dot }: BadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[variant]}`}>
      {dot && <span className={styles.dot} />}
      {children}
    </span>
  );
}
