# TowerLab REST API Contract Specification

## 1. Overview
All endpoints follow RESTful conventions, communicating over HTTP with `application/json` payloads.
Standard success status is `200 OK` (or `201 Created` for resource creation). Client errors yield `400 Bad Request` or `404 Not Found` with a structured `ApiError` envelope.

---

## 2. Global Error Envelope

```json
{
  "timestamp": "2026-11-13T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid tower placement: Tile (4, 3) is a PATH tile.",
  "path": "/api/simulations/play"
}
```

---

## 3. Endpoints

### 3.1 `GET /api/levels`
Retrieves a list of all available playable levels and their grid summaries.

#### Response `200 OK`
```json
[
  {
    "id": "level-1",
    "name": "The Serpentine Pass",
    "description": "Standard winding canyon path suitable for Cannon and Frost choke points.",
    "gridWidth": 12,
    "gridHeight": 8,
    "initialHealth": 100,
    "initialGold": 350,
    "totalWaves": 3
  }
]
```

---

### 3.2 `GET /api/levels/{levelId}`
Retrieves detailed level specifications, including the exact waypoint coordinate array and wave definitions.

#### Response `200 OK`
```json
{
  "id": "level-1",
  "name": "The Serpentine Pass",
  "description": "Standard winding canyon path suitable for Cannon and Frost choke points.",
  "gridWidth": 12,
  "gridHeight": 8,
  "initialHealth": 100,
  "initialGold": 350,
  "pathWaypoints": [
    {"x": 0, "y": 3},
    {"x": 4, "y": 3},
    {"x": 4, "y": 1},
    {"x": 8, "y": 1},
    {"x": 8, "y": 6},
    {"x": 11, "y": 6}
  ],
  "waves": [
    {
      "waveNumber": 1,
      "delayTicks": 20,
      "creeps": [
        {"type": "STANDARD", "count": 5, "intervalTicks": 15}
      ]
    },
    {
      "waveNumber": 2,
      "delayTicks": 40,
      "creeps": [
        {"type": "STANDARD", "count": 6, "intervalTicks": 12},
        {"type": "RUNNER", "count": 4, "intervalTicks": 10}
      ]
    },
    {
      "waveNumber": 3,
      "delayTicks": 50,
      "creeps": [
        {"type": "TANK", "count": 3, "intervalTicks": 25},
        {"type": "RUNNER", "count": 6, "intervalTicks": 8},
        {"type": "BOSS", "count": 1, "intervalTicks": 50}
      ]
    }
  ]
}
```

---

### 3.3 `POST /api/simulations/play`
Simulates a complete game run deterministically using the player's planned actions and PRNG seed. Returns the complete event log for frontend tick-by-tick replay.

#### Request Body
```json
{
  "levelId": "level-1",
  "seed": 133742,
  "actions": [
    {
      "tick": 0,
      "type": "BUILD_TOWER",
      "towerType": "CANNON",
      "x": 3,
      "y": 2,
      "targetingMode": "FIRST"
    },
    {
      "tick": 0,
      "type": "BUILD_TOWER",
      "towerType": "FROST",
      "x": 5,
      "y": 2,
      "targetingMode": "CLOSEST"
    },
    {
      "tick": 50,
      "type": "BUILD_TOWER",
      "towerType": "SNIPER",
      "x": 6,
      "y": 3,
      "targetingMode": "STRONGEST"
    }
  ]
}
```

