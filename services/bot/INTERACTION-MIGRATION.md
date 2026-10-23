# Interaction framework migration (item 1 of MIGRATION.md)

Design and JDA 6.5.0 findings: see "Item 1 design" in `MIGRATION.md`. Summary: Spring beans, one pipeline, no reflection, ack decided by settings, stateless follow-ups (select menus handled by their own handler), jda-ktx only for `await`.

## Steps

### 1. Core base code (no feature handlers)
- `interactions/InteractionSettings` + `Ack` (replaces `InteractionAction`; premium dropped, `voteRequired` kept with TODO in the guard)
- Handler interfaces: `InteractionHandler<E, C>`, `SlashHandler`, `ButtonHandler`, `StringSelectHandler`, `ModalHandler` (+ autocomplete later)
- `InteractionPipeline`: guild checks → guards → resolve context → ack → `handle` → error mapping (guards and resolution run before the ack so their errors are plain ephemeral replies; keep them fast, 3s limit)
- Four thin `@EventListener`s (slash, button, select menu, modal) → handler lookup by name / id (prefix before `?`), duplicate ids fail at startup
- `InteractionGuard` beans (`@Order`): cooldown, vote (TODO), member permissions, bot permissions. Guild-only is in the pipeline. Whitelist guard dropped (no property in the new bot).
- `InteractionContext` (+ `VcInteractionContext`) and `ContextResolver` (+ `VcContextResolver`); other contexts (settings, generator, ...) come with their feature
- `InteractionResponder` (stateless wrapper over `event.hook`: `reply`, `edit`, `error`)
- Slash command registration (collect `SlashHandler.data`, upsert globally / per whitelisted guild)
- Minimal UI helpers (V1 embeds for now) + user-facing exception type thrown by resolvers
- Cooldown storage (port of `CooldownsManager`), VC models needed by `VcContextResolver` (`VCOperationCTX`)
- Error mapping reuses `ConfigurationErrorEventPublisher` / `ConfigurationErrorEvent`

### 2. One vertical slice, components V1
One command, one button, one select menu, one modal, using the base from step 1 and still embeds + `ActionRow` (V2 default switched off meanwhile, see questions).
Proposed (lowest manager dependencies, covers all four handler types):
- Command: `name` (opens modal), or `lock` if we want a pure command
- Button: `region` (replies with a select menu)
- Menu: `RegionMenu`
- Modal: `NameModal`
Port only the managers they need (`VCNameManager`, region logic).

### 3. Feedback
Review the slice, adjust the base (interfaces, settings, responder) before porting more.

### 4. Port the rest
Remaining interactions per feature, as listed in `MIGRATION.md` items 6-10. Slash + button for the same action share one class.

### 5. Components V2
Turn `setDefaultUseComponentsV2(true)` back on; replace embed helpers with `Container` / `TextDisplay` helpers; fix `InterfaceManager` (builds embeds, would throw under V2); decide behaviour for old V1 interface messages.

## Decisions
1. Global V2 default is disabled in `Application.kt` until step 5 (embeds throw under V2).
2. `InteractionSettings` keeps `voteRequired` (guard only has a `// TODO` where the vote check goes) and the bot-permission guard keeps the `allowMissingAdminPerm` bypass.
3. Legacy `cmd>` button ids are dropped.
4. Slash + button for the same action share one class.
5. Handler interfaces do not share a generic event parameter (`InteractionHandler<C>` only; `SlashHandler`/`ButtonHandler`/... declare their own `handle(event, ctx)`), because Kotlin forbids inheriting the same interface with two type arguments, which a combined slash + button class needs.
6. Interface-message buttons (`interface=true`) are not special-cased yet: their handlers pick `Ack.DEFER_REPLY_EPHEMERAL`, others `Ack.DEFER_EDIT`.
