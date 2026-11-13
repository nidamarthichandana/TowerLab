-- TowerLab Seed Data

-- 1. Tower Configurations
INSERT INTO tower_configs (tower_type, cost, attack_range, fire_rate_ticks, base_damage, slow_factor, slow_duration_ticks, description)
VALUES 
('CANNON', 100, 2.5, 10, 45, 0.0, 0, 'Reliable mid-range physical projectile with steady DPS.');

INSERT INTO tower_configs (tower_type, cost, attack_range, fire_rate_ticks, base_damage, slow_factor, slow_duration_ticks, description)
VALUES 
('SNIPER', 150, 6.0, 25, 140, 0.0, 0, 'Long-range precision rifle delivering massive burst damage.');

INSERT INTO tower_configs (tower_type, cost, attack_range, fire_rate_ticks, base_damage, slow_factor, slow_duration_ticks, description)
VALUES 
('FROST', 125, 2.0, 15, 15, 0.40, 20, 'Frost emitter that slows enemy advance by 40% for 20 ticks.');

-- 2. Level 1: The Serpentine Pass (12x8)
INSERT INTO levels (id, name, description, grid_width, grid_height, initial_health, initial_gold, path_waypoints, waves_config, created_at)
VALUES (
    'level-1',
    'The Serpentine Pass',
    'Standard winding canyon path suitable for Cannon chokepoints and Frost slowing fields.',
    12,
    8,
    100,
    350,
    '[{"x":0,"y":3},{"x":4,"y":3},{"x":4,"y":1},{"x":8,"y":1},{"x":8,"y":6},{"x":11,"y":6}]',
    '[{"waveNumber":1,"delayTicks":20,"creeps":[{"type":"STANDARD","count":6,"intervalTicks":15}]},{"waveNumber":2,"delayTicks":35,"creeps":[{"type":"STANDARD","count":8,"intervalTicks":12},{"type":"RUNNER","count":4,"intervalTicks":10}]},{"waveNumber":3,"delayTicks":40,"creeps":[{"type":"TANK","count":3,"intervalTicks":25},{"type":"RUNNER","count":6,"intervalTicks":8},{"type":"BOSS","count":1,"intervalTicks":50}]}]',
    CURRENT_TIMESTAMP
);

-- 3. Level 2: The Double Horseshoe (12x8)
INSERT INTO levels (id, name, description, grid_width, grid_height, initial_health, initial_gold, path_waypoints, waves_config, created_at)
VALUES (
    'level-2',
    'The Double Horseshoe',
    'A longer switchback path with two tight loops ideal for high-range Sniper towers.',
    12,
    8,
    100,
    400,
    '[{"x":0,"y":1},{"x":10,"y":1},{"x":10,"y":4},{"x":2,"y":4},{"x":2,"y":6},{"x":11,"y":6}]',
    '[{"waveNumber":1,"delayTicks":15,"creeps":[{"type":"STANDARD","count":8,"intervalTicks":12}]},{"waveNumber":2,"delayTicks":30,"creeps":[{"type":"RUNNER","count":8,"intervalTicks":8},{"type":"TANK","count":2,"intervalTicks":20}]},{"waveNumber":3,"delayTicks":40,"creeps":[{"type":"TANK","count":5,"intervalTicks":18},{"type":"BOSS","count":2,"intervalTicks":40}]}]',
    CURRENT_TIMESTAMP
);
