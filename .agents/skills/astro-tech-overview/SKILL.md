---
name: astro-tech-overview
description: Broad technical map of Astro - backend services and how the code is organised, the frontend, where everything runs, CI/CD, secrets, environments and which repo owns what. Use when orienting in the project, deciding where a change belongs, or before deploy/infra work. For specific tools and CLIs see astro-services-tools.
---

# Astro: technical overview

## Repos (sibling folders)

| Repo | Owns |
|---|---|
| `astro` (this one) | Backend services and CI pipeline |
| `astro-bot.space` | Frontend: public site and dashboard (Nuxt, Vue, Tailwind) |
| `infra` | GitOps manifests for the Astro workloads, plus Terraform for Astro-specific cloud resources |
| `gport` (personal repo) | The Kubernetes cluster itself and shared cluster services (monitoring, storage, certificates, Redis) |
| `astro-bruno` | Bruno collection for calling the API by hand |

`docs/` in this repo holds focused write-ups (infrastructure and costs, pricing, authentication, migrations). Read them before duplicating anything.

## Backend

Kotlin on Spring Boot, one Gradle multi-module build, JDA for Discord.

- `bot`: the Discord bot. Receives gateway events and interactions, runs the voice channel logic. **Sharded**: each pod owns one shard.
- `api-central`: REST API for the dashboard. Handles Discord OAuth sign-in, server settings, billing webhooks, and talks to the bot pods when it needs live Discord data.
- `support-bot`: manages the support Discord server (premium role) and exposes a small internal API.
- `entitlements-expiration-job`: scheduled job that expires premium entitlements.
- `shared/core`: code shared by all services (models, repositories, clients, translations, premium rules, auto-configuration).
- `*-old` modules and `shared/old-core`: the legacy version, reference for behaviour only; new work goes in the non-old modules.
- `build-logic`: Gradle convention plugins that define how services are built, linted and containerised.

Inside services the layout follows the same idea: events and interactions in, handlers/controllers, managers/services for logic, shared repositories for data.

## Data

- **MongoDB** (managed cloud cluster): durable settings and users.
- **Redis** (in the cluster): temporary voice channel state and caches.
- **BigQuery**: analytics events.
- **Cloudflare R2**: media storage.

## Where it runs

Kubernetes (k3s) on Hetzner Cloud with a node autoscaler, defined in `gport`. Workloads are deployed by ArgoCD from the `infra` repo, one namespace per environment: production and staging. The bot is a StatefulSet (stable pod identity maps to shards); the API and support bot are Deployments or StatefulSets behind nginx ingress; the expiration job is a CronJob. Cloudflare provides DNS.

## CI/CD

- Semaphore builds on push, only for services whose code (or shared code) changed: build, test, then build and push the image to GitHub Container Registry with Jib.
- A passing build bumps the image tag in the `infra` repo; ArgoCD syncs it. Staging deploys automatically, production needs a manual promotion.
- Rollback is a revert in `infra`.
- Local pre-commit/pre-push hooks run ktlint and detekt.

## Configuration and secrets

Infisical is the source of truth for configuration and secrets, with one environment per deployment target. In the cluster the Infisical Kubernetes operator turns them into Secrets/ConfigMaps; sealed secrets cover only bootstrap items. Locally, the env is exported from Infisical.

## Observability

- Sentry for errors, logs and traces.
- Micrometer metrics pushed over OTLP to Prometheus; Grafana for dashboards.
- Kubernetes probes and metrics run on a separate management port that is never exposed publicly.

## Local development

Docker Compose gives local MongoDB and Redis (with web UIs); the `justfile` has the common tasks (run a service, compose, test, format, lint). Env comes from Infisical's dev environment.

## Translations

Source strings live in `shared/core` and sync with Crowdin.
