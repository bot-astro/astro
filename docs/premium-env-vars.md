# Premium: env vars to add to Infisical

Read by both `bot` and `api-central` through `PremiumProperties` (`premium.*`).

| Variable | Required | Value |
|---|---|---|
| `PREMIUM_SERVER_SKU_ID` | yes | Discord server premium SKU id. Old default was `1096107722115661934`. If empty, no guild is ever premium. |
| `PREMIUM_MONTHLY_PLAN_ID` | yes | Chargebee plan id, old default `Server-Premium-USD-Monthly` |
| `PREMIUM_YEARLY_PLAN_ID` | yes | Chargebee plan id, old default `Server-Premium-USD-Yearly` |
| `PREMIUM_RESTRICTIONS` | no | Defaults to `true`. Set `false` (e.g. staging) to treat every guild as premium. |

Add them to the `bot` and `api-central` secret paths for staging and production.

Already used, check they exist for `api-central`: `CHARGEBEE_SITE_NAME`, `CHARGEBEE_API_KEY`, `CHARGEBEE_SERVER_PREMIUM_PLAN_ID`.
`support-bot` has its own `SB__SERVER_PREMIUM_ROLE_ID` and `SB__SERVER_PREMIUM_SKU_ID` (the SKU id should match `PREMIUM_SERVER_SKU_ID`).
