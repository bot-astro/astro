# bot-old → bot migration plan

Legend: ✅ already in `bot` · ⬜ todo · ❓ needs discussion

## Already done
Application, sharding (session controller, shard manager), health indicators, ready/config-error listeners, `InterfaceManager` (create/update/delete only), `PermissionExtentions`, REST controllers (guild, interface).

## Items (suggested order)

1. ✅ **Interaction framework** (decided, see [Item 1 design](#item-1-design))
2. ⬜ **Core UI** (`core/ui`: Buttons, Embeds, Emojis, Messages) + `core/exceptions` (ConfigurationException, VcOperationException)
   **Decided: Components V2 (helpers replacing `Embeds`, interface message) is done as the very last step**; until then new code keeps using embeds with a `TODO: components v2`.
   Move button ids/emojis here (see TODO in `InterfaceManager`); exceptions likely map to `shared/core` exceptions + `ConfigurationErrorEvent`.
3. ✅ **Listeners: guild/entitlements/member** (`GuildJoin/Leave`, `Entitlement*`, `MemberUpdateActivities`, `ContextRefreshedListeners`, `ConfigurationErrorEventListener`✅)
   Done: `GuildJoin/Leave` (welcome message has TODOs: channel selection, premium buttons, Setup button, V2). Skipped: entitlements + `MemberUpdateActivities` (premium only), `ContextRefreshedListeners` (= `ApplicationReadyListener`).
4. ✅ **Services/infra** (`CooldownsManager`, `AnalyticsService` + `BigQueryService`, `ConfigurationErrorService`, `AuthWebFilter`, `ApplicationFeaturesConfig`, `DashboardController`, `LifecycleController`)
   Likely drop: `AuthWebFilter`/`LifecycleController` (replaced by `api/` + health), `ConfigurationErrorService` (replaced by event publisher). **Analytics kept**: `AnalyticsService` + `BigQueryService` ported (`@Scheduled` flush, no receivers, sealed `AnalyticsEventData`; `GUILD_SNAPSHOT` dropped, class was never defined). Publishers wired: slash command (`InteractionEventListener`), temporary VC generation + connection invocation (`VCEventHandler`). Cooldowns: `InteractionCooldownService` (Redis), also used for the generator cooldown.
5. ✅ **VC core models** (`models/discord`: `PermissionSets`, `SimpleMemberRolesManager`, `vc/VCOperationCTX`, `vc/event/*`)
6. ✅ **VC managers** (`managers/vc/*`: Name, Ownership, Permission, Position, PrivateChat, Template, WaitingRoom) — split by feature, one at a time. Drop premium checks.
7. ✅ **Voice event pipeline** (`GuildVoiceUpdateEventListener`, `VCEventDetector`, `voice/handlers/*`)
   Handlers are `@Component`s dispatched by `VCEventHandler`; premium checks dropped (TODOs). Analytics only on actual VC creation (legacy also fired on queue/cooldown returns). Ported `SimpleMemberRolesManager`, `VCEvent`/`VCEventData`, creation helpers of `VariablesManager`; added `GuildSettingsRepository.findByGuildID`.
8. ✅ **VC interactions, by feature** — each = command + button + menu/modal where present:
   - settings: name, limit, bitrate, region, template
   - permissions: lock/unlock, hide/unhide, ban, permit, invite, kick
   - ownership: claim, transfer
   - chat/logs, waiting room
   Decide: keep commands and buttons as separate classes or share one handler per action.
9. 🚫 **Setup / pre-dashboard commands** (decided: not ported, the dashboard covers them) (`generator`, `generator-waiting`, `interface`, `connection`, `settings`, `template-settings`, `setup` cmd+button, `premium`)
10. ✅ **Info commands** (`help` + 7 help buttons, `dashboard`)
    Help buttons could collapse into one handler; `HelpPremiumButton` skipped.
11. ⬜ **Tests** (`VariablesManagerTest`) — port alongside whichever manager owns variables.

## Open questions
- Autocomplete handler / `CommandChoice` / `CommandCategory`: `AutocompleteHandler` added (no pipeline, registered in `InteractionRegistry`), used by `/template`. `CommandChoice`/`CommandCategory` dropped.
- i18n: use `shared/core` i18n instead of `Messages.kt`: **decided, last step after Components V2.**

## Item 1 design

### JDA 6.5.0 facts that shape it (checked in sources)
- Components live in `net.dv8tion.jda.api.components.*`; top-level types are `MessageTopLevelComponent` / `ModalTopLevelComponent` (no more `LayoutComponent` / `ItemComponent`).
- Components V2 is on globally (`setDefaultUseComponentsV2(true)` in `Application.kt`). With V2 the builders **throw** if the message has `content`, `embeds` or a `poll`, and also if there are no components. So every reply is built from `Container` / `TextDisplay` / `Section` / `ActionRow`, not `Embed`.
  - Old `Embeds.default/error` → V2 helpers returning a `Container` (accent color + `TextDisplay`). Part of item 2.
  - **Existing bug:** `InterfaceManager` builds the interface with `setEmbeds` + `ActionRow`; with V2 default on, `MessageCreateBuilder.build()` throws. Needs a decision (interface message as V2 container, or explicitly `useComponentsV2(false)` for it). Also unverified: whether Discord allows editing an old V1 interface message into V2, so interface buttons should reply (new message), never edit the interface message.
- Modals: `net.dv8tion.jda.api.modals.Modal`; inputs/selects must be wrapped in `Label.of(label, desc, child)`. `ModalInteraction` is not an `IModalCallback` (cannot open a modal from a modal) but can `deferReply` / `deferEdit`.
- Ack: an interaction can only be acknowledged once; `isAcknowledged()` exists. After the first ack everything goes through `event.hook` (`editOriginal*`, `sendMessage*`).
- `Interaction` exposes `isFromGuild`, `guild`, `member`, `userLocale`, `entitlements` (useful when premium returns).
- Component events implement reply, edit and modal callbacks; `ModalInteractionEvent` implements reply + edit.

### jda-ktx: only use `await`
Enough on its own: `RestAction.await()` and `jda.await<Event>`. The command DSL (`Command {}`, `option<T>()`, `subcommand`) is thin sugar over `Commands.slash(...).addOption(...)`; plain JDA is fine and avoids a dependency on its API. Optional: its V2 builders (`Container {}`, `trees`) reduce verbosity of nested components, can be adopted later without touching the design.

### Approach: Spring beans, one pipeline (no reflection)
- One interface per event kind, with the context as a generic parameter:
  ```kotlin
  interface InteractionHandler<E : GenericInteractionCreateEvent, C : InteractionContext> {
      val settings: InteractionSettings
      val contextResolver: ContextResolver<E, C>
      suspend fun handle(event: E, ctx: C)
  }
  interface ButtonHandler<C : InteractionContext> : InteractionHandler<ButtonInteractionEvent, C> { val id: String }
  // same for SlashHandler (data: SlashCommandData), SelectMenuHandler, ModalHandler
  ```
- Handlers are `@Component`s. Four thin `@EventListener`s look the handler up by key (component id prefix / command path) and call a single `InteractionPipeline.run(event, handler)`. Duplicate ids fail at startup.
- `InteractionSettings(ack, cooldown, memberPermissions, botPermissions)`. Replaces `InteractionAction` (premium and vote removed for now, TODO). 
- Pipeline order: ack → guards → resolve context → `handle` → error mapping. Guards are ordered beans (`InteractionGuard`): guild-only, whitelist, cooldown, member permissions, bot permissions. Error mapping (`ConfigurationException`, `InsufficientPermissionException`, unknown) lives once in the pipeline and publishes `ConfigurationErrorEvent`.
- `ContextResolver<E, C>` replaces the big `when` in `InteractionContextBuilder`: `BaseContextResolver`, `VcContextResolver(ownershipRequired, origin)`, `SettingsContextResolver`, ... A failure throws a user-facing exception that the pipeline replies with.
- Ack is decided up front by the settings: `DEFER_REPLY(ephemeral)`, `DEFER_EDIT`, `NONE` (handler opens a modal). Acking first also keeps DB/Redis lookups in guards under Discord's 3s limit. After that `InteractionResponder` (replaces `InteractionReplyHandler`) is a thin, stateless-ish wrapper over the hook: `reply(components)`, `error(text)`, `edit(components)`. No `trackReply`, no swappable callbacks. Interface buttons (`interface=true` in the id) use `DEFER_REPLY`.
- Slash + button for the same action: one class may implement both `SlashHandler` and `ButtonHandler` and share `execute(ctx)`, removing the duplicated command/button pairs.

### `replyWithSelectMenu` and friends
The old helpers are stateful `await`-based continuations (15 usages of select menu, mostly pre-dashboard): they hold a coroutine open, need timeouts, re-point callbacks (`setCallbacks*`, 19 usages) and die on restart. Replacing them with a modal was only one option and changes UX, so it is **not** the default.
- Default: keep the UX (a message with a select menu / buttons), but handle the selection in a separate `SelectMenuHandler` / `ButtonHandler` bean (the old code already has menus like `TransferMenu`, `BanMenu`). Stateless: VC flows re-derive state from the member's voice state; settings flows put the entity id in the component id (100 char limit).
- Modal-with-select (`Label` + `StringSelectMenu`) stays available for flows where a modal UX is acceptable; decide per flow.
- Pure confirm dialogs (`replyWithConfirmation`) can stay a small `await`-based helper if there are only 2 usages.

### Open questions (item 1)
1. Interface message: V2 container or opt out of V2 for it? (see bug above)
2. Slash + button in one class: yes?
3. Vote requirement and `allowMissingAdminPerm`: keep in settings?
4. Drop legacy `cmd>` button ids?
