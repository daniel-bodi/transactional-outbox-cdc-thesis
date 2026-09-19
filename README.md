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
