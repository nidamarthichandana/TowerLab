import React from 'react';
import styles from './Card.module.css';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  glow?: 'blue' | 'purple' | 'green' | 'amber' | 'none';
  padding?: 'sm' | 'md' | 'lg';
  onClick?: () => void;
  style?: React.CSSProperties;
}

export default function Card({
  children,
  className = '',
  glow = 'none',
  padding = 'md',
  onClick,
  style,
}: CardProps) {
  return (
    <div
      className={[
        styles.card,
        styles[`glow-${glow}`],
        styles[`pad-${padding}`],
        onClick ? styles.clickable : '',
        className,
      ].join(' ')}
      onClick={onClick}
      style={style}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {children}
    </div>
  );
}

interface CardHeaderProps {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
  icon?: React.ReactNode;
}

export function CardHeader({ title, subtitle, action, icon }: CardHeaderProps) {
  return (
    <div className={styles.header}>
      <div className={styles.headerLeft}>
        {icon && <span className={styles.headerIcon}>{icon}</span>}
        <div>
          <h2 className={styles.title}>{title}</h2>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
        </div>
      </div>
      {action && <div className={styles.headerAction}>{action}</div>}
    </div>
  );
}
