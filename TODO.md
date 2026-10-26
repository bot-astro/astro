# TODO

## Manual Discord test plan (Hyperion bot)

Test setup to prepare once:
- 2 guilds: a **free** one and a **premium** one (test entitlement), plus the developer guild
- 3 accounts: **owner** (Manage Server), **member A**, **member B** (no perms), ideally one bot-less alt on another locale
- A guild where the bot is **missing permissions** (no Manage Channels / Move Members / Manage Roles), and one with `allowMissingAdminPerm` on

Legend: `[F]` free guild, `[P]` premium guild, `[locale]` repeat in a non-English locale

### Cross-cutting scenarios (apply to every interaction below)
- [x] Not in a VC -> `vc.context.not_in_vc` error
- [x] In a VC that is not temporary -> `not_in_temporary_vc` error
- [x] Guild without settings -> `not_configured` error
- [x] Not the VC owner (for owner-only interactions) -> `not_owner` error
- [x] Generator deleted while temp VC exists -> `generator_deleted` error
- [x] Cooldown: spam the same interaction (2s default) -> cooldown message with timestamp
- [x] Bot missing permissions -> `guard.bot_permissions` (and skipped with `allowMissingAdminPerm`)
- [x] Member missing permissions -> `guard.member_permissions`
- [ ] Vote-required interactions: not voted -> vote link, voted -> works, premium guild -> skips the lock
- [ ] Ephemeral vs public replies and defer/edit behaviour look right (Components V2 containers render, no "interaction failed")
- [ ] Command localization (names/descriptions) shows in a non-English client, replies use the guild locale
- [ ] Slash commands registered globally, developer commands only in the dev guild

### Info and setup
- [ ] `/help`: message, links/buttons, Ultimate field missing (known TODO), `[locale]`
- [ ] `/dashboard`: public reply, dashboard link button
- [ ] Setup button (`button/settings/setup`, from welcome message / help)
  - [ ] Owner without Manage Server -> blocked
  - [ ] Bot missing Manage Channels -> blocked
  - [ ] `[F]` first run creates category "Astro" + `interface` channel + `➕ Generator` with default settings
  - [ ] `[F]` second run -> generator limit (2) / interface limit (1) error with Ultimate + dashboard buttons
  - [ ] `[P]` multiple runs succeed
  - [ ] Guild near 500 channels -> `setup.max_channels`
  - [ ] Guild with no settings yet (settings created on the fly)
  - [ ] Interface message is posted in the new channel and its buttons work

### Developer commands (dev guild only)
- [ ] `/dev-ping` works for the developer, denied for others and in other guilds, not registered outside dev guild
- [ ] `/dev-message` preview: every message variant renders (welcome, help, errors...), `[locale]`

### Temporary VC lifecycle (events)
- [ ] Join generator -> temp VC created and user moved, name from template, bitrate/limit/category/position from generator
- [ ] Generator cooldown (3s): rejoin quickly -> no duplicate VC
- [ ] Queue mode: join generator with an available non-full temp VC -> moved there instead of creating
- [ ] Category full (50) -> fallback generator `[P]`, no fallback configured -> configuration error, `[F]` fallback -> premium error
- [ ] Permissions inherited: NONE / GENERATOR / CATEGORY
- [ ] Initial state locked/hidden with target role set, and with a missing target role -> config error
- [ ] Owner permissions (custom vs default), immune (moderator) role permissions, bot overrides
- [ ] Name variables: `{nickname} {username} {mention} {id} {n} {nato} {roman} {vc_*} {activity_*}`, premium variables `[F]` blocked vs `[P]` allowed
- [ ] Auto private chat on creation (`autoChat`) `[F]` blocked / `[P]` created, chat logs on/off
- [ ] Auto waiting room on creation (`autoWaiting`) `[F]` blocked / `[P]` created
- [ ] Creation message in VC chat (`[F]` blocked vs `[P]`), interface message
- [ ] Owner role assigned on creation `[P]` and removed when owner leaves
- [ ] Exceeding max generators/connections after downgrade -> premium configuration error
- [ ] Second user joins temp VC -> gets full VC + private chat permissions, join log message
- [ ] Non-owner leaves -> overrides removed from VC / chat / waiting, leave log message
- [ ] Owner leaves with others present -> ownership moves to first non-bot member, permissions/name/owner role updated
- [ ] Last member leaves -> VC, chat and waiting room deleted, entity removed
- [ ] Only bots left in the VC -> deleted
- [ ] Moving directly between two temp VCs / generator <-> temp VC
- [ ] Bot lacks permission to create/move -> configuration error posted (unknown error path)
- [ ] Configuration errors are delivered to the right place and are readable, `[locale]`
- [ ] Owner activity change (start/stop game, Spotify) -> VC / chat / waiting name updates when template uses `{activity_*}`, not for non-owners or bots, rate limit friendly
- [ ] Connections (role on join): ASSIGN / REMOVE / TOGGLE, join and leave, connected VC, connected category, connected generator's temp VCs, moving inside the same connection (no change), `permanent` connection keeps the role, `[F]` second connection blocked

