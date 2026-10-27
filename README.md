# Gym Reports

Gym Reports is a Java 21 and Spring Boot 3.4.0 microservice. It receives trainer
workload events from ActiveMQ, stores monthly duration summaries in MongoDB and
provides those summaries through a REST API.

## Responsibilities

- Consume `TRAINING_ADDED` and `TRAINER_STATUS_CHANGED` events.
- Maintain one MongoDB summary document per trainer.
- Aggregate duration in minutes by calendar year and month.
- Ignore duplicate deliveries using `eventId`.
- Expose trainer workload reports over HTTP.
- Publish Actuator health and Prometheus metrics.

The event contract is documented in [`docs/contracts/trainer-workload-v1.md`](docs/contracts/trainer-workload-v1.md).

## Requirements

For the complete platform:

- Docker Desktop with the Docker engine running.
- The `gym-crm` and `gym-reports` repositories in sibling folders.

For building or running this service directly from its source code:

- Java 21.
- Docker Desktop for the MongoDB integration test.

You do not need to install Maven separately. This repository includes the Maven
Wrapper (`mvnw` and `mvnw.cmd`).

## Quick start: complete platform

The easiest way to review this service is to run the complete platform. The
`compose.yaml` file is stored in the sibling `gym-crm` repository because it
starts both applications and their infrastructure.

Keep the repositories in this structure:

```text
epam-sp/
|-- gym-crm/
`-- gym-reports/
```

Open PowerShell in the `gym-crm` folder and run:

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Copy `.env.example` only the first time. If `.env` already exists, keep the
current file and continue with the remaining commands.

The first execution can take several minutes while Docker downloads images and
builds the two Java applications. The output of `docker compose ps` must list
`main`, `reports`, `mysql`, `mongodb` and `activemq`. Main, Reports, MySQL and
MongoDB should report a `healthy` state. ActiveMQ normally reports `Up`.

Verify the two applications:

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8081/actuator/health
```

Both responses should contain `"status":"UP"`.

The Reports API is available at `http://localhost:8081`. To review messages
processed by this service, run from `gym-crm`:

```powershell
docker compose logs --tail 50 reports
```

Stop the platform without deleting its database data:

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the
MySQL and MongoDB data.

## Build and test

On Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

On Linux or macOS:

```bash
./mvnw clean verify
```

The command compiles the project, runs all tests and creates the application JAR
inside `target/`. The MongoDB integration test starts a temporary MongoDB
container when Docker is available. It is skipped when Docker is unavailable.

## Configuration profiles

| Profile | MongoDB | ActiveMQ | Intended use |
|---|---:|---:|---|
| `dev` | Disabled | Disabled | Independent execution and Docker SubTask 1 |
| `test` | Disabled | Disabled | Spring context and unit tests |
| `docker` | Enabled | Configurable | Integrated execution |

`dev` is the default profile.

## Run independently without integrations

```powershell
.\mvnw.cmd spring-boot:run
```

Verify:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

The response should report `UP`. In this mode MongoDB and ActiveMQ are disabled,
so the workload REST endpoint is not available. This mode only verifies that the
application and its Docker image can start independently.

## Run against a local MongoDB container

Set the variables in the same PowerShell window used to start Maven:

```powershell
$env:SPRING_PROFILES_ACTIVE = "docker"
$env:MONGODB_URI = "mongodb://gymroot:<password>@localhost:27018/gym_reports?authSource=admin"
$env:GYM_INTEGRATIONS_ACTIVEMQ_ENABLED = "false"

.\mvnw.cmd spring-boot:run
```

Do not commit real credentials. PowerShell environment variables apply only to the current terminal session.

With MongoDB enabled, the workload endpoint is available. ActiveMQ remains
disabled, so no new workload events will be received automatically.

## Run with MongoDB and ActiveMQ

Start MongoDB and ActiveMQ first, then configure:

