# Pricing strategy

Status: analysis, nothing decided. Date: 2026-10-24. Data: MongoDB `AstroDB.guilds` (premium list), BigQuery `astro-405113.astro_bot`, `hcloud`. Related: [premium-quota-model.md](premium-quota-model.md), [cost-estimation.md](cost-estimation.md), [bot-memory-analysis.md](bot-memory-analysis.md).

## Core business principles

1. **Price follows value; cost is only the floor.** Customers pay for what the bot does for them, not for what it costs you. Costs say what you must earn to survive (~€172-202/month); they should not set the price.
2. **Fixed costs mean operating leverage.** Infra is almost fully fixed (step-wise by memory). Each extra subscriber adds ~€2.97 net at ~no cost, and one more guild costs ~€0.0003/month in memory. Growth in conversion beats further cost cutting: the migration saves €154/month, right-sizing €29, leaving dormant guilds ~€27, versus +€150-340/month from better conversion.
3. **Freemium economics.** 167k installed guilds are your marketing. The metric that matters is conversion (170 payers = 0.10% of installed, 1.4% of active) and what is gated.
4. **Do not break what pays you.** Know who pays and why before changing the model. Today payers pay for features, not volume (see below).
5. **Segment customers.** One price for light and heavy users leaves money on the table and overcharges small guilds. Good-better-best tiers capture both.
6. **Price changes are asymmetric.** Cuts are easy and need large volume gains to pay back; raises upset existing customers. Test reversibly (promo, new tier) before changing the base price.
7. **Measure before acting.** Missing today: premium history over time, VC duration, JVM metrics, interface clicks. Each gap hides a decision.
8. **Fairness and trust.** Grandfather payers, keep billing predictable (no bill shock), degrade before blocking, and warn before removing a bot from a server.
9. **Platform dependence.** Discord controls fees, API changes and privileged intents (`GUILD_PRESENCES`). Concentration risk: the top 10 guilds above 200 VCs make ~43% of over-quota volume.

## Reality check: who pays today

210 premium guilds (176 with `upgradedByUserID`, 37 with a valid entitlement, 2 both, 1 without `guildID`). Roughly matches your ~170 subscribers; lapsed `upgradedByUserID` values cannot be excluded.

VCs generated in September by guild band, and how many of each band are premium today (guilds with zero VCs: 121 of the 210 premium guilds):

| VCs / month | Guilds | Premium now | Conversion |
|---|---|---|---|
| 0 | ~155k | 121 | 0.08% |
| 1-49 | 11,197 | 61 | 0.5% |
| 50-99 | 795 | 16 | 2.0% |
| 100-199 | 320 | 8 | 2.5% |
| 200-299 | 80 | 1 | 1.3% |
| 300-399 | 47 | 0 | 0% |
| 400-499 | 18 | 0 | 0% |
| 500+ | 62 | 3 | 4.8% |

- **Only 3 of 210 payers exceed the 475 quota** of the quota-model doc. Replacing feature gating with a pure quota would lose ~98% of paying guilds.
- 182 of 210 payers generate under 50 VCs/month. They pay for features (or goodwill), not usage.
- Conversion rises with usage, but only to 2-5% even among heavy users. Heavy users are not being captured by feature gating.

## Lowering the price

Revenue to hold today's ~€505/month net (170 subs; net per sub = price x 0.85 x 0.875):

| Price | Net per sub | Subs needed | Change |
|---|---|---|---|
| $1.99 | €1.48 | 341 | +101% |
| $2.99 | €2.22 | 227 | +33% |
| **$3.99** | **€2.97** | **170** | — |
| $4.99 | €3.71 | 136 | -20% |
| $5.99 | €4.46 | 114 | -33% |

A cut to $2.99 costs ~€126/month on the current base. Costs do not rise with more subs, so the risk is only lost revenue per existing payer. With 0.10% conversion of installed guilds, awareness and perceived value are the bottleneck, not price. Prefer a time-limited promo or annual discount (reversible) over a permanent cut. A cut must also apply to existing subscribers.

## Usage-based tiers (the idea)

Proposed: 50-99 VCs $1, 100-199 $2, 200-299 $3, 300-399 $4, 400+ $5 (cap).

Addressable market (September): 1,322 guilds at 50+ VCs. Revenue at 100% conversion: 795x$1 + 320x$2 + 80x$3 + 47x$4 + 80x$5 = **$2,263 gross** (avg $1.71 per guild).

### As a replacement for feature gating: no

Existing payers at 50+ VCs (28 guilds) would pay $50 instead of $111.72. The other 182 would pay nothing. Revenue falls from ~€505 to ~€37/month (-93%). Matching today's revenue with tiers alone needs ~396 payers = 30% of all 50+ guilds.

### As an addition on top of features: promising

Keep Premium at $3.99 (features) unchanged, and require a volume plan above 50 VCs/month. Extra revenue if existing payers are retained (no cannibalisation):

| Conversion of the 1,322 | Extra gross | Extra net / month | vs today (€505) |
|---|---|---|---|
| 5% | $113 | €84 | +17% |
| 10% | $226 | €168 | +33% |
| 20% | $453 | €337 | +67% |
| 30% | $679 | €505 | +100% |

Usage-gated freemium (hard-ish limit) typically converts better than feature gating, which is 2-5% among heavy users today. Realistic range: 10-20%.

### Design notes

- **Use a trailing 3-month average**, not the current month. 60% of guilds that exceed 200 VCs do it once; month-to-month tiers would flap and cause bill shock.
- **Avoid cliffs**: 99 VCs at $1 and 100 at $2 invites gaming. Soft thresholds with a grace period.
- **Soft cap**: degrade (no chat/waiting room) before blocking VC creation.
- **The cap is probably too low**: 62 guilds at 500+ VCs (up to ~9k) pay the same $5 as 400. Test a higher top tier; their value per month is 10-20x the median payer's.
- **$1 tier is thin**: after fees ~€0.74 net, plus support cost per sub. The 50-99 band is the largest (795), but it is also the lowest value (2-3 VCs/day).
- **Discord entitlements are fixed SKUs.** Tiers need one SKU per tier plus upgrade/downgrade flows, or Chargebee metered billing. Check the platform fee on each path.
- **Grandfather existing payers** at their current terms for a fixed period.
- **Shadow mode first**: compute tiers for a month, show the usage meter, warn at 80%, and measure how many guilds would hit each tier before enforcing anything.

## Recommendation

1. Keep feature gating and the $3.99 price. Do not replace it with a quota.
2. Add a usage-based volume plan on top, gated on a trailing 3-month average with soft limits, starting in shadow mode.
3. Test a higher top tier for the 500+ band.
4. Do the infra work ([infra-cost-migration.md](infra-cost-migration.md), [bot-memory-analysis.md](bot-memory-analysis.md)); they are free profit regardless of pricing.
5. Fix the data gaps before launch: premium history over time, VC duration, interface clicks.

## Open questions

- Are the 121 zero-VC payers heavy voice-role (connection) users, or lapsed subscribers?
- Real platform fee per path (Discord entitlements vs Chargebee) and fixed per-transaction fees at $1.
- How many guilds exceed 1,000 VCs/month (for a higher top tier)?
- Do heavy free guilds actually value the bot enough to pay? A survey or a trial tier would answer it.
