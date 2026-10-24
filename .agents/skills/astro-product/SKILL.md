---
name: astro-product
description: What the Astro Discord bot does for users (temporary voice channels, voice roles, interfaces, templates), how it is monetised and what usage analytics exist. Use when brainstorming features, premium or pricing decisions, analytics questions, or when you need product context before touching feature code.
---

# Astro: the product

Astro is a Discord bot for **temporary voice channels** and **voice roles**. Server owners configure it mostly through the web dashboard; members use it through voice channels, buttons and slash commands.

## Core concepts (all per Discord server)

- **Generator**: a voice channel that users join to get their own temporary channel. It holds the defaults for what it creates (naming, limits, category, permissions, initial state, and optional extras like a private text chat, a waiting room or a fallback generator).
- **Temporary voice channel**: created by a generator, owned by a user, deleted when empty. Its live state is short-lived.
- **Interface**: a message with buttons so owners can control their channel without commands.
- **Template**: saved channel settings a user can re-apply, optionally limited to some generators.
- **Connection** (voice roles): links a voice channel to a role. Joining grants, removes or toggles the role.

## What channel owners can do

Rename, set limits, bitrate, region and status; lock, hide, permit or ban people; invite; claim or transfer ownership; use the private chat and waiting room; apply templates. Available through slash commands, buttons and menus.

## Monetisation

- Premium is bought per **server**, monthly or yearly, either through Discord's own premium subscriptions or by a user upgrading a server through the web billing provider.
- Premium works by **feature gating plus caps on how many generators, interfaces, connections and templates** a server can have. It does not meter voice channel usage.
- Premium holders get a role in the support server.
- Pricing analysis and decisions live in `docs/pricing.md`; don't restate numbers here.

## Analytics

Usage events go to BigQuery: voice channels generated, command invocations, connection triggers, and servers joined or left. Not tracked there: how many generators or templates a server has (that is in the database), channel lifetime, concurrency, and premium status over time. Read the analytics component in the bot service for the current schema.

## Where else the product shows up

- The dashboard and marketing site are in the separate frontend repo.
- Translations of bot text are managed in Crowdin.
