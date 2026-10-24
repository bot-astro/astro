# Infrastructure cost migration: cpx to cx

Status: proposal, nothing applied. Data: `hcloud` CLI (context `gport`) and `kubectl top`, 2026-10-24. Prices are net (excl. VAT), per month, nbg1. Infra lives in `Giuliopime/gport` (`hcloud-cluster/kube-hetzner.tf`).

## Why the bill went up

Hetzner raised CPX prices on 15 June 2026 (CPX52: €36.49 to €100.49). Servers created before that date keep the old price, new ones get the new price.

| Server | Created | Price |
|---|---|---|
| `gport-agent-as-cruiser-15fcecf57c624619` | 2026-02-28 | €36.49 |
| `gport-agent-as-cruiser-3000c09f2b5fa9e6` | 2026-03-02 | €36.49 |
| `gport-agent-as-cruiser-648ce9160ed59a05` | 2026-08-06 | €100.49 |

The autoscaler pool (`min_nodes = 3`) creates every replacement node at the new price. If the two old nodes are ever replaced or rescaled, that is another €128/month. Hetzner also did a separate increase on 1 April that applied to existing servers.

Sources: [Hetzner price adjustment](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/), [Northflank](https://northflank.com/blog/hetzner-cloud-server-price-increases), [WZ-IT](https://wz-it.com/en/blog/hetzner-price-increase-june-2026-cpx-ccx-alternatives).

## Current usage

The workload is memory-bound, not CPU-bound.

| Node | Type | CPU | Memory |
|---|---|---|---|
| 3x `agent-as-cruiser` | cpx52 (12c/24G) | 5-7% | 85-93% |
| `agent-sailboat` | cpx32 (4c/8G) | 4% | 76% |
| 3x `control-plane` | cpx22 (2c/4G) | 5-10% | 117-129% of allocatable (~2 GiB) |

- 11 `bot` pods (one per shard) use ~4 GiB each (~45 GiB total). They request 5 GiB and 1 CPU, and use ~0.12 CPU.
- Longhorn volumes total ~42 GB in use (Redis 30 GB, Prometheus 10 GB), single replica.
- Location is not a lever: nbg1, fsn1 and hel1 cost the same. Only ash, hil and sin are more expensive.

## Proposal

Move to Hetzner's cost-optimised `cx` line (same RAM, much cheaper, not affected by the June increase).

| Role | Now | Proposed | Now | New | Saving |
|---|---|---|---|---|---|
| 3x control plane | cpx22 (2c/4G) | cx23 (2c/4G) | €19.49 each | €5.49 each | €42 |
| 1x agent | cpx32 (4c/8G) | cx33 (4c/8G) | €35.49 | €8.49 | €27 |
| 3x autoscaler | cpx52 (12c/24G) | cx53 (16c/32G) | €173.47 total | €29.49 each (€88.47) | €85 |

| | Monthly |
|---|---|
| Compute now | ~€267 |
| Compute after | ~€113 |
| 2x lb11 (unchanged) | €13.98 |
| **Total now / after** | **~€281 / ~€127** |
| **Saving** | **~€154 (~55%)** |

IPv4 charges are not included in either figure and do not change. MongoDB Cloud (~$80/month) is also unchanged.

Notes:
- Keep 3 cx53: 96 GiB of RAM against ~61 GiB used. Two nodes (64 GiB) would be too tight.
- cx53 has 320 GB of disk against 480 GB; Longhorn usage fits.
- All x86, so no ARM image work. cx has slower CPUs than cpx, which is irrelevant at ~6% CPU.
- Control-plane nodes are already memory-starved at 4 GB. cx23 keeps parity; consider cx33 (8 GB, €8.49) if etcd or the API server gets OOM-killed.

## Rollout

1. Change `server_type` in `kube-hetzner.tf` for the autoscaler pool, one node first.
2. Watch shard latency and memory for a few days before replacing the rest.
3. Replace `agent-sailboat`, then the control plane one node at a time, keeping etcd quorum (3 nodes, never more than 1 down).
4. Drain and cordon each node before it is removed (per the module's guidance).

## Step 2: right-size bot pods (~€29/month more)

Live heap is ~1 GB per pod but pods request 5 Gi and sit at ~4 GiB. Lowering heap max to ~1.5 GiB and the request to 3 Gi lets the autoscaler pool go from 3 to 2 cx53 (`min_nodes = 2`), for ~€172/month total. Details and measurements: [bot-memory-analysis.md](bot-memory-analysis.md).

## Other savings (not costed)

- The bot uses ~4 GiB per shard with no JVM flags set in the StatefulSet. Capping the heap and trimming Discord caches (members, presences) could cut that substantially, and might allow fewer or smaller nodes.
- A Rust rewrite would likely shrink memory several-fold but is not worth it for cost alone: the cx swap saves more, and heap/cache tuning gets most of the memory win at a fraction of the effort.

## Open questions

- Real IPv4 and load balancer line items from the invoice, for an exact total.
- Do the bot pods need 5 Gi requests, or can the request drop towards the observed ~4.2 GiB?
- Is `cx` capacity reliably available in nbg1 when the autoscaler scales up?
