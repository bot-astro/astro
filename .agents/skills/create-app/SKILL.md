---
name: create-app
description: Build a Crowdin app end to end and leave the user with a published app they can click - scaffold it with @crowdin/serverless-apps-cli, write the UI with @crowdin/serverless-apps-sdk, publish it into their Crowdin or Crowdin Enterprise organization, and open it for them. Reach for this whenever someone wants something of their own inside the Crowdin UI - a panel in the translation editor, a tool or report in a project, a page in the organization menu, a dashboard over their projects - and also when they describe a localization chore they are tired of doing by hand. Covers phrasings like "create a Crowdin app", "add a tool to my Crowdin project", a bare "/create-app", and cases where the word "app" is never said. Also use it when a serverless Crowdin app already exists in the working directory and needs another module, a manifest change, or a republish. Not for the separate Crowdin CLI that syncs sources and translations through crowdin.yml.
---

# Create a Crowdin app

## Who is asking

A translator or a localization manager. They arrive with a chore they are sick of, not a specification: "show me how we translated this term before", "I want to see what the proofreader changed after me", "every Thursday I build this spreadsheet by hand".

They do not know what a module, a manifest, a scope, an editor mode or a project identifier is, and they do not know what this platform can and cannot do. They will not pre-check whether their idea is buildable, and they should not have to. **Every question you put to them must be answerable by someone who has never seen a line of code.** A question about module types, scopes, manifests, templates, editor modes, project ids or editions is a defect in this workflow, not a gap in the user.

