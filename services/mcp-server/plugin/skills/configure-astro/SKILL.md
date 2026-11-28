---
name: configure-astro
description: Change how the Astro Discord bot is set up in a server through the Astro MCP tools - generators (temporary voice channels), interfaces (control buttons), voice roles, templates, the bot profile and language. Use when the user wants to create, change, move or delete any of these.
---

# Configure Astro

Astro creates temporary voice channels and gives roles for voice channels. These are the things you can configure in a Discord server:

- **Generator**: a voice channel members join to get their own temporary channel. It holds the defaults of the channels it creates (name, limits, category, permissions, extras).
- **Interface**: a message with buttons so the owner of a temporary channel controls it without commands.
- **Voice role** (connection): a role Astro gives, removes or toggles when someone joins a voice channel.
- **Template**: saved channel settings members can re-apply.
- **Settings and profile**: bot language, and the nickname, bio, avatar and banner Astro shows in the server.

## Workflow

1. Find the server with `discord_guilds_list` (only servers the user manages and Astro is in). Never guess a guild id.
2. Read `guilds_settings_get` first. It returns every generator, interface, voice role and template with their ids, the ids the other tools need. Use `discord_guilds_listChannels` and `discord_guilds_listRoles` to find channel and role ids by name, ask the user when a name matches more than one.
3. Make the smallest change that does what was asked, with the `update` tools. They are partial updates: send only the fields that change. Nested objects are merged, lists are replaced as a whole (so send the whole list), `null` resets a field to its default. Do not send back the whole object you read.
4. For a change that is large, hard to undo or that you are unsure about, call the update tool with `query.dryRun: true` first. Nothing is saved, the result shows what the settings would be. Show the user the difference, then repeat the call without `dryRun` once they agree.
5. After a write, check the returned settings contain what the user asked for. Every write returns the whole guild settings, so read only the part you changed.

## Rules

- Creating or deleting touches Discord: a generator creates and deletes a real voice channel, an interface posts or deletes a real message. Say so and get a clear yes before deleting. Temporary channels a deleted generator already created stay.
- Free servers have limits on generators, interfaces, voice roles and templates, and some settings are premium only. A `403 PREMIUM_REQUIRED` is not a bug and retrying does not help: tell the user what needs premium and offer the alternative that does not.
- `429` means wait the `Retry-After` seconds. A `404` on a guild or channel usually means Astro is not in the server or the id is wrong.
- A read only connection does not list the tools that change things. Tell the user to connect again and allow changes.
- Never put secrets, invite links of other servers or personal data in names, bios or templates.
