# Transactional Outbox with CDC - Reference Implementation and Fault-Injection Study

**BSc thesis project**

- **Institution:** John Von Neumann University, GAMF Faculty of Engineering and Computer Science
- **Programme:** Computer Science Engineering BSc
- **Author:** Daniel Bodi
- **Academic year:** 2026/27

## Abstract

This thesis investigates the problem of data consistency in microservice architectures: specifically, the scenario 
where a service must write to both its local database and a message broker during a single business operation,
where distributed transactions are not a suitable option. The thesis explores the industry-recommended solution, 
the transactional outbox pattern, coupled with log-based Change Data Capture (CDC) for message publication, 
using PostgreSQL, Apache Kafka, and Debezium. 

The implemented system is subjected to intentional fault injections, such as halting the sender service at 
specific phases of the transaction or simulating the failure of various infrastructure components. Following each 
fault scenario, predefined consistency conditions are evaluated. The goal of these measurements is to determine 
which types of failures the pattern effectively mitigates, the associated trade-offs, and its inherent limitations.

## Technology stack

| Kind                     | Technology | Version     |
|--------------------------|------------|-------------|
| **Programming language** | Java       | 25 LTS      |
| **Framework**            | Spring Boot| 4.1.1       |
| **RDBMS**                | PostgreSQL | 18.6        |
| **Message Broker**       | Apache Kafka | 4.3.1     |
| **CDC**                  | Debezium   | 3.6.3.Final |
| **Testing**              | Testcontainers | 2.0.5   |
| **Orchestration**        | Docker Compose | -       |

## Repository structure

```
transactional-outbox-cdc-thesis/
├── backend/                                           Maven multi-module Java project
│   ├── libraries/
│   │   └── event-publishing-api/                      Integration event and publisher contract
│   ├── starters/                                      Reusable Spring Boot starters
│   │   ├── transactional-outbox-spring-boot-starter/  Transactional outbox (PostgreSQL)
│   │   ├── idempotent-consumer-spring-boot-starter/   Idempotent consumer (Spring Kafka, PostgreSQL)
│   │   ├── fault-injection-spring-boot-starter/       Deterministic crash points for the measurements
│   │   └── dual-write-spring-boot-starter/            Dual-write baseline publisher for the measurements
│   ├── services/
│   │   ├── subscription-service/                      Subscriptions: requests their payment and settles them on the outcome
│   │   ├── payment-service/                           Payments: charges the provider and publishes the outcome
│   │   └── psp-mock-service/                          Payment service provider mock with a durable charge log
│   └── Dockerfile                                     Shared service image, module selected by build argument
├── infrastructure/
│   ├── debezium/                                      Connector definitions and registration script
│   └── kafka/                                         Explicit topic definitions
├── measurement/
│   ├── measurement-runner/                            Runs the fault scenarios against the stack and evaluates them
│   ├── analysis/                                      Tables and figures of a measurement campaign (Python)
│   └── results/                                       Result files, one directory per campaign
├── docker-compose.yml                                 Databases, Kafka, Kafka Connect, Kafbat UI and the services
└── .env.example                                       Environment variable template (copy to .env)
```

## Prerequisites

- **Java 25** (Eclipse Temurin recommended)
- **Docker Desktop**, or Docker Engine with the Compose plugin
- **Bash shell** with `curl` available (macOS/Linux; on Windows via WSL)
- **Python 3** for the analysis of the measurements

The Maven wrapper (`mvnw`) is bundled under `backend/` and `measurement/measurement-runner/`; a separate Maven
installation is not required.

## Getting started

```bash
# 1. Create the local environment file
cp .env.example .env

# 2. Build the service images (after every change of the backend)
docker compose build
```

## Running the measurements

The runner starts a fresh stack for every run and registers the Debezium connectors itself where the scenario needs
them. Before each run it removes the running stack with its volumes (`docker compose down -v`).

```bash
# Run every scenario five times
cd measurement/measurement-runner
./mvnw spring-boot:run -Dspring-boot.run.arguments="--measurement.repetitions=5"

# A single scenario once
./mvnw spring-boot:run -Dspring-boot.run.arguments="--measurement.scenarios=S1_V1"
```

### Scenarios

| Scenario | Configurations                        | Fault                                                                                                                |
|----------|---------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| S0       | `S0_V0`, `S0_V1`                      | None                                                                                                                 |
| S1       | `S1_V0`, `S1_V1`                      | The subscription-service crashes right after a subscription commit                                                   |
| S2       | `S2_V1`                               | Kafka Connect (Debezium) is down for 10 s during the load                                                            |
| S3       | `S3_V0`, `S3_V1`                      | The Kafka broker is down for 10 s during the load                                                                    |
| S4       | `S4_V1`                               | The subscription database is down for 10 s during the load                                                           |
| S5       | `S5_V1_WITHOUT_KEY`, `S5_V1_WITH_KEY` | The payment-service crashes after the provider call, before its commit; the provider without and with idempotency key |
| S6       | `S6_V1`                               | The subscription-service crashes after the outcome commit, before the Kafka offset commit                            |

V0 is the dual write baseline, V1 the transactional outbox with CDC. A run sends 300 requests at 10 requests per second,
the fault happens at the 50th request.

### Results

The results of a campaign are written to `measurement/results/<campaign-id>/` (not versioned), where the id is the
start of the campaign in UTC, e.g. `20261015-063559`:

- `<configuration>-<repetition>.json` per run, e.g. `S1_V1-3.json`: the metadata, the verdicts of the invariants with
  the violations, the entity counts, the metrics (duplicate deliveries and provider requests, latency), the timeline
  (outages, recovery time, availability) and the raw data (requests, subscriptions, container events)
- `campaign.json`: the settings of the campaign, its start and end, and the failed runs with their error

### Analysis

```bash
# 1. Create the Python environment (once)
cd measurement/analysis
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# 2. Print the tables and draw the figures of a campaign
.venv/bin/python analyze.py ../results/<campaign-id>
```

The script prints three tables (invariants, fault tolerance, latency), each value as the median and the min–max of the
repetitions, and draws one figure per scenario from its median run to `measurement/results/<campaign-id>/figures/`
(`S0.png` … `S6.png`).

## Using the stack manually

```bash
# 1. Start the stack
docker compose up -d

# 2. Register the Debezium outbox connectors
./infrastructure/debezium/register-connectors.sh
```

With the default `.env` values the stack is reachable from the host at:

| Component            | Address                 |
|----------------------|-------------------------|
| Subscription service | `http://localhost:8080` |
| PSP mock             | `http://localhost:8082` |
| Kafbat UI            | `http://localhost:8081` |
| Kafka Connect REST   | `http://localhost:8083` |
| Kafka                | `localhost:29092`       |
| Subscription DB      | `localhost:5432`        |
| Payment DB           | `localhost:5433`        |
| PSP DB               | `localhost:5434`        |

To run a service from the IDE or with `spring-boot:run` instead, stop its container
(e.g. `docker compose stop payment-service`); the values in `.env` address the stack from the host.

```bash
# Create a subscription for the basic plan
curl -X POST http://localhost:8080/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"'"$(uuidgen)"'","planId":"11111111-1111-1111-1111-111111111111"}'

# Check the subscriptions: PENDING until the payment outcome arrives, then ACTIVE or FAILED
docker exec subscription-db psql -U subscription -d subscription \
  -c "SELECT id, status, created_at, completed_at FROM subscription ORDER BY created_at"
```

The seeded plans are `basic` (`11111111-…`), `pro` (`22222222-…`) and `enterprise` (`33333333-…`).