```powershell
$env:SPRING_PROFILES_ACTIVE = "docker"
$env:MONGODB_URI = "mongodb://gymroot:<password>@localhost:27018/gym_reports?authSource=admin"
$env:GYM_INTEGRATIONS_ACTIVEMQ_ENABLED = "true"
$env:ACTIVEMQ_BROKER_URL = "tcp://localhost:61616"
$env:ACTIVEMQ_USER = "admin"
$env:ACTIVEMQ_PASSWORD = "admin"

.\mvnw.cmd spring-boot:run
```

## Environment variables

| Variable | Required | Description |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | Integrated mode | Use `docker` to enable infrastructure |
| `MONGODB_URI` | Docker profile | Complete MongoDB connection URI |
| `GYM_INTEGRATIONS_ACTIVEMQ_ENABLED` | No | Enables the JMS listener; defaults to `true` in Docker |
| `ACTIVEMQ_BROKER_URL` | When JMS is enabled | ActiveMQ OpenWire URL |
| `ACTIVEMQ_USER` | When JMS is enabled | Broker username |
| `ACTIVEMQ_PASSWORD` | When JMS is enabled | Broker password |

Inside Docker Compose, service names replace `localhost`, for example:

```text
mongodb://gymroot:<password>@mongodb:27017/gym_reports?authSource=admin
tcp://activemq:61616
```

## REST API

The workload API is available when MongoDB integration is enabled, normally
through the `docker` profile. This service does not create workload data through
HTTP; `gym-crm` publishes the events that create or update each summary.

Return all available periods:

```http
GET /api/v1/reports/trainers/{trainerUsername}/workload
```

Filter by year:

```http
GET /api/v1/reports/trainers/john.smith/workload?year=2026
```

Filter by year and month:

```http
GET /api/v1/reports/trainers/john.smith/workload?year=2026&month=10
```

If `month` is supplied, `year` is required. Month must be between 1 and 12.

Example response:

```json
{
  "trainerUsername": "john.smith",
  "firstName": "John",
  "lastName": "Smith",
  "status": "ACTIVE",
  "unit": "MINUTES",
  "periods": [
    {
      "year": 2026,
      "month": 10,
      "trainingDuration": 60
    }
  ]
}
```

Response codes:

- `200`: report found
- `400`: invalid filters or event data
- `404`: trainer summary not found
- `503`: MongoDB unavailable

## Observability

The following endpoints are available without application credentials in this
local academic project:

```text
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

## Docker image

Build:

```powershell
docker build -t gym-reports:1.0 .
```

Run without integrations:

```powershell
docker run -d `
  --name gym-reports-standalone `
  -p 8081:8081 `
  -e SPRING_PROFILES_ACTIVE=dev `
  gym-reports:1.0
```

Verify:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
docker logs gym-reports-standalone
docker exec -it gym-reports-standalone sh
```

Remove the standalone demonstration container when finished:

```powershell
docker rm -f gym-reports-standalone
```

## MongoDB document

Database: `gym_reports`

Collection: `trainings_duration_summary`

`trainerUsername` has the unique index `uk_trainer_username`.

One document stores the trainer identity, status and all available monthly
totals. The unique index prevents two summary documents from being created for
the same trainer.

## Troubleshooting

### Docker cannot connect to its API

Start Docker Desktop and wait until its engine is running. Then verify that
`docker version` shows both **Client** and **Server** sections.

### The workload endpoint returns `404` in the `dev` profile

This is expected. The `dev` profile starts Reports without MongoDB, so the
workload endpoint is disabled. Use the complete Docker Compose platform or start
the application with the `docker` profile and a valid `MONGODB_URI`.

### Reports is unhealthy in Docker Compose

From the `gym-crm` folder, check its logs:

```powershell
docker compose logs --tail 100 reports mongodb activemq
```

Confirm that MongoDB and ActiveMQ are running and that the values in `.env`
match the credentials used to create the MongoDB volume.

### Port 8081 is already in use

Stop the application or container currently using port `8081`, then start the
service again.

## MVP limitations

- Processed event identifiers are retained inside each trainer document without expiration.
- Only one JMS consumer is configured to avoid concurrent updates to the same document.
- The endpoint has no service-to-service authentication in the local academic environment.
- CSV generation and AWS services are outside the containerization scope.
