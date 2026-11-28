---
name: diagnose-astro
description: Find out why the Astro Discord bot does not behave as expected in a server - temporary voice channels not created, roles not given, interface buttons missing, missing permissions. Use when the user reports Astro not working, or asks why something happened.
---

# Diagnose Astro

1. Read `guilds_errors_list` for the server. It returns the configuration errors Astro hit in the last 7 days (for example missing permissions when creating a temporary voice channel). Each has a `description` and a `guide` that explains how to fix it, use them. Group the repeated ones.
2. Read `guilds_settings_get` and compare with what the user expects: is there a generator at all, does the channel it points to still exist (`discord_guilds_listChannels`), does the role of a voice role still exist (`discord_guilds_listRoles`).
3. A missing permission in the error log is the most common cause of channels not being created. Permissions Astro lacks have to be given on Discord. `discord_constants_listChannelPermissions` only lists the ids a generator accepts in `owner_permission_ids`, the permissions the owner of a temporary channel gets.
4. Tell the user the cause in one or two sentences, and the fix. If the fix is a setting, propose the change and follow the `configure-astro` skill. If the fix is on Discord (a permission, a deleted role), say exactly which one, you cannot change it from here.

Do not change settings to "see if it helps". Diagnose first, change after the user agrees.