### Voice commands: settings (slash + panel button, both paths)
- [ ] `/name` + NameButton + `modal/vc/name`: valid, empty, too long, variables, premium variables `[F]`/`[P]`, non-owner
- [ ] `/limit` + LimitButton + `modal/vc/limit`: 0, 1-99, negative, >99, non-number
- [ ] `/bitrate` + BitrateButton + `modal/vc/bitrate`: below min, above guild boost max, non-number
- [ ] `/status` + StatusButton + `modal/vc/status`: set, clear, too long, bot lacking permission
- [ ] `/region` + `button/vc/region` + `menu/vc/region`: pick region, automatic
- [ ] Name/limit/bitrate changes also update `{vc_*}` variables in chat/waiting names
- [ ] Rate limit on rapid channel renames (Discord 2/10min) handled gracefully

### Voice commands: permissions
- [ ] `/lock`, `/unlock`, `/hide`, `/unhide` (+ panel buttons): state toggles on the right target role, already-locked/hidden message, effect verified with member A
- [ ] `/permit` + PermitButton + `menu/vc/permit` + user context menu: users, roles, multiple, self, bot, already permitted
- [ ] `/ban` + BanButton + `menu/vc/ban` + user context menu: banned member currently in the VC gets disconnected, owner, self, bot, immune role/moderator, already banned
- [ ] `/invite` + InviteButton + `menu/vc/invite` + user context menu: DM/notification works, invite link lets locked VC be joined, DMs closed
- [ ] Permit/ban/invite on hidden or locked VC, and when waiting room exists

### Voice commands: ownership
- [ ] `/transfer` + TransferButton + `menu/vc/transfer`: to a VC member, to non-member, self, bot; owner role moves `[P]`; name/permissions update
- [ ] `/claim` + ClaimButton: owner still in VC -> denied, owner left but others present -> works, claim when already owner
- [ ] Previous owner's overrides cleaned up after transfer/claim

### Voice commands: chat, waiting room, template, logs
- [ ] `/chat` + ChatButton: create, delete, already exists, `[F]`/`[P]` limits, users in VC can see/write, outsiders cannot
- [ ] `LogsButton`: toggle chat logs on/off, then join/leave messages follow
- [ ] `/waiting` + WaitingRoomButton: create/delete, already exists, `[F]`/`[P]`, users moved/denied correctly
- [ ] `/template` + TemplateButton + `menu/vc/template`: apply a template (name, limit, bitrate, perms), no templates, template limit `[F]` (3), invalid template, the action-row-inside-container question above

### Interface (panel in the interface channel)
- [ ] Panel message renders with all buttons (default order), buttons trigger the same handlers as slash commands
- [ ] Custom message/button order `[P]` via dashboard, `[F]` blocked
- [ ] Interface channel deleted / message deleted -> dashboard shows error, bot doesn't crash
- [ ] Recreate / update interface from the API (`InterfaceController`)

### Bot / guild lifecycle
- [ ] Bot joins a guild: welcome message in system channel, fallback to first writable channel, no writable channel, guild settings created with right locale
- [ ] Bot kicked: analytics event recorded
- [ ] Entitlement create / update / delete: premium status toggles in the guild, premium role added/removed in the support server (and user not in support server case)
- [ ] Premium expiry (`entitlements-expiration-job`) downgrades a guild correctly

### API (dashboard integration)
- [ ] `DiscordGuildController` and `InterfaceController` endpoints authorize correctly (see `docs/authentication.md`) and return expected data

### Infra / observability while testing
- [ ] Shard readiness / liveness indicators, shard gauges, pod gauge
- [ ] Redis global rate limit + shard session controller across 2 pods
- [ ] Errors reach Sentry with context, metrics visible in Grafana, BigQuery analytics rows appear
- [ ] Restart the bot mid-session: temp VCs keep working, cooldowns survive
