# Growth work: plan and summary

Implements the code part of the business growth plan (`plans/business-growth.md`, not in this repo). Install attribution comes first, because every other growth idea needs to be measured by where installs come from. The website side is in `astro-bot.space` PR #48.

## Plan

The checklist was split into three parallel packages, each in its own worktree and draft PR, plus a research package that wrote only drafts and reports.

| Package | Where | Content |
|---|---|---|
| Backend | this PR | Install attribution, analytics views, `/help` link, interface changes |
| Website | astro-bot.space #48 | `/invite?src=<source>` redirect |
| Research and drafts | `plans/drafts/` (outside git) | Data report (servers, revenue, churn, vote guard), listing copy, outreach and Reddit templates, YouTube script, translator call |

Contract shared by the packages:
- `GET astro-bot.space/invite?src=<source>` redirects to api-central `/v2/auth/discord` with `source=<src>`.
- Allowed sources: `topgg`, `dbl`, `interface`, `help`, `web-guide`, `youtube`, `reddit`, `partner`. Anything else or missing is stored as `unknown`.
- `source` travels in the server-side OAuth state. On the callback an install event is written to BigQuery with source, guild id and inviter id.

## What this PR contains

1. **Install attribution** (api-central): `AuthController.discord` takes `source`, keeps it in the Redis OAuth state (`OAuthStateService`, format `source:path`) and on callback writes an `INSTALL_EVENT` row through the shared `BigQueryService` (shared core) (disabled by default, `CA__BIGQUERY_ENABLED`, `CA__BIGQUERY_DATASET_NAME`). `GuildEventData` has a new `locale`, filled on join and removal.
2. **Analytics views** in `docs/analytics/queries`: `daily_growth`, `removals_by_age`, `installs_by_source`.
3. **`/help` link**: a field "Add Astro to your server" pointing to the direct install link with `state=help`.
4. **Invites from Discord**: every invite link inside Discord (`/help` field) is the direct Discord install URL (`Links.invite(source)`), with `redirect_uri=https://astro-bot.space/invite/callback`, `scope=bot applications.commands`, `permissions=8` and the source as `state`. Discord opens the install prompt in the app and then the browser at the website callback, which forwards the code to api-central `GET /v2/auth/discord/install`. It exchanges the code (so the guild id can't be spoofed), writes the `INSTALL_EVENT` with the source (the inviter id is null, the scope has no `identify`) and redirects to the dashboard of the guild. The redirect URI must be registered in the Discord application.
5. **Interface**: unchanged apart from the `/help` link, no invite link inside it. The interface is not set as the VC chat message by default.

## Tried and removed

Added at first, then removed after review (the history keeps the commits and the reverts):
- Ephemeral "Add Astro" reply after a VC rename or lock, with its service and i18n keys.
- VoiceMaster and TempVoice detection in the inviter onboarding DM.
- Minimal invite permissions flag. The invite keeps requesting Administrator (`permissions=8`), because Astro needs it in most cases.

The search-intent guide and the Astro vs VoiceMaster page were also added to the website PR and reverted.

## To do before or after merging

- **Terraform**: create the `INSTALL_EVENT` table (nullable `user_id`) and add a nullable `locale` column to `GUILD_EVENT`, or the join, removal and install inserts are rejected.
- **Prod config** on api-central: `CA__BIGQUERY_ENABLED`, `CA__BIGQUERY_DATASET_NAME` and Google credentials.
- **Views**: create `daily_growth`, `removals_by_age` and `installs_by_source` in BigQuery like the existing ones.
- **Translations**: only en-US was edited. After the wording is approved, sync the other languages (new keys: `interface.invite`, the `/help` field).
- **Listings**: point the top.gg and discordbotlist invite buttons to the same direct install link with `state=topgg` and `state=dbl`.
- A state created before a deploy can record a garbage `source` for up to 10 minutes, because the OAuth state format changed. The redirect still works.

## Known limits and open questions

- Adopting an existing voice channel as a generator is not possible today (`createGenerator` always creates a new channel). A small endpoint taking a channel id plus a dashboard button would do it. Not built.
- No module has test sources, so only compilation, ktlint and detekt were run. The OAuth flow and the Discord messages were checked by hand only partly.
