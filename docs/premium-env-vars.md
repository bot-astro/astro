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

## Support bot calls and Chargebee webhook

`bot` and `api-central` call the support bot API to add/remove the premium role (`SupportBotApiClient`):

| Variable | Services | Value |
|---|---|---|
| `SUPPORT_BOT_ENDPOINT_BASE_URL` | `bot`, `api-central` | In-cluster url of the support bot service, e.g. `http://support-bot.<namespace>:<SB__PORT>` |
| `SUPPORT_BOT_ENDPOINT_AUTH` | `bot`, `api-central` | Same value as `SB__API_AUTH` of the support bot |
| `VOTE_TRACKER_TOKEN` | `bot` | Vote Tracker integration API token (vote locks) |
| `VOTE_TRACKER_INTEGRATION_ID` | `bot` | Id of the bot's Vote Tracker integration |
| `CA__CHARGEBEE_WEBHOOK_TOKEN` | `api-central` | Password of the basic auth configured on the Chargebee webhooks (the old `chargebee.webhookToken`) |

Update the Chargebee webhooks to the new urls (basic auth, username `chargebee`, password = `CA__CHARGEBEE_WEBHOOK_TOKEN`):
- subscription created: `/v2/chargebee/events/subscription/create`
- subscription cancelled: `/v2/chargebee/events/subscription/cancel`
