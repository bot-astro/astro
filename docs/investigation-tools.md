# Investigation tools

Tools found and used for gathering data about Astro (infra, usage, premium, memory). All commands are read-only unless noted. Date: 2026-10-24.

## Infra and costs

| Tool | Use | Example |
|---|---|---|
| `hcloud` CLI (v1.61, context `gport`) | Exact Hetzner servers, prices, LBs, IPs | `hcloud server list`, `hcloud server-type list -o json` (prices per location), `hcloud load-balancer list`, `hcloud primary-ip list` |
| Hetzner API `/v1/pricing` | Full price table incl. IPv4, VAT rate | `curl -H "Authorization: Bearer <token>" https://api.hetzner.cloud/v1/pricing` (token in `~/.config/hcloud/cli.toml`) |
| Hetzner docs | Price changes | [Price adjustment](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/) |
| `gport` repo (`Giuliopime/gport`) | Terraform for the cluster | `hcloud-cluster/kube-hetzner.tf` (node pools, autoscaler min/max) |

No Hetzner MCP exists; the CLI covers everything.

## Cluster and runtime

| Tool | Use | Example |
|---|---|---|
| `kubectl` (context `gport`) | Node/pod usage, config, volumes | `kubectl top nodes`, `kubectl top pods -A --sort-by=memory`, `kubectl get sts bot -n astro -o jsonpath=...` |
| Prometheus | Memory/CPU history | `kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 19090:9090`, then query `container_memory_working_set_bytes{pod="bot-5"}` over 30d. No JVM or JDA metrics are exported yet |
| `kubectl debug` + `jcmd` | JVM heap analysis (bot image is a JRE with no `jcmd`) | `kubectl debug -n astro bot-5 --image=eclipse-temurin:25-jdk --target=bot --profile=general -c heapdbg -- sleep 900`, then `kubectl exec ... -c heapdbg -- jcmd 1 GC.heap_info` or `GC.class_histogram` (full GC, brief pause; avoid `bot-0`) |
| Longhorn CRDs | Volume sizes and usage | `kubectl get volumes.longhorn.io -n longhorn-system` |

## Usage analytics

| Tool | Use | Notes |
|---|---|---|
| `bq` CLI, project `astro-405113`, dataset `astro_bot` | VC usage, installed guilds, dormancy | Always pass `--use_legacy_sql=false --maximum_bytes_billed=...` and a `timestamp` partition filter |

Tables: `TEMPORARY_VC_GENERATION`, `SLASH_COMMAND_INVOCATION`, `CONNECTION_INVOCATION`, `GUILD_EVENT` (JOINED/KICKED), `GUILD_SNAPSHOT` (member_count, guild_name, shard_id; the only source of installed-guild counts, ~167k; data stops mid-Sept).

Useful queries done so far: monthly VCs and active guilds, guild bands by VC count, dormancy (no activity across the three activity tables), reactivation rate, premium usage via an inline `UNNEST([...])` of guild IDs.

## Premium and config data

| Tool | Use | Notes |
|---|---|---|
| `mongosh` (installed via Homebrew) | Read the premium guild list | DB `AstroDB`, collection `guilds`. Premium = `upgradedByUserID` set or an `entitlements` element with `endsAt` null or in the future. Connection string: ConfigMap `bot-config`, key `IO_KMONGO_CONNECTION_STRING` (read without printing it; it should move to a Secret) |
| `mongodump` | Present locally, not used | |

## Research and docs

| Tool | Use |
|---|---|
| WebSearch / WebFetch | Price changes, Discord intent docs |
| Claude Docs MCP | Create/edit shareable docs (not used; repo docs are plain markdown) |
| Skill `astro-overview` | Product outline, premium setup, analytics tables |
| Artifact tool | Publish a shareable page, e.g. a dashboard or report (not used) |

## Available but not relevant here or not connected

- Crowdin MCP and skills: localization only.
- Gmail, Google Calendar, Google Drive MCPs: connected but need authentication; not used.
- No Discord API token or Chargebee access has been used. Chargebee would give real subscription history and churn.

## Gotchas

- zsh: quote globs (`--include='*.kt'`), or grep fails with `no matches found`.
- The bot image has no `ps`, `which` or `jcmd`.
- Code under `*-old` is legacy; the new bot is in `services/bot`.
- Do not print the Mongo URI or Hetzner token; use them via env or command substitution.
