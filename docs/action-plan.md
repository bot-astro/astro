# Action plan

Date: 2026-10-24. Summary of [infra-cost-migration.md](infra-cost-migration.md), [bot-memory-analysis.md](bot-memory-analysis.md), [cost-estimation.md](cost-estimation.md) and [pricing-strategy.md](pricing-strategy.md). Numbers are net per month.

## Where you stand

| | Now | After steps 1-2 |
|---|---|---|
| Cost | ~€356 | ~€172 |
| Revenue (170 subs) | ~€505 | ~€505 |
| Profit | ~€149 | ~€333 |

## Do now (no decision needed)

| # | Action | Gain | Risk | Effort |
|---|---|---|---|---|
| 1 | Swap server types: control plane cpx22 to cx23 (or cx33), agent cpx32 to cx33, autoscaler cpx52 to cx53. Roll one node at a time, keep etcd quorum, autoscaler first. | ~€154 | Low | Hours |
| 2 | Right-size bot pods on one pod (not `bot-0`): heap ~1.5 GiB, request 3 Gi, limit 3.5 Gi. Watch a restart (startup load can peak above steady state), then roll out and set autoscaler `min_nodes = 2`. | ~€29 | Low-medium (OOM if too tight) | Days, plus a week of watching |
| 3 | Export JVM metrics (Micrometer) to Prometheus; optionally enable `-XX:NativeMemoryTracking=summary`. Makes step 2 verifiable. | Confidence | None | Hours |
| 4 | Move the Mongo connection string from the `bot-config` ConfigMap to a Secret. | Security | None | Hours |
| 5 | Snapshot the premium guild list monthly into BigQuery (and log VC duration). Enables churn, LTV and conversion tracking. | Data | None | Hours |

## Revenue: the low-regret moves

| # | Action | Expected | Risk |
|---|---|---|---|
| 6 | **Nudge heavy non-paying guilds.** 203 non-paying guilds generate 200+ VCs/month (207 total, 4 premium). A dashboard banner, owner DM or footer in their VC chat pointing at Premium. At 10-20% conversion: +€60-120/month. | +€60-120 | Low (existing price and model) |
| 7 | Find out why 121 zero-VC payers pay (voice-role connections? lapsed `upgradedByUserID`?) and why 25 payers are idle 6+ months. A 15-minute query. | Churn insight | None |
| 8 | Contact the top ~60 guilds (500+ VCs) personally; 59 of them do not pay. | Learning plus some subs | None |

## Decisions to take

| Decision | Recommendation | Why |
|---|---|---|
| Keep feature gating and $3.99? | **Yes** | Only 3 of 210 payers exceed the quota; a replacement loses ~98% of payers. |
| Permanent price cut? | **No** | $2.99 needs +33% subs to break even; the bottleneck is awareness (0.10% of installed), not price. Test a time-limited promo instead. |
| Usage-based model | **Additive pilot, not a leap** (below) | Upside +17-67% revenue, bounded downside if features stay. |
| Keep `GUILD_PRESENCES`? | **Yes** | The only source of activity data; saves ~5% heap if dropped. |
| Leave dormant guilds? | **Defer** | ~€27/month extra; 4.9% reactivate within 6 months. Revisit after step 2. |
| Rust rewrite? | **No** | Live heap is only 1 GB; right-sizing gets most of the gain. |
| 2 vs 3 autoscaler nodes | 2, with ~€29/month saved | Losing a node means a few minutes of downtime until the autoscaler replaces it. |

## Usage-based model: turning the leap into an experiment

The hard part is that nobody knows how heavy guilds will react. So measure before billing anything. Costs of each phase are near zero because infra is fixed.

| Phase | What | Duration | Billing change |
|---|---|---|---|
| 0. Shadow | Show a usage meter in the dashboard (80% warning). Log which tier each guild would fall in on a trailing 3-month average. Enforce nothing. | 4-6 weeks | None |
| 1. Nudge pilot | Step 6 above, with a control group of similar heavy guilds that see nothing. Measure conversion in both. | 4-6 weeks | None (existing $3.99) |
| 2. One tier | If the pilot works, add a single volume plan (e.g. $2.99 for 100+ VCs on the 3-month average). Soft limits only. Grandfather existing payers. | 6-8 weeks | One new SKU |
| 3. Full tiers | Expand to the 5-tier ladder and test a top tier above $5. | After 2 | More SKUs |

Decision rules, set before you start:
- Phase 1: nudged heavy guilds convert at 5% or more (vs control) means continue; under 2% means stop and keep the current model.
- Phase 2: net revenue up 10% or more and no visible rise in complaints or removals means expand.

Why this is lower regret than a full launch: every phase keeps Premium as is, so the worst case is some irritated heavy guilds and a few weeks of work, not a revenue collapse. The costs the model brings are Discord SKU setup per tier, upgrade and downgrade flows, and fairness work (grandfathering, soft caps).

## Order of work

1. Week 1: steps 1, 3, 4, 5, 7.
2. Weeks 1-2: step 2 on one pod, then the rest.
3. Weeks 2-8: steps 6 and 8, with the dashboard usage meter (phase 0).
4. Week 8+: review the pilot results and decide on phase 2.

## Open questions

- Platform fee per path (Discord entitlements vs Chargebee) and any fixed per-transaction fee.
- Whether a 2-node autoscaler pool is acceptable for availability.
- How many guilds exceed 1,000 VCs/month (for a top tier above $5).
