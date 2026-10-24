# Premium: monthly VC quota model

Status: proposal, not implemented. Data: BigQuery `astro-405113.astro_bot`, Jan–Sep 2026 (full months), table `TEMPORARY_VC_GENERATION`.

> **Update 2026-10-24:** only 3 of 210 current premium guilds exceed the 475 quota (121 generated zero VCs in September). A pure quota would lose ~98% of paying guilds. See [pricing-strategy.md](pricing-strategy.md); keep feature gating and add usage as an extra layer.

## Proposal

Replace feature gating with a **monthly quota of generated temporary VCs per guild**. No features are paywalled. Guilds over the quota pay a monthly or yearly subscription.

## Models considered

| Model | Verdict |
|---|---|
| Feature gating (current) | Same price for light and heavy users. Value not tied to usage. |
| Config count caps (generators, templates, ...) | Measures setup, not value. Adds friction at onboarding. Drop. |
| **Monthly VC quota** | Chosen. Cost and value scale with usage. Small servers stay free. |
| Size-based tiers (member count) | Rejected. Size does not track usage or cost, and `GUILD_EVENT` has no continuous member count. |
| One-off VC packs | Weak recurring revenue. |
| Per-VC-owner premium | Does not fit the guild-based entitlement model. |

## Data

Usage is highly concentrated. Median guild generates 4 VCs/month.

| Month | Active guilds | VCs | p50 | p90 | p95 | p99 |
|---|---|---|---|---|---|---|
| Jun | 13.4k | 318k | 4 | 43 | 76 | 237 |
| Jul | 13.8k | 344k | 4 | 50 | 87 | 262 |
| Aug | 12.9k | 315k | 4 | 49 | 89 | 278 |
| Sep | 12.5k | 311k | 4 | 52 | 91 | 302 |

Quota simulation, Jan–Sep 2026. "Above quota" is the share of all VCs generated past each guild's quota.

| Quota/month | VCs/day | Guilds over (avg/month) | VCs above quota | Month-to-month range |
|---|---|---|---|---|
| 100 | 3.3 | 561 | 39.8% | 34–46% |
| 200 | 6.7 | 219 | 29.9% | 26–36% |
| 300 | 10 | 132 | 25.1% | 21–30% |
| 450 | 15 | 79 | 20.7% | 16–25% |
| **475** | **15.8** | **71** | **20.2%** | **16–25%** |
| 600 | 20 | 52 | 18.0% | 14–22% |
| 900 | 30 | 31 | 14.6% | 10–18% |
| 1,200 | 40 | 21 | 12.4% | 8–16% |

Findings:
- A quota of about **475/month puts 20% of VCs above quota**, and about 0.5% of active guilds hit it.
- 30–40/day (900–1,200/month) affects only 21–31 guilds a month, so the paying market is a few dozen guilds.
- Most over-quota guilds are one-off: of ~1,030 guilds that exceeded 200 in a month, 60% did it once. About 90 exceeded it in 5+ of 9 months.
- A handful of guilds drive volume. The top 10 above 200 account for ~43% of over-quota VCs. The largest generates 7.6k–9.2k VCs/month. Some may be bot-like.

## Recommendation

1. Quota of **450–500 VCs/month** (about 15/day).
2. Enforce only after the guild is over quota in **2 of the last 3 months**, so occasional spikes are not penalised.
3. **Soft cap**: once enforced, degrade (no chat or waiting room) before blocking VC creation. Blocking a utility bot mid-month is the worst failure.
4. Show a usage meter in the dashboard and warn at 80%.

## Open questions

- Price per month and year, and expected conversion among the ~70 over-quota guilds.
- Inspect the top ~20 guilds for bot-like or abusive usage before fixing the number.
- Anti-abuse: count only VCs that stay occupied for a minimum time, or add a per-user cooldown.
- Hard vs soft cap behaviour, and what happens to existing premium guilds.

## Data gaps to fix

- No VC duration or deletion event (needed for VC-hour metering and abuse filtering).
- No premium status over time (needed for conversion and churn).
- Config counts (generators, templates, interfaces) live only in MongoDB.
- Interface button clicks may not be tracked as invocations.
- `GUILD_SNAPSHOT` (`member_count`, `guild_name`, `shard_id`) exists but is not in the README.

## Reproducing

Queries ran with `bq --project_id=astro-405113 query --use_legacy_sql=false --maximum_bytes_billed=1000000000`. Each scanned about 76 MB (a fraction of a cent). The partition filter on `timestamp` is required.
