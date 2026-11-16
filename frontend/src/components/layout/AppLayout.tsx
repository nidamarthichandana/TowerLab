import React, { useState } from 'react';
import { Outlet, NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Swords,
  PenTool,
  BarChart3,
  FileText,
  ChevronLeft,
  ChevronRight,
  Zap,
  Settings,
  Bell,
  Search,
  HelpCircle,
} from 'lucide-react';
import styles from './AppLayout.module.css';

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
  badge?: number;
}

const navItems: NavItem[] = [
  { path: '/',               label: 'Dashboard',       icon: <LayoutDashboard size={18} /> },
  { path: '/battlefield',    label: 'Battlefield',      icon: <Swords size={18} /> },
  { path: '/level-editor',   label: 'Level Editor',     icon: <PenTool size={18} /> },
  { path: '/balance-analyzer', label: 'Balance Analyzer', icon: <BarChart3 size={18} />, badge: 4 },
  { path: '/reports',        label: 'Reports',          icon: <FileText size={18} /> },
];

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();

  const currentPage = navItems.find(n =>
    n.path === '/' ? location.pathname === '/' : location.pathname.startsWith(n.path)
  )?.label ?? 'TowerLab';

  return (
    <div className={styles.root}>
      {/* ── Sidebar ── */}
      <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
        {/* Logo */}
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Zap size={18} />
          </div>
          {!collapsed && (
            <div className={styles.logoText}>
              <span className={styles.logoName}>TowerLab</span>
              <span className={styles.logoTagline}>Tactical Command</span>
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className={styles.nav} aria-label="Main navigation">
          {!collapsed && <span className={styles.navSection}>Navigation</span>}
          {navItems.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `${styles.navItem} ${isActive ? styles.navItemActive : ''}`
              }
              title={collapsed ? item.label : undefined}
            >
              <span className={styles.navIcon}>{item.icon}</span>
              {!collapsed && (
                <>
                  <span className={styles.navLabel}>{item.label}</span>
                  {item.badge !== undefined && (
                    <span className={styles.navBadge}>{item.badge}</span>
                  )}
                </>
              )}
              {collapsed && item.badge !== undefined && (
                <span className={styles.navBadgeDot} />
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bottom section */}
        <div className={styles.sidebarBottom}>
          <button className={styles.navItem} title={collapsed ? 'Settings' : undefined}>
            <span className={styles.navIcon}><Settings size={18} /></span>
            {!collapsed && <span className={styles.navLabel}>Settings</span>}
          </button>
          <button className={styles.navItem} title={collapsed ? 'Help' : undefined}>
            <span className={styles.navIcon}><HelpCircle size={18} /></span>
            {!collapsed && <span className={styles.navLabel}>Help & Docs</span>}
          </button>
        </div>

        {/* Status bar */}
        {!collapsed && (
          <div className={styles.statusBar}>
            <span className={styles.statusDot} />
            <span className={styles.statusText}>Engine v2.4.1 — Online</span>
          </div>
        )}

        {/* Collapse toggle */}
        <button
          className={styles.collapseBtn}
          onClick={() => setCollapsed(c => !c)}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
        </button>
      </aside>

      {/* ── Main area ── */}
      <div className={styles.main}>
        {/* Top bar */}
        <header className={styles.topbar}>
          <div className={styles.topbarLeft}>
            <div className={styles.breadcrumb}>
              <span className={styles.breadcrumbRoot}>TowerLab</span>
              <span className={styles.breadcrumbSep}>/</span>
              <span className={styles.breadcrumbCurrent}>{currentPage}</span>
            </div>
            <div className={styles.topbarDivider} />
            <div className={styles.topbarSearch}>
              <Search size={13} />
              <span>Search...</span>
              <span className={styles.searchKbd}>⌘K</span>
            </div>
          </div>

          <div className={styles.topbarRight}>
            <button className={styles.topbarBtn} aria-label="Notifications">
              <Bell size={16} />
              <span className={styles.notificationDot} />
            </button>
            <div className={styles.topbarDivider} />
            <div className={styles.userAvatarWrap} aria-label="User profile">
              <div className={styles.userAvatar}>TA</div>
              {!collapsed && <span className={styles.userAvatarName}>TowerAdmin</span>}
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className={styles.content} id="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
