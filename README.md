# Deployment Diff Notifier

A small Spring Boot service that records deployments, computes **what changed since the last deploy** of the same service + environment, and sends a Slack notification. Packaged with a multi-stage Docker build and a GitHub Actions CI/CD pipeline.

every time something ships, a CI/CD pipeline `POST`s a deploy event here. The service compares it against the previous deploy (version, commit, time since last deploy) and posts a human-readable summary to Slack — so a team always knows *what just went out and how it differs from before*.

---

## Tech stack

- **Java 17**, **Spring Boot 3.5.3**
- **Spring Web** (REST API)
- **Spring Data JPA** + **Hibernate**
- **H2** in-memory database
- **Bean Validation** (`@Valid`, `@NotBlank`)
- **RestClient** for the outbound Slack call
- **JUnit 5** + **MockMvc** + **MockRestServiceServer** for tests
- **Docker** (multi-stage build)
- **GitHub Actions** for CI/CD

---

## Architecture

A classic layered design — each layer has one job:

```
HTTP request
   │
   ▼
DeploymentController   ← REST endpoints, validation, HTTP status codes
   │
   ▼
DeploymentService      ← business logic: look up previous deploy, build the diff, notify
   │           │
   ▼           ▼
DeploymentRepository   SlackNotifier   ← outbound Slack call (fail-safe)
   │
   ▼
Deployment (JPA entity) ↔ H2 database
```

Key types:

- `Deployment` — the JPA entity (one row per deploy).
- `DeploymentRequest` — the incoming DTO, validated so bad requests are rejected with `400`.
- `DeploymentDiff` — a Java `record` describing the difference between this deploy and the previous one.
- `SlackNotifier` — posts the diff to a Slack webhook; **best-effort** (a failed notification never fails the deploy).

---

## API

Base path: `/api/deployments`

### Record a deploy (and get the diff back)

```bash
curl -X POST http://localhost:8080/api/deployments \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payments-api",
    "version": "1.4.0",
    "gitCommit": "a1b2c3d",
    "environment": "production"
  }'
```

First deploy of a service+environment returns `firstDeploy: true`. A later deploy returns the diff:

```json
{
  "firstDeploy": false,
  "serviceName": "payments-api",
  "environment": "production",
  "previousVersion": "1.3.0",
  "currentVersion": "1.4.0",
  "previousCommit": "9f8e7d6",
  "currentCommit": "a1b2c3d",
  "minutesSinceLastDeploy": 42
}
```

### List / filter deploy history

```bash
curl "http://localhost:8080/api/deployments"
curl "http://localhost:8080/api/deployments?service=payments&env=production"
```

Both filters are optional. `service` is a partial, case-insensitive match; `env` is an exact, case-insensitive match.

### Get a single deploy by id

```bash
curl http://localhost:8080/api/deployments/1
```

Returns `200` with the deploy, or `404` if the id does not exist.

---

## Slack notifications

Set your Slack **Incoming Webhook** URL in `application.properties`:

```properties
slack.webhook-url=https://hooks.slack.com/services/XXX/YYY/ZZZ
```

- Leave it **empty** to disable notifications entirely (the service simply no-ops).
- The call is **fail-safe**: if Slack is unreachable, the error is logged as a warning and the deploy still succeeds. A notification failure should never break a deployment record.

For local testing you can point it at any HTTP endpoint (e.g. a [webhook.site](https://webhook.site) URL or a small local receiver).

---

## Run locally

```bash
# run the app
./mvnw spring-boot:run

# run the tests
./mvnw test

# build the runnable jar
./mvnw clean package
```

App starts on `http://localhost:8080`. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:deployments`).

---

## Run with Docker

The `Dockerfile` uses a **multi-stage build**: stage 1 compiles the jar with Maven + JDK, stage 2 copies only the jar into a slim JRE image and runs it as a non-root user (smaller, safer final image).

```bash
docker build -t deploy-diff-notifier .
docker run -p 8080:8080 deploy-diff-notifier

# with Slack enabled:
docker run -p 8080:8080 -e SLACK_WEBHOOK_URL=https://hooks.slack.com/... deploy-diff-notifier
```

---

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request to `main`:

1. **Build & Test** — sets up JDK 17, then runs `./mvnw -B verify` (compiles + runs all tests).
2. **Docker Build** — only after tests pass, and only on pushes to `main`, builds the Docker image to confirm the `Dockerfile` is valid.

This gives fast feedback on every change and guarantees `main` always builds, tests green, and containerizes.

---

## Tests

8 tests cover the full flow:

- records a valid deployment as a first deploy
- computes the diff against a previous deploy
- rejects a deployment missing a required field (`400`)
- lists and filters deployments
- returns `404` for an unknown id
- posts a message to the Slack webhook when configured (stubbed with `MockRestServiceServer`)
- does nothing when no webhook is configured

Run them with `./mvnw test`.

---
