---
name: astro-overview
description: Outline of what the Astro Discord bot does (temporary voice channels, voice roles, interfaces, templates), its current premium/paywall setup and its BigQuery analytics. Use when brainstorming product, pricing, premium or analytics decisions, or when you need product context before touching feature code.
---

# Astro: product outline

Discord bot for **temporary voice channels** and **voice roles**. Backend only (frontend dashboard lives in `astro-bot.space`).

## Core concepts (per guild, stored in `GuildSettingsEntity`)

- **Generator**: a voice channel users join to get their own temporary VC. Holds all defaults for the generated channel: name, locked/hidden names, status, user limit, bitrate, category, inherited permissions, owner role/permissions, initial state (locked/hidden/...), initial position, rename conditions, min/max limits for user commands, and a fallback generator.
  - Optional extras per generator: **auto chat** (private text channel per VC), **auto waiting room** (queue VC), **queue mode**.
- **Temporary VC**: the channel produced by a generator, owned by a user, kept in Redis, deleted when empty.
- **Interface**: a message with buttons (embed + button layout) users click to control their VC without commands.
- **Template**: saved VC settings (name, limit, bitrate, region) a user applies to their VC; can be restricted to some generators.
- **Connection**: voice channel -> role link. Joining assigns, removes or toggles a role (optionally permanent). This is the "voice roles" half of the bot.

## Owner commands on a temporary VC (`bot/commands/vc/*`)

- Settings: name, limit, bitrate, region, status
- Permissions: lock/unlock, hide/unhide, permit, ban, invite (slash, buttons, menus, user context menus)
- Ownership: claim, transfer
- Chat (private text channel) and logs, waiting room
- Templates

Info commands: help, dashboard. Configuration is done mostly through the web dashboard (api-central), not in Discord.

## Monetisation today

- Paid per **guild**, monthly or yearly, via Discord entitlements (server SKU) or a user "upgrading" a guild (Chargebee subscription). `entitlements-expiration-job` expires entitlements; `support-bot` applies the premium role in the support server.
- Legacy model is **feature gating plus count caps** (see `PremiumRequirementDetector` in `shared/old-core`):
  - Free caps: 2 generators, 1 interface, 1 connection, 3 templates
  - Premium-only: fallback generator, auto private chat, auto waiting room, bot message in VC chat, owner role assignment, editing interface message / button order, bad-word validation, premium name variables (`{n}`, `{nato}`, `{roman}`, `{activity_*}`)
- **In the rewrite premium is not implemented yet** (see `CLAUDE.md`): don't port the limit checks.

## Analytics (BigQuery, `bot/components/analytics`)

Batched inserts, tables partitioned by `DAY` on `timestamp`, partition filter required:

| Table | Columns |
|---|---|
| `TEMPORARY_VC_GENERATION` | guild_id, user_id, generator_id, timestamp |
| `SLASH_COMMAND_INVOCATION` | name, guild_id, channel_id, user_id, main_option_name/value, raw_options, timestamp |
| `CONNECTION_INVOCATION` | guild_id, user_id, connection_id, timestamp |
| `GUILD_EVENT` | guild_id, users_count, action (join/leave), timestamp |

Not tracked in BigQuery: how many generators/interfaces/templates/connections a guild has (that is in MongoDB), VC lifetime/duration, concurrent VCs, premium status over time.
