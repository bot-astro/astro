# Premium pricing

Date: 2026-10-24. Status: analysis, nothing decided. Data: MongoDB `AstroDB.guilds`, BigQuery `astro-405113.astro_bot` (Jan-Sep 2026). Costs: [infrastructure.md](infrastructure.md). Net figures assume 15% fees (to confirm) and $1 = €0.875.

## Where we stand

170 subscribers at $3.99 = ~€505/month net, against ~€356 cost today (~€172 after [infrastructure.md](infrastructure.md)). Already profitable; break-even is ~120 subs today, ~50 after. Each extra subscriber adds ~€2.97 net at ~no cost. Payers are 0.10% of 167k installed guilds, 1.4% of the 12.5k active ones: awareness and conversion are the bottleneck, not price or cost.

## Who pays today

210 premium guilds (matches ~170 subs; lapsed `upgradedByUserID` can't be excluded). September VCs by band:

| VCs / month | Guilds | Premium | Conversion |
|---|---|---|---|
| 0 | ~155k | 121 | 0.08% |
| 1-49 | 11,197 | 61 | 0.5% |
| 50-99 | 795 | 16 | 2.0% |
| 100-199 | 320 | 8 | 2.5% |
| 200-499 | 145 | 1 | 0.7% |
| 500+ | 62 | 3 | 4.8% |

Payers pay for **features, not volume**: 182 of 210 generate under 50 VCs/month and only 3 exceed 475. Usage is highly concentrated (median guild: 4 VCs/month; p99: ~300), volume is flat since the April peak (~311k VCs/month), and the top 10 heavy guilds make ~43% of over-quota volume.

## Decisions

| Question | Decision | Why |
|---|---|---|
| Replace feature gating with a VC quota (~475/month)? | **No** | Loses ~98% of payers (revenue ~€505 → ~€37). Only ~70 guilds exceed it; 100% converting at $3.99 gives ~€208. |
| Permanent price cut? | **No** | $2.99 needs +33% subs to hold revenue (+101% for $1.99). Test a time-limited promo or annual discount instead; any cut must also apply to existing subscribers. |
| Usage-based plan on top of features? | **Yes, as a staged pilot** | Upside +17% to +67% revenue (5-20% of the 1,322 guilds at 50+ VCs converting), bounded downside because Premium stays as is. |

## Usage-based plan: staged pilot

Idea: keep Premium at $3.99 and add a volume plan for guilds above ~50 VCs/month. Tier sketch: 50-99 $1, 100-199 $2, 200-299 $3, 300-399 $4, 400+ $5 (~$2,263 gross at 100% conversion).

| Phase | What | Duration | Billing change |
|---|---|---|---|
| 0. Shadow | Dashboard usage meter (warn at 80%); log the tier each guild would fall in on a trailing 3-month average. Enforce nothing. | 4-6 weeks | None |
| 1. Nudge pilot | Banner, owner DM or VC-chat footer pointing heavy non-paying guilds (203 guilds at 200+ VCs, 4 premium) at Premium, with a control group. | 4-6 weeks | None |
| 2. One tier | One volume plan (e.g. $2.99 for 100+ VCs, 3-month average), soft limits, existing payers grandfathered. | 6-8 weeks | One new SKU |
| 3. Full tiers | 5-tier ladder, test a top tier above $5 (62 guilds at 500+ VCs, up to ~9k). | After 2 | More SKUs |

Go/no-go, set before starting: nudged guilds converting at 5%+ vs control continues, under 2% stops. Phase 2 expands if net revenue is up 10%+ without more complaints or removals.

Design rules:
- Use a trailing 3-month average (60% of guilds above 200 VCs do it once); avoid cliffs (99 vs 100 VCs); degrade (no chat/waiting room) before blocking VC creation.
- The $1 tier is thin (~€0.74 net) and the 50-99 band is the lowest value (2-3 VCs/day).
- Discord entitlements are fixed SKUs: one per tier plus upgrade/downgrade flows, or Chargebee metered billing. Check the fee on each path.
- Fairness: grandfather payers, no bill shock, warn before removing the bot from a server.
- Anti-abuse: some top guilds may be bot-like; inspect the top ~20 and consider counting only VCs occupied for a minimum time.

## Next steps

1. Find out why 121 zero-VC payers pay (voice-role connections? lapsed `upgradedByUserID`?) and why 25 are idle 6+ months. One query.
2. Snapshot the premium guild list monthly into BigQuery and log VC duration, for churn, LTV and conversion.
3. Contact the top ~60 guilds (500+ VCs) personally; 59 don't pay.
4. Start phase 0, then phase 1.

Open questions: platform fee per path (Discord vs Chargebee) and fixed per-transaction fees; whether $3.99 is net of VAT; how many guilds exceed 1,000 VCs/month; overlap between payers and heavy guilds.

## Data and queries

- BigQuery tables: `TEMPORARY_VC_GENERATION`, `SLASH_COMMAND_INVOCATION`, `CONNECTION_INVOCATION`, `GUILD_EVENT` (JOINED/KICKED), `GUILD_SNAPSHOT` (member_count, guild_name, shard_id; ~167k installed, stops mid-Sept). Always pass a `timestamp` partition filter:

  ```sh
  bq --project_id=astro-405113 query --use_legacy_sql=false --maximum_bytes_billed=1000000000 '...'
  ```

  A monthly VC query scans ~76 MB (a fraction of a cent).
- Premium guilds: `mongosh`, DB `AstroDB`, collection `guilds`. Premium = `upgradedByUserID` set, or an `entitlements` element with `endsAt` null or in the future. Don't print the connection string.
- Gaps: no VC duration/deletion event, no premium status over time, interface clicks may not be tracked, config counts live only in MongoDB.
- Not yet used: Chargebee (real subscription history and churn).
