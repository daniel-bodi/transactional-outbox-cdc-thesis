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
├── backend/                                    Maven multi-module Java project
│   ├── outbox-postgres-spring-boot-starter/    Reusable outbox producer library
│   ├── subscription-service/                   Reference producer service
│   └── payment-service/                        Reference consumer service
├── infrastructure/
│   └── debezium/                               Connector definitions and registration script
├── docker-compose.yml                          PostgreSQL, Kafka, Kafka Connect, Kafbat UI
└── .env.example                                Environment variable template (copy to .env)
```

## Prerequisites

- **Java 25** (Eclipse Temurin recommended)
- **Docker Desktop**, or Docker Engine with the Compose plugin
- **Bash shell** with `curl` available (macOS/Linux; on Windows via WSL)

The Maven wrapper (`mvnw`) is bundled under `backend/`; a separate Maven installation is not required.

## Getting started

```bash
# 1. Create the local environment file
cp .env.example .env

# 2. Start the infrastructure stack (PostgreSQL databases, Kafka, Kafka Connect, Kafbat UI)
docker compose up -d

# 3. Start the backend services (each in its own terminal).
#    Their Flyway migrations create the tables Debezium relies on,
#    so this must happen before the connector is registered.
cd backend
./mvnw -pl subscription-service spring-boot:run
./mvnw -pl payment-service spring-boot:run

# 4. Register the Debezium outbox connector.
#    Requires the services above to have completed their Flyway migrations;
#    otherwise the connector fails because the observed tables do not yet exist.
./infrastructure/debezium/register-connectors.sh
```

Once started, the subscription service listens on `http://localhost:8080` and the Kafbat UI on `http://localhost:8081`.
