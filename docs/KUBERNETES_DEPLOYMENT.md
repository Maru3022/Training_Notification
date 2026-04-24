# Kubernetes Deployment Guide

## What is included

- `k8s/base`: reusable manifests for namespace, deployment, service, ingress, HPA, PDB and network policy.
- `k8s/overlays/prod`: production overlay with stronger replica and ingress settings.
- `k8s/secret.example.yaml`: template for required runtime secrets.

## Required cluster dependencies

- Kubernetes 1.29+
- NGINX Ingress Controller
- Metrics Server for HPA
- Reachable PostgreSQL, Redis and Kafka services
- Optional TLS secret `training-notification-tls`

## Required secrets

Create the runtime secret before deployment:

```bash
kubectl apply -f k8s/secret.example.yaml
```

Replace placeholder values with real credentials or let CI create the secret from GitHub Actions secrets.

## Local manifest validation

```bash
kubectl kustomize k8s/overlays/prod
kubectl apply --dry-run=server -k k8s/overlays/prod
```

## Manual deployment

```bash
kubectl apply -k k8s/overlays/prod
kubectl -n training-notification rollout status deployment/training-notification
```

## Runtime contract

- Application traffic: `8086`
- Management and probes: `8081`
- Readiness probe: `/actuator/health/readiness`
- Liveness probe: `/actuator/health/liveness`

## CI/CD secrets

The GitHub Actions workflow expects these secrets for Kubernetes rollout:

- `KUBE_CONFIG_DATA`
- `K8S_NAMESPACE` (optional, defaults to `training-notification`)
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_PASSWORD`
- `TELEGRAM_BOT_TOKEN` (optional)

## Notes

- The deployment uses rolling updates with zero unavailable pods during rollout.
- `NetworkPolicy` allows ingress from the ingress controller namespace and keeps egress open so SMTP and external brokers do not break unexpectedly.
- Update ingress hostnames before promoting to production.
