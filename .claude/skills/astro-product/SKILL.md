---
name: astro-product
description: What Astro (the Discord bot) does, its features, vocabulary and how users configure it. Use for product decisions, user-facing copy, feature questions or when unsure what a generator/connection/interface/template is.
---

# Astro product

Discord bot for **temporary voice channels** and **voice roles**. Backend repo only; the dashboard frontend is `astro-bot.space`. Users configure everything on the **dashboard** (REST API in `api-central`); slash commands are only for what members do while in a VC.

## Vocabulary
- **Generator**: a voice channel; joining it creates a **temporary VC** and moves the member in (the member becomes its **owner**). Settings: name templates (also locked/hidden), user limit, bitrate, category, inherited permissions, owner permissions/role, immune (moderator) role, initial state, position, rename conditions, queue mode, fallback generator (when the category is full), auto private chat, auto waiting room. Empty temporary VCs are deleted.
- **Temporary VC**: state `UNLOCKED | LOCKED | HIDDEN | UNHIDDEN`, owner, optional **private chat** (text channel) and **waiting room** (voice channel), chat logs toggle.
- **Connection** (voice role): joining a voice channel or category gives/removes/toggles a role (`ASSIGN | REMOVE | TOGGLE`); `permanent` keeps the role after leaving.
- **Template**: saved VC settings (name, limit, bitrate, region) an owner can apply with `/template`; can be limited to some generators.
- **Interface**: a message with buttons in a text channel (or sent on VC creation) that lets owners control their VC without commands.
- **Name variables**: `{nickname} {username} {mention} {id}`, `{activity_*}`, `{vc_*}`, incremental `{n} {nato} {roman}`; names are cut to 100 chars.

## Member commands
Settings: name, limit, bitrate, region, template. Permissions: lock/unlock, hide/unhide, ban, permit, invite, kick. Ownership: claim, transfer. Chat (+ logs), waiting room. Info: help, dashboard. Most require being the owner of the temporary VC the member is in.

## Decisions already made
- Setup commands (generator/interface/connection/settings creation) are **not** in the bot: the dashboard covers them; the welcome message points there.
- Premium (entitlements, limits) is **not implemented** in the rewrite; it will come back with a different design. Don't add limit checks or premium upsell.
- Misconfiguration is reported to the server admins on the dashboard error page (`ConfigurationErrorEntity`), not by DM.
- Tone of user-facing text: short, friendly, emoji from `ui/Emojis`, errors say what to fix.
- Legacy bot behaviour is the reference for product behaviour unless `services/bot/MIGRATION.md` says otherwise.
