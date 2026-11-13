# TowerLab

**Tower Defense Simulation Engine & Game Balance Analytics Platform**

TowerLab is a full-stack platform consisting of a deterministic, tick-based Java simulation engine, balance analysis suite, and a modern planning & replay interface.

---

## Architecture Overview

1. **Deterministic Java Engine (`com.towerlab.engine`):**
   - Pure POJO simulation loop.
   - 12×8 grid with coordinates and waypoint pathfinding.
   - Tower archetypes: **Cannon**, **Sniper**, **Frost**.
   - Targeting modes: `FIRST`, `LAST`, `STRONGEST`, `CLOSEST`.
   - Seeded PRNG (`Random(seed)`) guarantees 100% reproducible tick-by-tick event streams.

2. **Game Balance Analyzer (`com.towerlab.analytics`):**
   - High-throughput parallel Monte Carlo batch runner (`POST /api/simulations/batch`).
   - Statistical balance metrics: Win Rate %, Average Base Health, Ticks Survived, Gold Accumulation.
   - Per-Tower telemetry: Damage Share %, Kill Share %, Cost Efficiency ($\frac{\text{Damage}}{\text{Cost}}$), and Tower Utilization %.

3. **Persistence Layer:**
   - **PostgreSQL / H2:** Configured with Spring Data JPA.
   - Default `dev` profile uses in-memory H2 with Web Console (`/h2-console`) for zero-configuration setup.
   - `postgres` profile connects directly to PostgreSQL 18 with complete DDL schema (`schema.sql`).

4. **REST API:**
   - `GET /api/levels`: Retrieve map catalog.
   - `GET /api/levels/{id}`: Detailed map waypoints and creep wave composition.
   - `POST /api/simulations/play`: Execute deterministic single run and generate replay event stream.
   - `POST /api/simulations/batch`: Execute multi-run balance analytics.
   - `GET /api/tower-configs`: Retrieve live tower balance parameters.

---

## Documentation

Full architectural specifications are located in the [`docs/`](file:///c:/Users/SHIVAM/Desktop/iiit/docs) folder:
- [GAME_RULES.md](file:///c:/Users/SHIVAM/Desktop/iiit/docs/GAME_RULES.md): Complete game rules, grid geometry, tower and creep stats, tick sequence.
- [DATABASE_SCHEMA.md](file:///c:/Users/SHIVAM/Desktop/iiit/docs/DATABASE_SCHEMA.md): Relational schema, DDL, indexes, and JSON column specifications.
- [API_CONTRACT.md](file:///c:/Users/SHIVAM/Desktop/iiit/docs/API_CONTRACT.md): Comprehensive REST API contract with request/response schemas.
- [CLASS_DESIGN.md](file:///c:/Users/SHIVAM/Desktop/iiit/docs/CLASS_DESIGN.md): Class diagrams, Strategy pattern for targeting, batch runner concurrency.
- [FRONTEND_HANDOFF.md](file:///c:/Users/SHIVAM/Desktop/iiit/docs/FRONTEND_HANDOFF.md): Instructions for the frontend Plan, Run, Replay, and Analytics modules.

---

## Running the Backend

### Prerequisites
- Java 17+ (e.g. Eclipse Adoptium OpenJDK 17)
- Maven Wrapper is included (`mvnw` / `mvnw.cmd`), no separate Maven install needed.

### Running with H2 (Default Dev Profile)
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
The server will start on `http://localhost:8080`.
H2 Console available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:towerlabdb`, User: `sa`, Password: empty).

### Running with PostgreSQL
```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/towerlab"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "<your-password>"
.\mvnw.cmd spring-boot:run
```

### Running Tests
```powershell
cd backend
.\mvnw.cmd test
```
