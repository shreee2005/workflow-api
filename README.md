# Workflow API

The core backend service of the **Workflow Engine** platform. It exposes the REST API used to define, trigger, and manage workflow executions, and publishes work items to the `workflow-worker` service for asynchronous processing.

> Part of a 3-service system: **workflow-api** (this repo) · [workflow-worker](../workflow-worker) · [workflow-ui](../workflow-ui)

---

## Overview

`workflow-api` is a Spring Boot service that acts as the system of record and entry point for the Workflow Engine. It handles authentication/authorization, persists workflow and process data to PostgreSQL, and hands off long-running or async tasks to `workflow-worker` over RabbitMQ.

## Architecture

```
┌──────────────┐        REST/HTTPS         ┌──────────────────┐
│  workflow-ui │ ────────────────────────▶ │   workflow-api     │
└──────────────┘                            │  (this service)   │
                                             └─────────┬─────────┘
                                                        │
                                       publishes jobs   │  reads/writes
                                                        ▼
                                   ┌────────────┐   ┌────────────┐
                                   │  RabbitMQ   │   │ PostgreSQL │
                                   └─────┬──────┘   └────────────┘
                                         │
                                         ▼
                                ┌───────────────────┐
                                │  workflow-worker    │
                                └───────────────────┘
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot |
| Data | PostgreSQL, Spring Data JPA |
| Messaging | RabbitMQ (Spring AMQP) |
| Security | Spring Security, JWT (jjwt), OAuth2 Client |
| Observability | Spring Boot Actuator, Micrometer + Prometheus, OpenTelemetry (Zipkin exporter), Logstash JSON logging |
| Build | Maven (`mvnw`) |
| Testing | Spring Boot Test, H2 (in-memory DB), spring-rabbit-test, spring-security-test |
| Containerization | Docker (see `/docker`) |
| CI | GitHub Actions (`.github/workflows`) |

## Prerequisites

- Java 17 (JDK)
- Maven (or use the bundled `mvnw` / `mvnw.cmd`)
- PostgreSQL instance (local or containerized)
- RabbitMQ instance (local or containerized)

## Getting Started

### 1. Configure environment

Set the following in `src/main/resources/application-dev.yml` or as environment variables:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/workflow_dev
SPRING_DATASOURCE_USERNAME=wf_user
SPRING_DATASOURCE_PASSWORD=wf_pass
SPRING_RABBITMQ_HOST=localhost
```

### 2. Build and run

```bash
./mvnw clean package
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows, you can also start the API via:

```powershell
.\scripts\start-api.ps1
```

### 3. Run with Docker

```bash
cd docker
docker compose up -d --build
```

## Database

SQL schema/migration scripts live under `/sql`. Apply them against your PostgreSQL instance before first run if they are not applied automatically.

## API Reference

> ⚠️ Fill in with your actual controller endpoints, e.g.:

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/workflows` | Create a new workflow definition |
| `GET` | `/api/workflows/{id}` | Fetch a workflow definition |
| `POST` | `/api/workflows/{id}/trigger` | Trigger a workflow execution (queued to `workflow-worker`) |
| `GET` | `/api/executions/{id}` | Get execution status/history |

Authentication is via JWT bearer tokens issued through the OAuth2/JWT flow configured in this service.

## Observability

- Health/metrics: `/actuator/health`, `/actuator/prometheus`
- Distributed tracing exported via OpenTelemetry to Zipkin
- Structured JSON logs via Logstash Logback encoder

## Project Structure

```
workflow-api/
├── .github/workflows/   # CI pipelines
├── docker/              # Dockerfile & compose for local/dev
├── sql/                 # DB schema / migration scripts
├── src/                 # Application source
├── pom.xml
└── mvnw / mvnw.cmd
```

## Contributing

1. Create a feature branch off `main`
2. Make your changes with tests
3. Run `./mvnw clean verify` before opening a PR

## License

Add your license here (e.g., MIT, Apache 2.0).
