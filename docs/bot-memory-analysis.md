# Bot memory analysis

Status: measured on `bot-5` (not applied anywhere). Date: 2026-10-24. Related: [infra-cost-migration.md](infra-cost-migration.md), [cost-estimation.md](cost-estimation.md), [pricing-strategy.md](pricing-strategy.md).

## Summary

- Each bot pod holds **~1 GB of live data** but uses **~4 GiB** of memory. The rest is JVM slack, not demand.
- The pods are sized by their **5 GiB request**, and requests decide how many nodes the autoscaler creates. Lowering the request is what saves money.
- Right-sizing the heap and requests could drop the autoscaler pool from 3 to 2 nodes (about €29/month), with no feature change and no guild left.
- Dropping `GUILD_PRESENCES` would save ~5% of live heap and kill the activity variables. Not worth it.
- 76-89% of installed guilds are dormant, but leaving them is worth only ~€27/month on top, and costs reactivations and goodwill. Defer.

## Concepts

| Term | Meaning |
|---|---|
| **Heap** | The memory area where the JVM keeps all Java objects (guilds, channels, roles, strings). Max set by `-XX:MaxRAMPercentage=60` of the 5 Gi limit = **3 GiB**. |
| **Live set** | Objects still reachable and needed. This is the real memory need. |
| **Garbage** | Objects nobody uses any more, waiting for the garbage collector (GC). They still occupy the heap until a GC runs. |
| **Used / committed / max** | *Used* = live + not-yet-collected garbage. *Committed* = memory the JVM has taken from the OS. *Max* = the cap. |
| **G1 GC** | The collector in use. It grows the heap up to the max when allocation is busy and rarely gives memory back to the OS. |
| **Non-heap** | Everything else the process needs: class metadata, thread stacks (one set per shard), network buffers, JIT code, GC bookkeeping. Estimated ~1 GiB here (RSS minus committed heap). |
| **RSS / working set** | What the pod actually occupies in RAM: heap committed + non-heap. What Kubernetes and `kubectl top` report. |
| **Request** | What the Kubernetes scheduler reserves for the pod (5 Gi). **Nodes are sized from requests, not from usage.** |
| **Limit** | The ceiling; above it the pod is OOM-killed. 5 Gi here. |

Analogy: the heap is a warehouse that grows when busy and is never shrunk. The goods you actually need fill 1/3 of it, but you rent all of it, plus the building around it (non-heap).

## Measurements (bot-5, 15,467 guilds)

| Measure | Value |
|---|---|
| Heap committed | 3 GiB (= the cap) |
| Heap used before GC | 1.78 GiB |
| **Live heap after full GC** | **~0.94 GiB** (989 MB histogram total) |
| Working set, 30-day min / max | 4,013 / 4,037 MiB |
| Live heap per guild | ~64 KB |

The 30-day working set is a flat line. Demand would move with load; a flat line at the cap means the JVM simply filled its allowance.

Largest live objects (histogram, bytes): `long[]` 229 MB, `byte[]` 153 MB, `Object[]` 146 MB, `PermissionOverrideImpl` 76 MB (1.58M objects, ~100 per guild), `TLongObjectHashMap` 65 MB, `String` 56 MB, `RoleImpl` 45 MB (698k), `TextChannelImpl` 27 MB (378k), `VoiceChannelImpl` 11 MB.

### Why the request matters

11 pods x 5 Gi = 55 GiB requested for the bot alone. A cx53 has ~30.7 GiB allocatable, so the bot needs 2+ nodes by itself and the autoscaler adds a third for everything else. Other pods use ~16 GiB (Prometheus, Redis, Longhorn, APIs, system).

## Proposal: right-size

| Setting | Now | Proposed |
|---|---|---|
| Heap max | 3 GiB (60% of 5 Gi) | ~1.5 GiB (`-Xmx1536m`, about 1.5x live set) |
| Memory request | 5 Gi | 3 Gi |
| Memory limit | 5 Gi | 3.5 Gi |
| Expected RSS | ~4.0 GiB | ~2.5 GiB (1.5 heap + ~1.0 non-heap) |

