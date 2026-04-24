# Training Notification Service

Spring Boot microservice for fitness-platform notifications. The service consumes training events from Kafka, resolves recipients through PostgreSQL and Redis-backed lookups, and delivers notifications by email with optional Telegram and Firebase channels. The project is now prepared for Kubernetes-first delivery with production manifests and a stronger GitHub Actions pipeline.

## What changed

- Kubernetes deployment assets were added under `k8s/` with a production `kustomize` overlay.
- The application now exposes actuator liveness/readiness probes on a dedicated management port.
- CI/CD was upgraded from Docker-over-SSH delivery to a staged pipeline with manifest validation, security scans, SBOM generation, image publication and Kubernetes rollout.

## Core capabilities

- Kafka consumers for `training-events` and `training-topic`
- Email notifications rendered with Thymeleaf templates
- Optional Telegram and Firebase delivery channels behind feature toggles
- Redis-based lookup cache for recipient resolution
- PostgreSQL persistence for notification audit logs
- Scheduled weekly summary email job
- REST endpoint for publishing test events into Kafka

## Technology stack

- Java 17
- Spring Boot 3.4.13
- Spring Data JPA, Spring Kafka, Spring Mail, Spring Web, Spring Actuator
- PostgreSQL, Redis, Apache Kafka
- Maven, Checkstyle, JaCoCo, Pitest, Testcontainers
- Docker + GHCR
- Kubernetes + Kustomize

## Architecture

```text
Client/API -> NotificationController -> Kafka topics
Kafka topics -> TrainingListener / InteractionListener -> NotificationService
NotificationService -> Email / Telegram / Firebase senders
NotificationService -> PostgreSQL audit log
UserLookupService -> Redis cache -> PostgreSQL users
Kubernetes Ingress -> Service -> Deployment -> Pods with readiness/liveness probes
```

## Repository layout

```text
.github/workflows/        GitHub Actions CI/CD pipeline
docs/                     Additional operational documentation
k8s/base/                 Reusable Kubernetes manifests
k8s/overlays/prod/        Production overlay for Kubernetes
src/main/java/            Application source code
src/main/resources/       Runtime configuration and templates
src/test/                 Tests
dockerfile                Runtime container image
```

## Local run

### Prerequisites

- JDK 17+
- Maven 3.9+ or Maven Wrapper
- PostgreSQL
- Redis
- Kafka

### Important environment variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `TELEGRAM_ENABLED`
- `TELEGRAM_BOT_TOKEN`
- `FIREBASE_ENABLED`
- `MANAGEMENT_SERVER_PORT`

### Build and test

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

### Run locally

```bash
./mvnw spring-boot:run
```

Default ports:

- Application: `8086`
- Management / probes: `8081`

## Kubernetes deployment

Production manifests live in `k8s/overlays/prod`.

### Validate manifests

```bash
kubectl kustomize k8s/overlays/prod
```

### Deploy manually

```bash
kubectl apply -f k8s/secret.example.yaml
kubectl apply -k k8s/overlays/prod
kubectl -n training-notification rollout status deployment/training-notification
```

### Included Kubernetes resources

- `Namespace`
- `ServiceAccount`
- `ConfigMap`
- runtime `Secret` contract
- `Deployment`
- `Service`
- `Ingress`
- `HorizontalPodAutoscaler`
- `PodDisruptionBudget`
- `NetworkPolicy`

## CI/CD

GitHub Actions now performs:

1. Dockerfile lint + Maven quality gate
2. Full Maven test and package stage with JaCoCo artifact upload
3. Source vulnerability scan with Trivy SARIF upload
4. Kubernetes manifest rendering and schema validation
5. Container build with Buildx cache, provenance and SBOM
6. Container vulnerability scan
7. Kubernetes deployment with secret reconciliation and rollout tracking

### Required GitHub secrets

- `MAIL_PASSWORD` for CI test bootstrap
- `KUBE_CONFIG_DATA` for cluster access
- `K8S_NAMESPACE` optional override
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_PASSWORD`
- `TELEGRAM_BOT_TOKEN` optional

## REST API

### Publish test event

```http
POST /api/v1/notifications/test-send
Content-Type: application/json
```

Example payload:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "telegramTag": "@user",
  "training_name": "Morning Yoga",
  "data": "2026-04-24",
  "status": "COMPLETED",
  "email": "user@example.com",
  "exercises": []
}
```

## Operational notes

- Readiness probe: `/actuator/health/readiness`
- Liveness probe: `/actuator/health/liveness`
- Rolling update strategy keeps service availability during deploys
- Update ingress hostname and cluster service DNS names before production cutover

## Further reading

- [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)
- [docs/METHOD_REFERENCE.md](docs/METHOD_REFERENCE.md)
- [docs/KUBERNETES_DEPLOYMENT.md](docs/KUBERNETES_DEPLOYMENT.md)
