# Cost estimation: proposed architecture

Status: estimate. Infra from `hcloud` (prices fetched via the Hetzner API, nbg1, net of VAT), usage from BigQuery `TEMPORARY_VC_GENERATION`, 2026-10-24. Architecture: see [infra-cost-migration.md](infra-cost-migration.md). Quota model: see [premium-quota-model.md](premium-quota-model.md).

## Monthly cost

| Item | Now | Proposed |
|---|---|---|
| Control plane (3x) | 3x cpx22 = €58.47 | 3x cx23 = €16.47 |
| Agent (1x) | cpx32 = €35.49 | cx33 = €8.49 |
| Autoscaler (3x) | 2x cpx52 @ €36.49 + 1x cpx52 @ €100.49 = €173.47 | 3x cx53 @ €29.49 = €88.47 |
| Load balancers | 2x lb11 = €13.98 | €13.98 |
| IPv4 | 9 @ €0.50 = €4.50 | €4.50 |
| **Hetzner** | **€285.91** | **€131.91** |
| MongoDB Cloud | ~€70 ($80, rate assumed) | ~€70 |
| **Total** | **~€356** | **~€202** |

VAT (22%) would add ~€44/month to the proposed total if it is not recoverable. Not included: Cloudflare, BigQuery (cents per query), domains, Chargebee, backups. The IPv4 line assumes the two load-balancer IPs are billed too; they may be included in the lb11 price.

## Usage

| Month | VCs | Active guilds |
|---|---|---|
| Jan | 305k | 13.8k |
| Feb | 247k | 12.8k |
| Mar | 383k | 14.6k |
| Apr | 491k | 14.9k |
| May | 450k | 14.6k |
| Jun | 318k | 13.4k |
| Jul | 344k | 13.8k |
| Aug | 315k | 12.9k |
| Sep | 311k | 12.5k |

Volume is flat to declining since the April peak.

## Unit cost (Sep: 311k VCs, 12.5k guilds)

| | Now | Proposed |
|---|---|---|
| Per 1,000 VCs | €1.14 | €0.65 |
| Per active guild per month | €0.028 | €0.016 |
| Median guild (4 VCs/month) | €0.005 | €0.003 |

Cost is mostly fixed. It is driven by shard memory (guild count and Discord caches, ~4 GiB per shard), not by VCs generated. Per-VC figures are an allocation, not marginal cost: the 20% of VCs above a 475 quota cost no more to serve than the rest.

## Break-even

Assumes a ~15% blended payment/platform fee (Discord entitlements, Chargebee); confirm the real figure. Target: cover ~€202/month.

| Price/month | Net per sub | Subscribers to break even |
|---|---|---|
| €3 | €2.55 | 80 |
| €5 | €4.25 | 48 |
| €8 | €6.80 | 30 |
| €10 | €8.50 | 24 |

The quota model targets ~70 over-quota guilds. Revenue by conversion at €5/month:

| Conversion of 70 | Subscribers | Net revenue | Share of cost covered |
|---|---|---|---|
| 30% | 21 | €89 | 44% |
| 50% | 35 | €149 | 74% |
| 100% | 70 | €298 | 147% |

Break-even price for a given conversion: 21 subs = €11.31, 35 subs = €6.79, 70 subs = €3.39.

## Actual revenue

170 subscribers at $3.99/month, 15% fees, $1 = €0.875 (the rate implied by the MongoDB conversion above).

| | USD | EUR |
|---|---|---|
| Gross | $678 | €593 |
| Net of 15% fees | $577 | €505 |

| | Now | Proposed |
|---|---|---|
| Cost | €356 | €202 |
| Profit | €149 | €303 |
| Margin | 29% | 60% |
| Cost per subscriber | €2.09 | €1.19 |

Cost is covered 1.4x today and 2.5x after the migration. Break-even is ~120 subscribers today and ~68 after (at €3.49 net each). The 170 payers are ~1.4% of active guilds.

Risk for the quota model: payers today pay for features (and count caps), not volume. Most of the 170 are probably under 475 VCs/month, so a quota with no paywalled features would give them no reason to pay. Only ~70 guilds exceed the quota, and 100% of them converting at $3.99 yields ~€208, about the proposed cost. Overlap between current payers and over-quota guilds is unknown; check it before dropping feature gating.

## Corrections (installed guilds, premium reality)

- The bot is in **167,485 guilds** (`GUILD_SNAPSHOT`, Sep 2026); only 12.5k generated a VC in September. Per installed guild, cost is **€0.0012/month** (€202) and revenue €0.0030. Marginal memory cost of one more guild is ~€0.0003/month.
- Live heap is ~1 GB per pod while pods request 5 Gi; right-sizing saves another ~€29/month: **~€172/month total**, profit ~€333. See [bot-memory-analysis.md](bot-memory-analysis.md).
- Only 3 of 210 premium guilds exceed 475 VCs/month, so a pure quota would lose ~98% of paying guilds. See [pricing-strategy.md](pricing-strategy.md).

## Takeaways

- The migration saves ~€154/month and roughly doubles profit (€149 to €303). Do it regardless of pricing.
- The business is already profitable. The pricing question is revenue protection, not covering costs: don't switch to a quota model that loses current payers.
- The 70-guild quota market alone only covers costs at high conversion or a price of €7+; the 170 existing payers are what keep it profitable.
- Quota size barely affects cost. Pick it for revenue and fairness, not infra.
- Most leverage left is memory per shard: heap and cache tuning could shrink the autoscaler pool further.

## Open questions

- How many of the 170 payers are over 475 VCs/month (join MongoDB premium guilds with BigQuery).
- Whether $3.99 is net of VAT/sales tax, and the real fee split between Discord entitlements and Chargebee.
- EUR/USD rate for MongoDB and revenue, and whether VAT on costs is recoverable.
- Real IPv4 line items on the invoice.
