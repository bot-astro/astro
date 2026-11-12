# Bot memory improvement plan

Status: not applied. The new version deploys with the **same memory config as the legacy one**: bot pods request/limit `5Gi` (1 CPU), heap `MaxRAMPercentage=60` (3 GiB). Measurements and cost context are in `infrastructure.md`.

## Why
- Nodes are sized by **requests**, not usage: 11 bot pods x 5 Gi = 55 GiB forces a third autoscaler node.
- Measured on `bot-5` (15,467 guilds): live heap after a full GC is ~0.94 GiB, but the heap is capped at 3 GiB, G1 fills it, and the working set is a flat ~4 GiB over 30 days. The rest is JVM slack.

## Target

| Setting | Now | Target |
|---|---|---|
| Heap max | 3 GiB (`MaxRAMPercentage=60` of 5 Gi) | ~1.5 GiB (`-Xmx1536m`, ~1.5x live set) |
| Memory request / limit | 5 Gi / 5 Gi | 3 Gi / 3.5 Gi |
| Expected RSS | ~4.0 GiB | ~2.5 GiB (1.5 heap + ~1.0 non-heap) |

Bot requests drop from 55 to 33 GiB, which with ~16 GiB for other pods fits 2 cx53 nodes (~61 GiB allocatable) at ~80%. Then set the autoscaler to `min_nodes = 2`, `max_nodes = 3` (in `gport`). Trade-off: losing a node takes bots down for minutes until the autoscaler replaces it.

## Where to change it
- Heap: `jvmFlags` in `build-logic/convention/src/main/kotlin/service-conventions.gradle.kts` (shared by all services, so either make it per service or use `-Xmx` only for the bot).
- Requests/limits: `apps/bot/envs/astro/resources.yml` in the `infra` repo.
- Node count: `gport` autoscaler pool.

## Rollout
1. Export JVM metrics (Micrometer) and optionally add `-XX:NativeMemoryTracking=summary` to confirm the ~1 GiB non-heap estimate. Optionally `-XX:G1PeriodicGCInterval`.
2. Change **one pod first** (not `bot-0`; a StatefulSet applies one spec to every pod, so test it on staging, or use a canary via `updateStrategy.rollingUpdate.partition` set to 10 so only `bot-10` rolls).
3. Watch RSS, GC pauses, OOM kills and shard latency for a week.
4. Roll to the rest, then lower the autoscaler minimum.

## Other ideas (unmeasured)
- Disable `CacheFlag.MEMBER_OVERRIDES` if nothing reads member overrides (`PermissionOverrideImpl` is 76 MB, ~100 per guild).
- Keep `GUILD_PRESENCES` (only source of the `{activity_*}` premium variables, ~30-50 MB per pod).