When their request runs past what the platform allows, that is your problem to solve, not theirs to have avoided. See [when the platform says no](#when-the-platform-says-no).

## What you are actually building

A frontend-only React bundle that Crowdin loads into an iframe inside its own UI. There is no backend and no token in the app: the host executes Crowdin REST calls and keeps the app's persisted records ([Crowdin Storage](#when-the-app-needs-to-remember-something)) for you, under the session of whoever opened the app, limited to the scopes the manifest declares.

## Where the facts come from

The platform gains module types, manifest fields, SDK methods and scopes over time. This skill cannot keep up with that, and neither can your training data - so for **what exists**, fetch the docs. They ship a machine-readable variant of every page: append `.md` to any documentation URL, or take the whole set at once.

| Fetch this | For |
|---|---|
| [building-app/modules.md](https://crowdin.github.io/serverless-apps/building-app/modules.md) | The current module types and their `prepare*` functions |
| [reference/manifest.md](https://crowdin.github.io/serverless-apps/reference/manifest.md) | Manifest fields, module shapes, permissions |
| [reference/cli-commands.md](https://crowdin.github.io/serverless-apps/reference/cli-commands.md) | Commands and flags as released |
| [building-app/crowdin-api.md](https://crowdin.github.io/serverless-apps/building-app/crowdin-api.md) | The tokenless API client |
| [building-app/storage.md](https://crowdin.github.io/serverless-apps/building-app/storage.md) | Crowdin Storage - persisted shared, per-user and module-scoped records |
| [building-app/context.md](https://crowdin.github.io/serverless-apps/building-app/context.md) | Context, theme, events |
| [building-app/host-actions.md](https://crowdin.github.io/serverless-apps/building-app/host-actions.md) | Editor and host actions |
| [building-app/user-interface.md](https://crowdin.github.io/serverless-apps/building-app/user-interface.md) | The UI kit |
| [building-app/i18n.md](https://crowdin.github.io/serverless-apps/building-app/i18n.md) | Localization |
| [development/troubleshooting.md](https://crowdin.github.io/serverless-apps/development/troubleshooting.md) | Known failures |
| [understanding-scopes](https://support.crowdin.com/developer/understanding-scopes/) | What each scope grants |
| [llms-full.txt](https://crowdin.github.io/serverless-apps/llms-full.txt) | Everything, one fetch |

The division of labour: **the docs are authoritative on what exists, this skill on what order to do it in and what breaks.** When a list here disagrees with the docs, the docs win and the list here is stale - reach for a capability the docs describe even if it is absent from these pages. When something works but breaks in a way the docs do not mention, that is what this skill is for.

## Where you must land

The finish line is not "code written" and not even "app published" - it is **the user looking at their working app**. Treat the run as unfinished until you have handed over a link you have reason to believe renders, or, for the placements that have no link, until they confirm they can see it.

## The one question worth asking first

Ask for a link:

> Could you send me a link to your Crowdin page - the project you work in, or just your home page?

Anyone can copy a URL out of their address bar, and it answers the two things you otherwise cannot derive:

- **Which edition.** Host `crowdin.com` means Crowdin; anything else (`<org>.crowdin.com`) means Crowdin Enterprise. This decides whether the placement they want even exists - six of the eleven openable module types live in exactly one edition.
- **Which project.** `crowdin.com/project/<identifier>` or `<org>.crowdin.com/u/projects/<id>` gives you the value `preview --project` needs, without ever asking for an "identifier".

If they send nothing, the CLI's own state file holds the edition after login: `~/.crowdin/serverless-apps-cli.json`, where a non-empty `domain` means Enterprise and its absence means crowdin.com.

## Settle four things before scaffolding

| Slot | Why it has to be settled early |
|---|---|
| **Name** | `create <name>` names the directory, registers the app, and labels the app and all its modules in the UI. Pass a short latin slug; if the user wants a human label, set `name` in the manifest afterwards and push it. |
| **Placement** | Where it appears in the Crowdin UI. The most expensive to get wrong, and constrained by edition ([modules.md](references/modules.md)). |
| **Logic** | What result they expect to see, which drives the UI, the API calls and therefore the `scopes`. |
| **Who sees it** | `default_permissions`. Its default is `owner` only, which is wrong for most requests and cannot be widened after the fact - see [visibility](#8-close-the-loop-on-visibility). |

## Ask sparingly, and only in their words

**Derive, do not ask:** who the user is, what data the app touches, the `scopes` (an output of the green gate, not an input to the brief - [scopes.md](references/scopes.md)), the edition and project (from their link), editor `modes` (declare **every mode the docs list** by default, so the panel is there wherever the user happens to be working - narrow it only if they ask, because a module is simply absent from any mode its manifest omits and the user has no way to know why), and a sensible app name. The template is fixed: always `projects-dashboard-cli`.

**State, do not ask:** put everything you derived into a short brief and let them correct it. Write it in the language they wrote to you in, and in plain words - "a panel on the right in the editor, next to the string", never "editor-right-panel, modes translate/review". A list of claims is easier to object to than a list of questions.

**Ask only when:**

1. **Placement genuinely does not follow from the request.** "in the editor next to the string" is settled. A bare "in the project" leaves a real choice between the Tools section and the Reports section - ask that, described as places, with your recommendation.
2. **Two implementations differ in what the user would see.** Not "how should I build it" but "is progress a chart per language, a table per file, or one number?".
3. **The platform forces a trade-off on them.** Covered below.

One question at a time, each with your recommended answer attached, so "yes" is a complete reply. If they do not know, take your recommendation and move on. **Zero questions is a perfectly good outcome.**

Then show the brief and wait for a go-ahead: `create` registers a real app in their organization and there is no `delete` command.

## When the app needs to remember something

Crowdin Storage is the app's own persisted key-value store, hosted by Crowdin per installation - so stored settings, notes that survive a reload, and state a team shares are all buildable, without a backend. It needs `application.storage` in the manifest `scopes`, and calls run like the REST proxy: as the current viewer, no tokens. The surface lives in [building-app/storage.md](https://crowdin.github.io/serverless-apps/building-app/storage.md); [references/sdk.md](references/sdk.md#keeping-data-crowdin-storage) has the verified snippets.

The design decision is not how to store but **who each record belongs to**, and the key prefix is what enforces the answer:

| The data is | Use | Because |
|---|---|---|
| One user's own - their filters, drafts, notes, a credential they entered | `kv.user.*` (the `user:` prefix) | The platform hides it: another user's records simply do not exist for the caller, in reads and listings alike |
| Owned by one of the app's surfaces - team state, integration settings, a credential of that surface's team | a key under `module:{moduleKey}:` | The platform serves it only where the app runs as that module, to viewers allowed there - a managers-only module's records never reach the app's other surfaces |
| Public to everyone the app reaches, or shared between the app's modules | a plain key | Nothing protects it: everyone who can use the app can read it - in a public project that includes anonymous visitors - and every signed-in viewer can **overwrite** it |

Default to `user:` for anything about a person and `module:` for anything one surface's team owns, because both stay access-controlled by the platform; a plain shared key is for values anyone the app is visible to may legitimately see - and, signed in, change - including data the app's modules must share, since `module:` records never cross surfaces. One person is exempt from module narrowing: the app's installer sees every module's records on any surface - only other users' `user:` records stay hidden from them. The order matters because the failure is silent: a plain key behaves perfectly while one person tests the app, and leaks or gets clobbered only when the second user arrives.

Two facts that shape designs: unauthenticated viewers have no user identity, so `kv.user.*` throws for them while shared keys keep working - check `user.id` in the context before offering per-user features; and uninstalling the app deletes every record immediately, with no retention.

## When the platform says no

Three things a frontend-only app genuinely cannot do:

- **Use a secret without revealing it.** There is no backend, so the code that uses a credential runs in the viewer's browser - a key baked into the bundle is readable by any viewer, and a storage record comes back decrypted to everyone its scope admits, `secret: true` or not. Storing one is fine exactly when everyone who can reach it may know it: a credential each user enters for themselves under `kv.user.*`, or a team credential for an integration under the `module:` prefix of a module only that team can use - `secret: true` in both cases. A key that must work for people who should not see it belongs in Crowdin's own integration settings, not in the app.
- **Be called from outside.** Webhooks, schedules, emails, and the platform extension points Crowdin invokes over HTTP (`custom-mt`, `custom-file-format`, `ai-provider`, `external-qa-check` and friends - not even valid module types here). A closed tab is listening to nothing.
- **Outlive the tab.** A few megabytes of file processing in the browser is fine; hundreds are not.

**A no is never the end of the run.** The user came with a real chore and cannot act on "this needs a backend". Deliver the part that works and name the trade-off in one plain sentence:

| They asked for | Give them |
|---|---|
| A weekly email digest | The page itself, which they open when they need it, plus a pointer to Crowdin's own notification settings. Say the email part is not possible in this kind of app |
| A third-party engine or key | Crowdin's own settings for that integration when the key must work for people who should not see it. When everyone using it may also know it, [Crowdin Storage](#when-the-app-needs-to-remember-something) holds it with `secret: true`: `kv.user.*` for a personal key, a restricted module's `module:` prefix for a team key |
| A huge file processed | The same work on a size that fits, with the limit agreed up front |

Only when nothing useful survives, say so and offer to hand it to a developer. Never end a user's run with npm package names as their next step.

## The build sequence

### 0. Preflight

Node `^22.19 || ^24` and pnpm. `npx` implies Node, so pnpm is the usual gap: `corepack enable pnpm`. If Node itself is missing, say it plainly - this needs one install first - and help rather than turning it into a requirement they must satisfy alone.

Probe auth with a read-only call:

```bash
npx @crowdin/serverless-apps-cli@latest list --json
```

JSON means you are logged in. `Not logged in. Run crowdin-serverless-apps login first.` means run [the auth handshake](#the-auth-handshake) first.

Say where the app will be created, in absolute terms, before creating it. Every later change happens in that directory and the user needs to know where their app lives.

### 1. Scaffold

```bash
npx @crowdin/serverless-apps-cli@latest create <name> --template projects-dashboard-cli --yes
cd <name> && pnpm install
```

Both flags matter: without them the CLI waits on an interactive picker that hangs in a non-interactive shell.

**Always `projects-dashboard-cli`.** The template is not a decision to weigh and not worth a question. With it the CLI owns the whole toolchain - Vite, Tailwind, Lingui, Biome, the base tsconfig - so everything downstream works as described here. `projects-dashboard-standalone` hands the build back to the app and every later problem with it becomes yours; pick it only if the user explicitly asks to own the build.

`create` writes `CROWDIN_APP_ID` into `.env` and registers the app. From here use the local binary inside the app directory - `npx crowdin-serverless-apps <command>` - so the toolchain matches the app's pinned version.

### 2. Placement: the part that silently breaks

The scaffold ships two modules, `organization-menu` + `profile-resources-menu`, and **each of them exists in only one edition**. Whichever edition the user is on, one of the pair is dead: it will never appear, and `preview` refuses to build a link for it. So the first edit is always to keep the one that matches their edition and delete the other from **both** `manifest.json` and `src/index.tsx`.

If the app belongs somewhere else entirely, change it in both places together:

- `manifest.json` - the `modules` map, keyed by module type
- `src/index.tsx` - the matching `prepare*` call, synchronous, at the top level

They have to agree with each other and with what the host sends in `context.app.type`. When they do not, the SDK never calls `prepare()`, the host shows a blank iframe, and nothing in the console explains why.

Editor modules need `modes`; asset panels need `fileNamePattern`; context-menu entries need an `options` triple whose legal targets depend on the location. If the user's projects have no files - string-based projects - set `stringBasedAvailable: true`, because it defaults to false and the app is otherwise absent from every one of their projects with no error anywhere.

[references/modules.md](references/modules.md) has the per-type matrix: placement in plain words, `prepare*`, extra fields, edition lock, and whether `preview` can open it at all.

### 3. Write the UI

Read [references/sdk.md](references/sdk.md) before writing SDK code - verified snippets from a working scaffold, and the traps types do not catch. Two that reach the user directly: call `resize()` after any layout change or the app looks truncated, and design a real empty state, because a first-run organization with no TMs or a project with no glossary renders a blank screen the user reads as broken.

**Every app is localizable, and that is not an optional extra.** The scaffold arrives wired for it - Lingui, a `locales/` folder, catalogs - so the default is to keep it, and stripping it would be the deviation. Put every user-visible string behind a macro (`<Trans>`, `<Plural>`, `` t` ``) as you write it, not afterwards: retrofitting means touching every line of JSX again. Then `npx crowdin-serverless-apps extract` collects them into `locales/*.po`, and `build` re-extracts by default. This app runs inside a localization platform whose own users work in their own languages; hardcoded English is the one thing they will notice immediately.

### 4. Give it a logo

The scaffold ships a placeholder `public/logo.svg`, and leaving it is the clearest sign nobody finished the job - every app in the menu wears the same generic tile. Draw the thing the app is about: layers for translation memories, bars for a report, a tag for a glossary.

Square `viewBox` matching the placeholder's `0 0 64 64`; no text, since it renders at roughly 24px; give the mark its own background, because host themes flip between light and dark and CSS cannot reach inside an `<img>`; self-contained, no remote fonts or images; a couple of KB at most. Write it to `public/logo.svg` - the manifest already points at `/logo.svg` and everything under `public/` ships in the bundle. Afterwards confirm `dist/logo.svg` exists and appears in `dist/bundle.zip`.

### 5. Green gate

```bash
pnpm typecheck && pnpm lint
npx crowdin-serverless-apps manifest validate
```

Do not hand code over while any of the three is red. `typecheck` catches invented SDK APIs; neither it nor `lint` reads `manifest.json`, which is what `manifest validate` is for. It checks the file against the schema, and it checks `scopes` against the code: every call the app makes through the API client is matched to the scope that method needs, `createStorage()` to `application.storage`, and the result compared with the manifest. A call the manifest does not cover is a problem with the exact string to add; a declared scope no call uses is a note. Validation is offline - no login, no app link - but the scope half reads the packages in `node_modules`, so run it after `pnpm install` and again whenever the manifest or the API calls change. It exits 1 on a problem, groups problems by section and prints a `fix` line under each; read that hint, it spells out the accepted values, scope strings included.

A green run that carries a `scope check skipped` note has not looked at the scopes. Do what the note says - `pnpm install`, or `pnpm update @crowdin/serverless-apps-cli @crowdin/crowdin-api-client` in an app that predates the check - and run it again ([scopes.md](references/scopes.md)).

Passing does not mean Crowdin will accept the manifest, only that the cheap mistakes are gone.

### 6. Sync the manifest

```bash
npx crowdin-serverless-apps manifest status   # exit code 1 means local and remote differ
npx crowdin-serverless-apps manifest push
```

Before publishing, not after. `publish` moves only the bundle; `modules`, `scopes` and `name` travel only with a push. `preview` also reads the manifest from Crowdin, not from disk, so an unpushed change means you are previewing the old placement.

### 7. Publish instead of running a dev server

```bash
npx crowdin-serverless-apps publish -y
```

Builds, uploads `dist/bundle.zip`, and switches Crowdin to serving it. Do not start `dev`: it ties the app to your machine and forces the user to keep a terminal open, which defeats handing them something that just works. `dev` is the right tool when the *user* wants a fast local loop, in their own terminal. Say the trade-off out loud - every iteration is now a build plus an upload rather than an instant reload.

### 8. Close the loop on visibility

```bash
npx crowdin-serverless-apps list --json
```

Each app reports its resolved `permissions`, so read them instead of assuming. A fresh install is `user: owner`, meaning the app works for whoever created it and is invisible to everyone else - and because `create` installed the app before you edited anything, setting `default_permissions` in the manifest afterwards does not widen an existing install.

So: derive the audience from the request ("so the translators can see it" is `all`, a vendor page is `guests`, a cost report is `owner`), and if it needs to be wider than what `list --json` reports, walk the user to the Crowdin apps manager in plain words. Skipping this is how a run ends badly with everything green: they share the link, a colleague sees nothing, and the app looks broken.

Remember calls run as the **viewer**. An app built for translators must only need what a translator may do, or it will 403 for exactly the person it was made for.

### 9. Open it, and look

```bash
npx crowdin-serverless-apps preview --module <key> [--project <id|identifier>]
```

Always pass `--module`: with more than one module and no flag the CLI refuses to choose. Project modules and `editor-right-panel` need a project - omitting `--project` falls back to the first project the login can access, which is fine for a smoke test and wrong whenever the content depends on the project, so pass the one from the user's link.

Seven of the eighteen types have no page to open - the three editor types other than `editor-right-panel`, plus `modal`, `chat`, `context-menu` and `navbar-extension` - and for those `preview` **fails with an error rather than degrading**. That is expected, not a bug: the handover is a short navigation instruction instead of a link. Tell the user that before scaffolding such a placement, and offer the linkable alternative.

Then verify, because nothing before this point proves the app renders: `typecheck`, `lint` and `publish` are all blind to a blank iframe, a 403, a clipped panel or an empty screen. If you can drive a browser, open the link, confirm the app draws and the console is clean. If you cannot, say plainly that it is unverified and ask the user to open it and tell you what they see. Never report success on the strength of a printed URL.

Hand the link over as a clickable link in your message - `preview` opens a browser in *your* environment, which may not be the user's.

## The auth handshake

Login is a browser OAuth flow, so it needs the user. Start it **in the background** so you can read its output while it waits:

```bash
npx @crowdin/serverless-apps-cli@latest login
```

In a non-interactive shell it prints the authorize URL as plain text - `open this link (https://accounts.crowdin.com/oauth/authorize?...)`. Pull that URL out and hand it over as a clickable link; the CLI's own attempt to open a browser may do nothing in your environment. It waits five minutes, then gives up - mention that, and restart it if they stepped away.

Never ask for a token in chat. On a machine with no browser at all, `CROWDIN_PERSONAL_TOKEN` in the environment works instead.

## When something goes wrong

| What you see | What it means |
|---|---|
| `Not logged in. Run crowdin-serverless-apps login first.` | The auth handshake. |
| The CLI hangs with no output on `create` | Missing `--template` or `--yes`; it waits on a picker. |
| `This app declares 2 modules. Pass --module <key>` | The untouched scaffold. Delete the module that is dead on this edition, then pass the key. |
| `Module type X only has a page on Crowdin Enterprise` / `only on crowdin.com` | Edition-locked placement. Pick one that exists on their edition. |
| `Module type X has no page of its own` | One of the seven unopenable types. Expected: hand over navigation, not a link. |
| Blank iframe, or the host reports the app did not start | Manifest type, `prepare*` and `context.app.type` disagree, or a registration is not at the top level, or two modules of one type without keys. |
| App published but absent from the UI slot | `manifest push` skipped, or a string-based project without `stringBasedAvailable`. |
| App opens but shows nothing | Empty data, or the first-project fallback landed somewhere with no glossary/TM/strings. Check before blaming the code. |
| 403 `Endpoint isn't allowed for token scopes.` at runtime | A manifest scope. Run `manifest validate`: it names the call and the string to add. If validate is green, run `manifest status` - the scope is on disk but not pushed. If it printed `scope check skipped`, do what the note says first; a green run with that note has not looked at the scopes ([scopes.md](references/scopes.md)). |
| Any other 403, or an empty list, all checks green | The viewer's own rights, not the manifest: calls run as whoever opened the app, and no check can see that. Build for what that person may do, or narrow the audience. |
| Every storage call rejected | `application.storage` missing from the manifest `scopes` - `manifest validate` names the `createStorage()` call that needs it - or, if validate is green, the manifest change not pushed. |
| `kv.user.*` throws while shared keys work | The viewer is anonymous - no user identity. Check `user.id` in the context before offering per-user features. |
| A write under `module:...:` rejected | The key names a module other than the one the app is currently running as, or an unknown one. On reads the same situation is silent: `get` resolves `undefined`, listings skip the records. |
| `You have no accessible projects to preview this module in.` | Either no project access, or a crowdsource module needing a public project. |
| `Could not list your projects - this login may lack the project scope.` | Log in again to grant it. |
| `GraphQL is not available to serverless apps` | The proxy is REST `/api/v2` only. |
| `FormData bodies are not supported through AP.apiRequest` | Pass a raw `Blob`, `File` or `ArrayBuffer`. |
| `manifest push` rejected by Crowdin | Run `manifest validate`; it names the offending path. |
| `npx` fails on network or permissions | Your environment, not the CLI. Ask the user to allow it. |

## References

- [references/modules.md](references/modules.md) - every module type: placement, `prepare*`, manifest fields, edition lock, what `preview` can open
- [references/sdk.md](references/sdk.md) - verified SDK snippets and the traps types do not catch
- [references/scopes.md](references/scopes.md) - how `manifest validate` derives the scopes from the code, and what it cannot see
- [references/cli.md](references/cli.md) - command and flag reference, env vars, what each command writes
