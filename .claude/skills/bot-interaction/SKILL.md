---
name: bot-interaction
description: How to add or change Discord interactions (slash commands, buttons, selects, modals, autocomplete) and voice event handling in `services/bot`. Use when working on the bot module.
---

# services/bot interactions

Base package `space.astro.bot`. Spring beans, no reflection. Never touch `bot-old` except as behaviour reference (see `services/bot/MIGRATION.md`). Don't compile/build, the user does.

## Handler = one `@Component`
Interfaces in `interactions/handlers/InteractionHandlers.kt`: `SlashHandler` (`data`), `ButtonHandler`/`StringSelectHandler`/`EntitySelectHandler`/`ModalHandler` (`id`), all with `settings` + `contextResolver` and `suspend fun handle(event, ctx)`. `AutocompleteHandler` (`command` = full path with dots, `handle(event)`) skips the pipeline: no guards/ack, call `event.replyChoices(...)` within 3s, max 25 choices.
- One class may implement slash + button (+ autocomplete) and share a private function (see `commands/vc/permissions/UnlockInteraction.kt`, `commands/vc/template/TemplateCommand.kt`).
- Component ids: `const val BUTTON_ID = "button/vc/<name>"`; `?` starts the payload (`id?key=value`), matching uses the part before it. Max 100 chars. Duplicate keys fail at startup.
- Files go in `commands/<area>/...`, `commands/info` for non-VC.

## Settings and context
- `InteractionSettings(ack, cooldown = 2s, memberPermissions, botPermissions)`. `Ack`: `DEFER_REPLY_EPHEMERAL` (default), `DEFER_REPLY` (public), `DEFER_EDIT` (components only, edits their message), `NONE` (handler replies, e.g. opens a modal; a modal can't open from a modal).
- Pipeline (`InteractionPipeline`): guild check → guards (cooldown, permissions) → context resolve → ack → handler → error mapping. Don't re-implement any of this in handlers.
- Context: `contextResolverFactory.create(ownershipRequired, origin)` gives a `VcInteractionContext` (`ctx.vcOperationCTX`), plain `InteractionContext` otherwise (`guild`, `member`, `guildSettings`, `responder`).
- Reply only via `ctx.responder` (`reply`, `followUp`, `error`). After the ack everything goes through the hook.

## Errors
- Throw `InteractionException("user message")` for expected user errors, `ConfigurationException(ConfigurationErrorEntity(...))` for server misconfiguration. The pipeline replies and publishes `ConfigurationErrorEvent`; `InsufficientPermissionException` is mapped too. Don't catch them in handlers.

## VC features
- State lives in `TemporaryVCEntity` (Redis, `TemporaryVCRepository`) and `GuildSettingsEntity` (Mongo; look up by `findByGuildID`, not `findById`). Repositories are blocking: use `withContext(Dispatchers.IO)` from handlers.
- Change channels through `components/managers/vc/*` with a `VCOperationCTX`: managers mutate, then `ctx.queueUpdatedManagers()`, and **the caller saves `temporaryVCEntity`**.
- Voice events: `GuildVoiceUpdateEventListener` → `VCEventDetector` → `VCEventHandler` → `Joined*/Left*/ConnectedVCHandler`. Roles go through `SimpleMemberRolesManager` (queued once at the end).

## Rules
- Components V2 is on globally: embeds + content in the same message throw. Existing embed code carries `// TODO: components v2`; keep that marker, or `useComponentsV2(false)` for plain embeds. V2 helpers/i18n come last.
- Premium is not implemented: leave `// TODO: premium`, don't port limit checks.
- Analytics: publish an `AnalyticsEventData` subclass (`models/analytics`) with `ApplicationEventPublisher`; add its BigQuery table in `BigQueryService.tableName`.
- Async: only jda-ktx `await()`; fire-and-forget with `.queue()`.
- Code style: trailing commas, wrapped parameters, `@Component` constructor injection.
