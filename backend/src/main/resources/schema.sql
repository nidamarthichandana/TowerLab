-- TowerLab DDL Schema

CREATE TABLE IF NOT EXISTS levels (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    grid_width INT NOT NULL DEFAULT 12,
    grid_height INT NOT NULL DEFAULT 8,
    initial_health INT NOT NULL DEFAULT 100,
    initial_gold INT NOT NULL DEFAULT 350,
    path_waypoints TEXT NOT NULL,
    waves_config TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS simulation_runs (
    id VARCHAR(36) PRIMARY KEY,
    level_id VARCHAR(64) NOT NULL,
    seed BIGINT NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    base_health INT NOT NULL,
    gold INT NOT NULL,
    total_ticks INT NOT NULL,
    actions_json TEXT NOT NULL,
    events_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS batch_experiments (
    id VARCHAR(36) PRIMARY KEY,
    level_id VARCHAR(64) NOT NULL,
    total_runs INT NOT NULL,
    win_rate DOUBLE PRECISION NOT NULL,
    avg_base_health DOUBLE PRECISION NOT NULL,
    avg_ticks DOUBLE PRECISION NOT NULL,
    avg_gold DOUBLE PRECISION NOT NULL,
    tower_metrics_json TEXT NOT NULL,
    actions_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tower_configs (
    tower_type VARCHAR(32) PRIMARY KEY,
    cost INT NOT NULL,
    attack_range DOUBLE PRECISION NOT NULL,
    fire_rate_ticks INT NOT NULL,
    base_damage INT NOT NULL,
    slow_factor DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    slow_duration_ticks INT NOT NULL DEFAULT 0,
    description TEXT
);