#### Response `200 OK`
```json
{
  "id": "d3b07384-d113-4f9e-8c63-441d3d639682",
  "levelId": "level-1",
  "seed": 133742,
  "outcome": "VICTORY",
  "baseHealth": 85,
  "gold": 210,
  "totalTicks": 420,
  "events": [
    {
      "tick": 0,
      "type": "TOWER_BUILT",
      "towerId": 1,
      "towerType": "CANNON",
      "x": 3,
      "y": 2,
      "targetingMode": "FIRST",
      "goldRemaining": 250
    },
    {
      "tick": 20,
      "type": "WAVE_STARTED",
      "waveNumber": 1
    },
    {
      "tick": 20,
      "type": "CREEP_SPAWNED",
      "creepId": 1,
      "creepType": "STANDARD",
      "health": 80,
      "x": 0.0,
      "y": 3.0
    },
    {
      "tick": 45,
      "type": "TOWER_FIRED",
      "towerId": 1,
      "targetCreepId": 1,
      "damage": 45
    },
    {
      "tick": 45,
      "type": "CREEP_DAMAGED",
      "creepId": 1,
      "remainingHealth": 35
    },
    {
      "tick": 55,
      "type": "TOWER_FIRED",
      "towerId": 1,
      "targetCreepId": 1,
      "damage": 45
    },
    {
      "tick": 55,
      "type": "CREEP_KILLED",
      "creepId": 1,
      "goldBounty": 15,
      "currentGold": 265
    },
    {
      "tick": 420,
      "type": "SIMULATION_COMPLETED",
      "outcome": "VICTORY",
      "baseHealth": 85,
      "goldRemaining": 210
    }
  ]
}
```

---

### 3.4 `POST /api/simulations/batch`
Runs a Monte Carlo batch balance analysis over $N$ runs (default 100) using the given tower layout with randomized seeds. Calculates win rates, survival stats, and per-tower balance metrics.

#### Request Body
```json
{
  "levelId": "level-1",
  "runs": 100,
  "baseSeed": 1000,
  "actions": [
    {
      "tick": 0,
      "type": "BUILD_TOWER",
      "towerType": "CANNON",
      "x": 3,
      "y": 2,
      "targetingMode": "FIRST"
    },
    {
      "tick": 0,
      "type": "BUILD_TOWER",
      "towerType": "FROST",
      "x": 5,
      "y": 2,
      "targetingMode": "CLOSEST"
    }
  ]
}
```

#### Response `200 OK`
```json
{
  "experimentId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "levelId": "level-1",
  "totalRuns": 100,
  "wins": 84,
  "losses": 16,
  "winRate": 84.0,
  "avgBaseHealth": 72.4,
  "avgTicks": 465.2,
  "avgGold": 185.0,
  "towerMetrics": {
    "CANNON": {
      "towerType": "CANNON",
      "count": 1,
      "totalDamageDealt": 345000,
      "damageSharePercentage": 72.8,
      "totalKills": 1520,
      "killSharePercentage": 68.5,
      "costEfficiency": 34.5,
      "utilizationPercentage": 64.2
    },
    "FROST": {
      "towerType": "FROST",
      "count": 1,
      "totalDamageDealt": 129000,
      "damageSharePercentage": 27.2,
      "totalKills": 698,
      "killSharePercentage": 31.5,
      "costEfficiency": 10.32,
      "utilizationPercentage": 81.0
    }
  }
}
```

---

### 3.5 `GET /api/tower-configs`
Returns active balance stats for all tower types.

#### Response `200 OK`
```json
[
  {
    "towerType": "CANNON",
    "cost": 100,
    "attackRange": 2.5,
    "fireRateTicks": 10,
    "baseDamage": 45,
    "slowFactor": 0.0,
    "slowDurationTicks": 0,
    "description": "Reliable mid-range physical projectile with steady DPS."
  },
  {
    "towerType": "SNIPER",
    "cost": 150,
    "attackRange": 6.0,
    "fireRateTicks": 25,
    "baseDamage": 140,
    "slowFactor": 0.0,
    "slowDurationTicks": 0,
    "description": "Long-range precision rifle delivering massive burst damage."
  },
  {
    "towerType": "FROST",
    "cost": 125,
    "attackRange": 2.0,
    "fireRateTicks": 15,
    "baseDamage": 15,
    "slowFactor": 0.40,
    "slowDurationTicks": 20,
    "description": "Frost emitter that slows enemy advance by 40% for 20 ticks."
  }
]
```