- Bot requests drop from 55 to 33 GiB. Plus ~16 GiB for other pods = ~49 GiB, which fits 2x cx53 (~61 GiB allocatable) at ~80%.
- Autoscaler pool: `min_nodes = 2`, `max_nodes = 3`. Saves one cx53 = **€29.49/month**. New total ~€172/month.
- Optional: `-XX:G1PeriodicGCInterval` so the heap returns unused memory, and Micrometer JVM metrics (none reach Prometheus today).
- Roll out on one pod first (not `bot-0`), watch RSS, GC pauses and OOM kills for a week, then the rest.
- Trade-off: with 2 nodes, losing a node takes bots down until the autoscaler adds one (minutes). Costs ~€29/month to avoid.

## Presence intent and the activity variables

Premium name variables `{activity_*}` come from member activities, delivered only via the `GUILD_PRESENCES` privileged intent (`PRESENCE_UPDATE`). Discord documents no other source. So **you cannot drop the intent and keep the variables.**

Memory the presence data uses (bot-5 histogram): `MemberPresenceImpl` 11.6 MB, `ActivityImpl` 8.3 MB, `RichPresenceImpl` 7.7 MB, `Activity$Timestamps` 2.3 MB = ~30 MB direct, ~40-50 MB with strings and lists. That is **~4-5% of live heap**, ~0.5 GiB across 11 pods. The intent's real costs are event traffic, CPU and GC churn, but CPU sits at ~6%, so none of it costs money today.

Verdict: keep it. A premium-only presence deployment (a separate gateway for only premium guilds' shards) is conceivable but untested and fragile; not worth the savings.

Sources: [Discord: you might not need a privileged intent](https://docs.discord.com/developers/gateway/you-might-not-need-a-privileged-intent), [JDA gateway intents](https://www.mintlify.com/discord-jda/JDA/api/utilities/gateway-intent).

## Dormant guilds

Installed: 167,485 (`GUILD_SNAPSHOT`, Sep 2026). "Active" = any VC generation, voice-role connection or slash command.

| No activity for | Guilds | Share |
|---|---|---|
| 3+ months | 148,575 | 89% |
| 6+ months | 139,664 | 83% |
| 12 months | 126,697 | 76% |

- Of 129.6k guilds idle Oct 2025-Mar 2026, **6,299 (4.9%) were active again Apr-Sep 2026** (~1,000/month). Leaving loses these unless they re-invite.
- 25 of 210 premium guilds are idle 6+ months and must be excluded.
- Leaving 140k guilds frees live heap and lets shards drop from 176 to ~11-28, but the ~1 GiB non-heap per JVM stays: ~€27/month on top of right-sizing.
- Discord forces every guild's data onto its shard, so guilds cannot be "lazily loaded". The only levers are leaving, caching less, or a leaner cache.

If done: 30 days notice to the owner or system channel, settings kept, re-invite restores them, 12-month threshold, premium excluded.

Cheaper code-side options (not measured): disable `CacheFlag.MEMBER_OVERRIDES` if no logic reads member overrides (check code first); a custom lean cache (e.g. Rust/Twilight) is the biggest lever but a rewrite, and with 1 GB live it is the least attractive step.

## Reproducing

```
kubectl debug -n astro bot-5 --image=eclipse-temurin:25-jdk --target=bot --profile=general -c heapdbg -- sleep 900
kubectl exec -n astro bot-5 -c heapdbg -- jcmd 1 GC.heap_info
kubectl exec -n astro bot-5 -c heapdbg -- jcmd 1 GC.class_histogram   # forces a full GC (brief pause)
```

The image is a JRE without `jcmd`, hence the ephemeral JDK container. Dormancy queries ran on BigQuery tables `GUILD_SNAPSHOT`, `TEMPORARY_VC_GENERATION`, `CONNECTION_INVOCATION`, `SLASH_COMMAND_INVOCATION`.

## Open questions

- Does any logic read member permission overrides (needed to decide on `MEMBER_OVERRIDES`)?
- What does G1 do at a 1.5 GiB heap under real load (pause times, allocation rate)? Needs JVM metrics.
- Native memory breakdown (`-XX:NativeMemoryTracking=summary`) to confirm the ~1 GiB non-heap estimate.
