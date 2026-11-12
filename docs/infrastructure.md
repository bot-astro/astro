# Infrastructure and costs

Date: 2026-10-24. Status: proposal, nothing applied. Prices net of VAT, per month, Hetzner nbg1. Infra lives in `Giuliopime/gport` (`hcloud-cluster/kube-hetzner.tf`).

## Situation

- Hetzner raised CPX prices on 15 June 2026 (CPX52: €36.49 to €100.49). Existing servers keep their old price, every autoscaler replacement pays the new one. Two old cpx52 nodes at €36.49 and one at €100.49; replacing the old two would cost another €128/month.
- The workload is memory-bound, not CPU-bound: autoscaler nodes sit at 5-7% CPU and 85-93% memory.
- 11 `bot` pods (one per shard) use ~4 GiB each but request 5 GiB. Longhorn holds ~42 GB (Redis 30, Prometheus 10), single replica.
- Location is not a lever: nbg1, fsn1 and hel1 cost the same.

## 1. Switch cpx to cx (saves ~€154/month, low risk)

Same RAM, not affected by the June increase, all x86. cx CPUs are slower, irrelevant at ~6% CPU.

| Role | Now | Proposed | Monthly |
|---|---|---|---|
| 3x control plane | cpx22 (2c/4G), €19.49 each | cx23, €5.49 each | €58.47 → €16.47 |
| 1x agent | cpx32 (4c/8G) | cx33 (4c/8G) | €35.49 → €8.49 |
| 3x autoscaler | cpx52 (12c/24G), €173.47 total | cx53 (16c/32G), €29.49 each | €173.47 → €88.47 |

Keep 3 cx53 (96 GiB vs ~61 GiB used). Control-plane nodes are already memory-starved at 4 GB; use cx33 (€8.49) if etcd or the API server gets OOM-killed.

Rollout: change `server_type` for one autoscaler node, watch shard latency and memory for a few days, then the rest. Then the agent, then the control plane one node at a time (never more than 1 down, keep etcd quorum). Drain and cordon each node first.

## 2. Right-size bot pods (saves ~€29/month more)

Measured on `bot-5` (15,467 guilds): live heap after full GC is **~0.94 GiB**, but the heap is capped at 3 GiB (`MaxRAMPercentage=60` of 5 Gi), G1 fills it, and the working set is a flat ~4 GiB over 30 days. The rest is JVM slack. Nodes are sized by **requests**, not usage: 11 x 5 Gi = 55 GiB for the bot alone forces the third node.

| Setting | Now | Proposed |
|---|---|---|
| Heap max | 3 GiB | ~1.5 GiB (`-Xmx1536m`, ~1.5x live set) |
| Memory request / limit | 5 Gi / 5 Gi | 3 Gi / 3.5 Gi |
| Expected RSS | ~4.0 GiB | ~2.5 GiB (1.5 heap + ~1.0 non-heap) |

Bot requests drop to 33 GiB; with ~16 GiB for other pods that fits 2 cx53 (~61 GiB allocatable) at ~80%. Set autoscaler `min_nodes = 2`, `max_nodes = 3`. Trade-off: losing a node takes bots down for minutes until the autoscaler replaces it.

Rollout: one pod first (not `bot-0`), watch RSS, GC pauses and OOM kills for a week, then the rest. Optionally add `-XX:G1PeriodicGCInterval` and export JVM metrics (Micrometer; none reach Prometheus today) and `-XX:NativeMemoryTracking=summary` to confirm the ~1 GiB non-heap estimate.

## Totals

| | Now | After 1 | After 1+2 |
|---|---|---|---|
| Hetzner (servers, 2x lb11 €13.98, 9 IPv4 €4.50) | €285.91 | €131.91 | €102.42 |
| MongoDB Cloud (~$80) | ~€70 | ~€70 | ~€70 |
| **Total** | **~€356** | **~€202** | **~€172** |
| Profit at €505 revenue | ~€149 | ~€303 | ~€333 |

Not included: Cloudflare, BigQuery (cents), domains, Chargebee, backups, VAT (+22% if not recoverable). IPv4 may already be part of the lb11 price.

Unit cost (Sep: 311k VCs, 12.5k active guilds, after step 1): €0.65 per 1,000 VCs, €0.016 per active guild. Cost is fixed and driven by shard memory, not by VCs generated; one more guild costs ~€0.0003/month.

## Decisions

| Question | Decision | Why |
|---|---|---|
| Keep `GUILD_PRESENCES`? | Yes | Only source of the `{activity_*}` premium variables. Presence data is ~4-5% of live heap (~30-50 MB per pod). |
| Leave dormant guilds? | Defer | 167k installed, 76-89% dormant, but worth only ~€27/month on top of step 2 (non-heap ~1 GiB per JVM stays), and 4.9% reactivate within 6 months. If done: 30 days notice, settings kept, 12-month threshold, premium excluded. |
| Rust rewrite? | No | Live heap is 1 GB; right-sizing gets most of the gain. |
| 2 vs 3 autoscaler nodes | 2 | €29/month for availability is not worth it. Revisit if outages hurt. |

Other cheap idea (unmeasured): disable `CacheFlag.MEMBER_OVERRIDES` if no logic reads member overrides (`PermissionOverrideImpl` is 76 MB, ~100 per guild).

## Other to-dos

- Move the Mongo connection string from the `bot-config` ConfigMap (`IO_KMONGO_CONNECTION_STRING`) to a Secret.
- Check real IPv4 and load balancer line items on the invoice, and that `cx` capacity is available in nbg1 when scaling.

## Tools and how to reproduce

| Need | How |
|---|---|
| Hetzner servers, prices, LBs | `hcloud` CLI (context `gport`): `server list`, `server-type list -o json`, `load-balancer list`, `primary-ip list`. Full price table: `/v1/pricing` API (token in `~/.config/hcloud/cli.toml`). |
| Node/pod usage | `kubectl top nodes`, `kubectl top pods -A --sort-by=memory` (context `gport`) |
| Memory history | `kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 19090:9090`, query `container_memory_working_set_bytes{pod="bot-5"}` |
| Volumes | `kubectl get volumes.longhorn.io -n longhorn-system` |
| JVM heap | The bot image is a JRE without `jcmd`, so attach a JDK ephemeral container (avoid `bot-0`): |

```sh
kubectl debug -n astro bot-5 --image=eclipse-temurin:25-jdk --target=bot --profile=general -c heapdbg -- sleep 900
kubectl exec -n astro bot-5 -c heapdbg -- jcmd 1 GC.heap_info
kubectl exec -n astro bot-5 -c heapdbg -- jcmd 1 GC.class_histogram   # forces a full GC (brief pause)
```

Sources: [Hetzner price adjustment](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/), [Discord: you might not need a privileged intent](https://docs.discord.com/developers/gateway/you-might-not-need-a-privileged-intent).
