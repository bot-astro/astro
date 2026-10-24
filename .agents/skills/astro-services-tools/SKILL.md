---
name: astro-services-tools
description: Catalogue of the external services and infrastructure Astro uses (Hetzner, Kubernetes, ArgoCD, Longhorn, Prometheus, Grafana, Sentry, Infisical, Semaphore, MongoDB, Redis, BigQuery, Cloudflare, Chargebee, Crowdin...) and the CLI, MCP or dashboard to inspect each. Use when investigating production, checking metrics or logs, reading config, debugging a deploy, or looking for the right tool for a job.
---

# Astro: services and tools

Production is live. Prefer **read-only** commands (get, list, describe, top, logs, queries). Ask before anything that mutates state: apply, delete, scale, restart, writes to databases, secret changes.

For what each service does in the architecture, see `astro-tech-overview`.

## Compute and cluster

| Service | Use it for | Tool |
|---|---|---|
| Hetzner Cloud | Servers, load balancers, IPs, prices | `hcloud` CLI (context `gport`) |
| Kubernetes (k3s) | Pods, logs, events, rollouts, node and pod usage | `kubectl` (context `gport`); namespaces for each environment, plus `monitoring` and `databases` for shared services |
| ArgoCD | Deploy and sync state of Astro workloads | UI, or `kubectl get applications -n argocd` |
| Longhorn | Persistent volumes | `kubectl get volumes.longhorn.io -n longhorn-system`, its UI |
| Terraform | Cluster (in `gport`) and Astro cloud resources (in `infra`) | `terraform` CLI |
| Sealed secrets | Encrypted bootstrap secrets in git | `kubeseal` |

JVM internals of a running bot pod: attach a JDK ephemeral container with `kubectl debug` (avoid the first shard) and use `jcmd`. See `docs/infrastructure.md`.

## Observability

| Service | Use it for | Tool |
|---|---|---|
| Sentry | Errors, logs, traces, releases, alerts | Sentry MCP and the Sentry skills; web UI |
| Prometheus | Metrics (pod memory, JVM and app metrics) | `kubectl port-forward` the Prometheus service in `monitoring`, then query its HTTP API |
| Grafana | Dashboards over Prometheus | Web UI (shared cluster domain) |
| Alertmanager | Alert routing | Same stack as Prometheus |

## Config and secrets

| Service | Tool |
|---|---|
| Infisical | `infisical` CLI (`export`, `secrets`, `run`) and the Infisical MCP; web UI. Environments map to deployment targets. Never print secret values into logs or commits. |
| Infisical Kubernetes operator | Syncs Infisical into cluster Secrets; inspect its custom resources with `kubectl` |

## Build, deploy, source

| Service | Use it for | Tool |
|---|---|---|
| GitHub | Code, PRs, Actions | `gh` CLI |
| GitHub Container Registry | Service images | `gh`/web UI |
| Semaphore | CI pipelines, promotions to staging/production | Web UI (and the `sem` CLI if installed) |
| Gradle | Build, test, lint | `./gradlew`, wrapped by `just`; the user compiles and tests themselves, see `CLAUDE.md` |
| pre-commit | Hooks (ktlint, detekt) | `prek`, via `just hooks` |

## Data

| Service | Use it for | Tool |
|---|---|---|
| MongoDB (managed cloud) | Settings, users, premium state | `mongosh` or Compass with the connection string from Infisical; locally a Compose container with a web UI. Avoid editing documents through UIs, they can corrupt types. |
| Redis (in cluster) | Temporary channel state, caches | `redis-cli` via `kubectl exec` in the `databases` namespace; locally RedisInsight in Compose |
| BigQuery | Usage analytics | `bq` CLI or console, always with a partition filter |

## Business and third parties

| Service | Use it for | Tool |
|---|---|---|
| Chargebee | Subscriptions, invoices, webhooks | Web dashboard and API |
| Discord developer portal | App settings, SKUs, intents, entitlements | Web portal |
| Cloudflare | DNS, R2 media bucket | `cf` CLI, dashboard or Terraform |
| Crowdin | Translations | Crowdin CLI, Crowdin MCP and Crowdin skills |
| Bruno | Calling the API by hand | Collection in the `astro-bruno` repo |

## Habits

- Check the right kubectl context and namespace before every command; production and staging look alike.
- Metrics and Sentry first, logs second, shell access last.
- When a tool isn't available locally, say so and give the user the exact command instead of guessing output.
