---
name: i18n-setup
description: Takes a project from hardcoded strings to internationalized, connected to Crowdin, and translating continuously. Use when someone wants to add i18n or multi-language support, internationalize an app, extract hardcoded strings, set up Lingui (or another i18n library), or connect Crowdin to a repository - including connect-only, "my app already uses i18next/next-intl/vue-i18n/Lingui, wire it to Crowdin". Not for individual `crowdin` CLI commands or crowdin.yml syntax (that is `crowdin-cli`), not for editing Crowdin context files or `ai_context` on their own (`context-extraction`), not for generating a glossary alone (`glossary-terms-extraction`), not for uploading screenshots alone (`generate-screenshots`), and not for building Crowdin apps (`create-app`).
---

# i18n Setup

This skill orchestrates the journey: the phase sequence, the decisions, and the filesystem workspace those decisions live in. Library-specific work runs through the library's own skills, routed by `manifest.json`; this skill does the Crowdin side of each phase and verifies the delegated work on disk. For any `crowdin` command's syntax and semantics, read the `crowdin-cli` skill.

Load references lazily: read `manifest.json` when phase 2 starts, and read a reference only when the step that needs it is the step you are on. A phase you skip should cost nothing.

## The journey

| Phase | Name | Produces | Credentials |
|---|---|---|---|
| 1 | Detect | `.crowdin/detection.json` | none |
| 2 | Decide | `decisions.md`, `manifest-snapshot.json`, `plan.md` | none |
| 3 | Setup | i18n library installed, configured, catalogs scaffolded, `.agents/crowdin-i18n-rules.md` rendered | none |
| 4 | Wrap | every user-visible string wrapped, catalogs populated, build green | none |
| 5 | Connect | `crowdin.yml`, verified config, sources uploaded | **token gate** |
| 6 | Context | `ai_context` written for the new strings, a starting glossary uploaded, screenshots when opted in | token |
| 7 | CI | sync workflow + the secret instruction for the user | token (as a CI secret) |

Two entry points share this spine, and phase 2 decides which: the **full journey** runs all seven phases; **connect-only** — the project is already internationalized — omits phases 3–4 entirely rather than emitting no-op steps. Everything before phase 5 is offline; the token gate opens phase 5 — say so at the end of phase 2, so the user can authorize the CLI while phases 3–4 run.

## Execution model

> The plan is a linear checklist. Execute steps in order on the main thread. If your environment provides a subagent/task tool, you may dispatch the convert phase's file partitions as parallel workers that write `progress/wrap-N.json`; collect them all before the verify step. Never require this: a run with no subagent support must produce an identical end state, just serially.

Two rules are correctness, not architecture, and hold in both modes:

- **Extraction runs once, after all wrapping is done.** Parallel writers pointed at one catalog race and lose messages. Wrap every file first, then extract a single time.
- **Ambiguity is stop-and-ask, never improvisation.** If a reference does not cover the case in front of you — an odd config layout, two i18n libraries present, a string you cannot tell is user-visible — stop and ask. Guessing produces plausible code that breaks a build the user has to debug later.

Package installs always run on the main thread, never inside a worker — including when a delegated skill does the installing — so the lockfile stays consistent.

## The workspace

```
.crowdin/
  detection.json          # phase 1 output: stack, existing i18n, candidate files, locale signals
  decisions.md            # frozen user choices: entry point, locales, library, scope, add-ons, mode
  plan.md                 # the executable checklist ("- [ ] step_id" lines)
  manifest-snapshot.json  # verbatim copy of the matched stacks[] entry + the catalogs[] rows it uses
  progress/               # ONLY when parallel wrap workers are dispatched (wrap-N.json)
```

Rules for it:

- **Create and ignore it on first run.** Add `.crowdin/` to the repo's `.gitignore`. If `.gitignore` already has content, show the one-line diff you intend to make before making it. The rules artifact is *outside* the workspace: `.agents/crowdin-i18n-rules.md` is a committed project file.
- **Ticking checkboxes in `plan.md` is the progress protocol.** Check each step off as it completes — `- [ ]` becomes `- [x]`. In the default linear run that is the entire bookkeeping story, and it is enough to resume from. A `progress/` directory exists only when you actually dispatched parallel wrap workers; a serial run never creates one.
- **Resume, don't silently re-plan.** If `plan.md` already exists when you start, report how far the previous run got (ticked versus unticked steps) and ask whether to resume from the first unticked step or restart from detection, which rewrites `detection.json` and `plan.md`. Unguided runs resume.
- **Write atomically.** Every workspace file is written as `<name>.tmp` and then `mv`'d into place, so an interrupted run never leaves a half-written JSON file that the next run parses as truth.
- **Disk beats records.** When a recorded value disagrees with the repository as it is right now — a path in `decisions.md` that no longer exists, a locale directory someone deleted — the disk wins. Re-resolve the value, note the correction in `decisions.md`, and keep going.

## Phase 1 — Detect (read-only)

Phase 1 writes exactly one file, `.crowdin/detection.json`, and touches nothing else in the repository. No installs, no edits, no `git` writes.

Route by language on first match, then follow that language's rules file:

| Signal at the repository root | `language` | Detection rules |
|---|---|---|
| `package.json` | `js-ts` | `references/detect/js-ts.md` |
| *no row matches* | — | Stop: "This project's language isn't supported yet." Name what is (JavaScript/TypeScript today), and offer connect-only if translation catalogs already exist that Crowdin can take as-is. |

Detection *rules* are in the per-language reference (adding a language family: `references/extending.md`).

Emit this schema. The field names and the value sets are load-bearing: phase 2 matches `stacks[]` entries against these exact fields, and the collapse ladder reads these exact values. Renaming one silently breaks routing rather than failing loudly.

```json
{
  "language": "js-ts",
  "framework": "vite" | "next" | "cra" | "unknown",
  "compiler": "swc" | "babel" | null,
  "react": true, "typescript": true,
  "packageManager": "npm" | "yarn" | "pnpm" | "bun",
  "sourceDir": "src",
  "git": { "isRepo": true, "branch": "main", "remote": "..." },
  "existing": {
    "library": "lingui" | "next-intl" | "vue-i18n" | "i18next" | "none",
    "configured": false, "stringsWrapped": "yes" | "partial" | "no",
    "crowdinYml": false, "workflow": false
  },
  "candidateFiles": [{ "path": "src/App.tsx", "matchCount": 7 }],
  "localeSignals": { "existingLocaleDirs": [], "readmeHints": [] }
}
```

The `|` unions above are the allowed value sets, not values: write literal JSON, one concrete value per field, never the union text itself. Every key is required — `null`, `"none"`, `[]` for the absences. Report uncertainty in the prose summary, never as an invented value or an extra key.

Then summarize what you found for the user in a few lines — stack, existing i18n state, how many files carry candidate strings — before asking anything.

## Phase 2 — Decide

Everything in this phase is a decision, not an action. Nothing is installed and no project file is modified until the plan is approved.

**1. Resolve the entry point.** If `existing.library != "none" && existing.stringsWrapped != "no"`, the project is already internationalized: offer **connect-only as the default** and the full journey as the alternative (which would mean migrating libraries — a real ask, never a default). Otherwise the full journey is the default. An existing foreign i18n library is never a reason to refuse; it is the reason connect-only exists.

**2. Check the hard stops.** First match wins, and this check happens *after* `detection.json` is on disk and *before* `plan.md` is created.

| Condition | Verdict |
|---|---|
| A build toolchain that cannot be extended without ejecting from it — today that means `framework: "cra"` (`react-scripts` in dependencies) | **Stop.** Wrapping needs a build the setup delegation can add a transform to; a toolchain that forecloses that has no path in, and stopping before anything is installed beats a half-configured project. Use the message below. |
| No `stacks[]` entry matches the detected stack **and** `existing.library` is `"none"` | **Stop.** There is no conversion pipeline for this stack and no catalogs to connect. List the stacks that are supported and the catalog formats connect-only accepts, so the user knows which of the two gaps to close. |
| Native/hybrid shell signals (`react-native`, `expo`, `@capacitor/core`) next to a web entry point | **Not a stop.** Route the web path and say plainly that the native shell's own strings are out of scope for this run. |
| `existing.library != "none"` | **Not a stop.** This is connect-only. |

A hard stop still writes `detection.json` — that file is the evidence for the refusal — and writes nothing else: no `plan.md`, no `crowdin.yml`, no installs, no change to `package.json`. Print the message, then stop. Do not offer to proceed anyway.

Say it in full rather than in initials, so the user can search for the toolchain by name. For the first row:

> This project is built with Create React App (`react-scripts`), which this skill can't internationalize. Wrapping strings needs a build the i18n library can add its transform to, and CRA's is closed to that without ejecting — on top of which the toolchain has been unmaintained since 2023. Move to a build that accepts plugins, such as Vite or Next.js, and run this again — the rest of the journey works unchanged afterwards. If the app already has translation catalogs, I can skip the conversion and connect them to Crowdin instead.

**3. Collect the locale set, the code scheme, and the mapping.** Ask for the source locale and the targets, then freeze in `decisions.md` not just which locales but how they're spelled and which `languages_mapping` rows that spelling needs — phases 3 and 5 both read all three. The `translation` pattern's language placeholder is always **`%locale%`**. **Greenfield default spelling: each target's Crowdin locale code** (`uk-UA`, `pt-BR`, `es-ES`) — exactly what `%locale%` resolves to, so the default needs no `languages_mapping` at all. Read the code per language from `crowdin language list --all --code locale -o toon`: the full supported-language list, needing no token and no `crowdin.yml`, which also validates unfamiliar codes before credentials exist. A machine without the CLI yet runs phase 5's CLI preflight early — that install is global, not a project change.

**Record two codes per language, not one.** `%locale%` resolves to the **locale code**; the CLI's `-l/--language` options take the **language id**, and so do `languages_mapping` keys and the glossary's source language in phase 6. The id is its own field, not a prefix of the locale — many ids carry the region too (`es-ES`, `pt-BR`, `zh-CN`). `crowdin language list --all` returns ids by default and locale codes with `--code locale`; run it both ways and join the two lists on the language name.

**Two cases override the default spelling:** the user asks for specific codes (URL segments like `/uk/`, an in-house convention), or the project's locale directories already spell them (`detection.json`'s `localeSignals.existingLocaleDirs`). Keep that spelling and record one `languages_mapping` row per target whose code differs from its Crowdin locale code — never rename a project's directories to dodge a row. `references/connect.md` and the `crowdin-cli` skill's configuration reference own the mapping mechanics; this file doesn't repeat them.

**4. Ask which Crowdin edition.** crowdin.com or Crowdin Enterprise. Enterprise means one extra recorded value — the organization — which becomes `base_url: https://{org}.api.crowdin.com` in `crowdin.yml` and, when a personal access token is used, where it is created. Nothing else in the journey differs.

**5. Confirm the remaining choices**, then freeze all of them in `decisions.md`: the app's one-line product description (phase 6 passes it to the context skill), the scope of files to wrap, whether to work on a new git branch, mode (**guided** — confirm at each phase boundary; **unguided** — run the approved plan to completion, stopping only on ambiguity or a hard stop), and add-ons. Add-ons are off by default, including `crowdin auto-translate`: never machine-translate a fresh project unasked, and when the user does opt in, `--method tm` only. A second add-on, **draft translations**, is never pitched here: it is recorded only when the user asks for translations during the journey, and it adds a `draft_translations` step to phase 6 that delegates to the `translate` skill.

Two add-ons have to be *asked* rather than left off. The first is the **unwrapped-string lint rule**: phase 4's recall delegate installs it either way as a permanent dev dependency — its own skill recommends the install and treats the no-install path as the lesser one, so a plan that never raises it forces this orchestrator to answer a dependency question on the user's behalf, silently. Put it to them at Decide — the rule catches the next unwrapped string, the audit alone catches only today's — and record either answer, so phase 4 hands the delegate a decision instead of inheriting one.

The second is **screenshots**: phase 6 can delegate to the `generate-screenshots` skill, which writes a Playwright capture of the running app into the project and uploads the images to Crowdin tagged to the strings they show — the strongest context a translator can get, and the one step in the journey that needs the user's hands mid-run: the app started, themselves signed in. Silence is not consent for that, so ask; the plan carries `generate_screenshots` only on a yes.

**6. Match the manifest.** Compare the structural keys of each `stacks[]` entry's `match` object against the equally-named fields of `detection.json`. An entry matches on `language` and `library`, nothing more; `framework`, `compiler`, `react` and the rest are context to hand the delegation, never keys to route on. The `library` key is the choice being offered, so it is matched against the user's answer, not against existing state. Then write `manifest-snapshot.json`: the matched entry verbatim, including its `variant`, `catalog`, `skills` block, and `references` paths, plus the `catalogs[]` row named by `catalog` (with its `model`, `source`, `translation`, `type`, and `docs`). From here on, read paths, recipes, and delegation targets from the snapshot, not from the live manifest — a run planned against one manifest must execute against that same manifest.

While the snapshot is open, probe whether the skills its `skills` block names are loadable here, and record the answer in `decisions.md` with the install command the user would need. This is a note, not a gate: **planning never blocks on skill availability** — the gate is `verify_skill_dependencies` at the top of phase 3; recording it now lets the plan warn the user in advance instead of stopping them later. In `plan.md` the warning rides the step's own line — `- [ ] context_download — crowdin-cli not loadable here; npx skills add crowdin/skills --skill crowdin-cli` — the step id and checkbox stay canonical, never replaced by a pause marker.

**7. Emit `plan.md` as a strict checklist.** Every step is one line that *begins* with its id:

```
- [ ] step_id — short human-readable detail
```

The id starts the line, immediately after the checkbox. Anything a human or a later step needs goes after the id on the same line or in indented sub-bullets — never before it, and never instead of it. Group the lines under `## Phase N — Name` headings.

The step vocabulary is keyed by the matched catalog's `model` from the snapshot.

Shared by every stack:

| Phase | Steps |
|---|---|
| 3 Setup | `verify_skill_dependencies` first, then `install_packages`, `create_config`, `provider_wiring`, `scaffold_catalogs`, `generate_coding_rules`, `install_coding_rules`, `build_verification`, `commit_setup` |
| 4 Wrap | one `wrap_<path>` step per file (`wrap_src_App_tsx`) — non-component modules (data and util modules, API routes: files that render nothing) come last, behind a single `module_i18n_approach` step that decides once how an i18n instance reaches non-render code, because per-file improvisation here is how threading goes subtly wrong — then `build_check`, `recall_self_check`, `comment_review`, `commit_wrap` |
| 5 Connect | `write_crowdin_yml`, `config_lint`, `config_sources`, `config_translations`, `upload_dryrun`, `upload_sources`, `commit_connect`, plus optional `create_project` |
| 6 Context | `context_download`, `fill_ai_context`, `context_upload`, `context_status`, `generate_glossary`, `upload_glossary`, plus `draft_translations` only when the add-on is recorded, and `generate_screenshots` when the screenshots add-on was chosen |
| 7 CI | `write_workflow`, `commit_ci`, `set_secret_instruction`, `prove_round_trip` |

Added by `model`:

| `model` | Adds |
|---|---|
| `compile-time-extraction` | `extract_smoke` in phase 3 (prove extraction works before wrapping anything), then in phase 4, after every file is wrapped: `extract_clean`, then `catalogs_current` — the catalogs the app loads agree with the source catalog extraction just rewrote. `catalogs_current` is an outcome, not a command: some pipelines reach it through a compile step, others compile at import and reach it by doing nothing, and which one this project has is the setup skill's business, not a field in this manifest |
| `runtime-catalog` | `verify_catalogs` in phase 4 instead of all three — nothing extracts, so nothing keeps the hand-authored catalog in step with the code. The check is that every key the code references resolves in the source catalog; `references/verify-catalogs.md` says what to compare, what a missing key costs once phase 5 has uploaded, and why the count of keys it could not resolve statically is part of the result |

`verify_skill_dependencies` leads phase 3 because phases 3–4 are delegated to the ecosystem skills the snapshot names, and a plan that starts installing before confirming its dependencies are loadable can strand a half-configured project. Connect-only plans have no phases 3–4 and therefore never emit it — they delegate nothing.

`create_project` runs first within phase 5 when it runs at all, because `write_crowdin_yml` needs the numeric project id; when the project already exists the user supplies that id instead. The other six phase-5 steps keep the order shown — that sequence is the safety gate, not a suggestion.

Every phase that writes repository files ends with its commit step — `commit_setup`, `commit_wrap`, `commit_connect`, `commit_ci` — committing that phase's files on the branch `decisions.md` names. The approved plan is the consent for these commits, so none of them asks again, and a run that stops mid-journey loses at most one phase of work. Phase 6 writes only to Crowdin and the gitignored workspace, so it has no commit step, except that `draft_translations`, when recorded, commits the target catalogs it drafted into, so phase 7's round trip starts from clean targets; a phase whose files turn out untouched (a collapse-ladder omission) skips its commit the same way.

**Before writing `plan.md`, read `references/plan-format.md`** — the format contract as one full worked `compile-time-extraction` plan — and emit yours in exactly that shape.

**8. Get an explicit go.** Show the plan and state what it will change — and when the snapshot's `supportLevel` is anything but `stable`, say so plainly: the user is approving a less-proven path. Then wait for approval. No step executes before that word. In an unguided run the go covers the whole plan; in a guided run it covers the next phase.

## Phases 3–4 — Setup and Wrap (delegated)

Phases 3–4 **delegate**: installing an i18n library, wiring its compiler plugin, and wrapping strings in its idiom are the library ecosystem's job, maintained against every release. This orchestrator supplies the Crowdin-side constraints the library's skills cannot know, then checks the result on disk.

Read `references/delegate.md` when phase 3 starts — it is the whole procedure: the `verify_skill_dependencies` gate, the adapter handoff (`references.adapter` in the snapshot: locale codes, catalog layout, format, and the post-condition checklist), the constraints stated in each invocation, the per-role invocations, the post-condition verification on disk, and the `generate_coding_rules` render. One of its rules binds even before it is read: **never substitute your own library guidance for a missing skill** — not a summary of it, not "the general approach". A missing dependency makes the correct outcome a paused run, never an improvised one.

## Phase 5 — Connect

`references/connect.md` covers the CLI preflight, `crowdin.yml` authoring, the token gate, project creation or verification, and every gate's expected output. The order of the gates is fixed and does not vary by stack:

```
crowdin config lint  →  crowdin config sources  →  crowdin config translations  →  crowdin upload sources --dryrun  →  real upload
```

Only `config lint` answers without credentials; the other three all reach the project, which the gate order absorbs — the token and project id exist before any of them runs. `crowdin config translations` is the kill switch: it resolves the `translation` pattern's `%locale%` placeholder — through the `languages_mapping` rows, if any were decided — against the project's actual languages, printing the per-language paths the pattern resolves to so you can compare them against the layout phase 3 created. Only after all four pass does `upload_sources` run for real.

**The agent never sees the token value.** Not in a prompt, not in a variable it echoes, not in a file it writes. `crowdin.yml` always carries `api_token_env: "CROWDIN_PERSONAL_TOKEN"` and never a literal token; `project_id` is committed, because it is not a secret. `connect.md`'s token gate covers how a token is detected, how the user obtains one — `crowdin login` in their own terminal by default — and how it is verified without ever being read.

## Phase 6 — Context

Context is the part of this journey a human would skip and a translator would miss most. It is a handoff between two existing skills, run after the first successful upload:

1. `crowdin context download` — every string, with no `--status` filter: the adapter's reference-comment constraint puts a `#:` reference in every string's context, so `--status empty` would match nothing. Commands: the `crowdin-cli` skill's [context reference](../crowdin-cli/references/context.md).
2. Invoke **`context-extraction`** to fill `ai_context` in the downloaded JSONL, passing what this skill uniquely knows: the app's one-line product description from `decisions.md`, and the `#:` source references that came along with the strings (PO uploads carry them into the JSONL `context` field, so the extraction skill can read the actual code around each string instead of guessing).
3. `crowdin context upload` — push the enriched file back.
4. `crowdin context status` — report coverage to the user.

Then the glossary, in the same phase because it needs the same two things: the project to exist, and the catalog already uploaded. `generate_glossary` and `upload_glossary` delegate to the **`glossary-terms-extraction`** skill, which drafts the terms, holds the review gate, and runs the idempotent upload. `references/glossary.md` is the handoff: what to pass it, and what to record when it returns.

When `decisions.md` records the draft-translations add-on, `draft_translations` closes the phase, after the glossary exists to be respected. It delegates to the **`translate`** skill, which syncs, drafts into the local target files, verifies, and uploads the drafts as unapproved suggestions. `references/translate.md` is the handoff.

Screenshots close the phase when the add-on was chosen at Decide: `generate_screenshots` delegates to the **`generate-screenshots`** skill, which plans the screens with the user, commits a capture definition, captures the running app, and uploads images auto-tagged to the strings they show — `ai_context` tells translators what a string means, a screenshot shows where it lives. It runs last because it is the only step here that needs the app running and the user present to sign in. `references/screenshots.md` is the handoff: what to pass, what stays the delegate's own, and what each outcome records.

## Phase 7 — Continuous sync (CI)

Delegated, like phases 3–4. The **`github-action`** skill writes the workflow — its inputs, the version to pin, and everything that only fails once a run is on a runner. `references/continuous-sync.md` covers what comes before and after: the decision (a workflow, or Crowdin's native GitHub integration), the gate that runs before anything is written (`git ls-remote origin 'l10n*'` — an existing `l10n*` branch means an automation already syncs this repository, and a hit is a stop-and-ask), the handoff, and the post-conditions to read back.

`write_workflow` then delegates, and `set_secret_instruction` asks the user to set `CROWDIN_PERSONAL_TOKEN` as a repository secret themselves — same rule as phase 5, the value never passes through the conversation. The secret is a personal access token; `continuous-sync.md` says when the user has to create one first.

`prove_round_trip` closes the journey, and it is the last step for the same reason `extract_smoke` is an early one: **every step before it can pass while the app still shows one language.** Translate a single string in the Crowdin project — one word is enough, by hand in the editor — let the sync bring it back, and see it render. That exercises the whole chain end to end: the catalog the workflow downloads, the path `%locale%` resolved to, the macro the setup phase wired, and the locale the app negotiates. Nothing else does; a green build, a full catalog, four passed gates and a written workflow are all compatible with a project that will never display a translation.

It needs the user twice — they hold the token, and they own the translation — so it is a step to *ask for and wait on*, not one to report as done. When they would rather stop, say plainly what is unproven and leave the step unticked: an honest 12-of-13 beats a complete-looking plan over an untested pipeline.

## Collapse ladder

Re-running this skill on a repository it already touched must be safe and quiet. Each rung reads its condition from `detection.json`:

| Already true | Do this instead |
|---|---|
| `existing.stringsWrapped: "yes"` | Skip phase 4. There is nothing to convert. |
| `existing.configured: true` | Verify the config against the adapter's post-conditions and repair only what is wrong. Do not recreate it, and do not re-run the setup delegation over a working config. |
| `existing.crowdinYml: true` | Run `config lint` plus the sources/translations gates against the existing file and fix mismatches. Do not regenerate it — and **the plan omits `write_crowdin_yml` entirely**, keeping the gate steps. A checklist line named for writing a file that nothing will write is a lie the executor of that plan may act on. |
| `existing.workflow: true` | Leave the workflow alone; report its trigger set. **The plan omits `write_workflow`** for the same reason. |
| All of the above | Still write `plan.md`, and still run `config_lint` (plus `config_sources` and `config_translations` where credentials are available) against the existing config as the verification that nothing has drifted, before reporting `crowdin status` and offering only the genuinely missing add-ons. "Change no files" means the *project's* files — planning and read-only verification are not project changes. |

## Extension contract

A stack is a **language family plus a library** (`js-ts-lingui`), never finer — frameworks are the delegated ecosystem's routing, not manifest keys. What a new stack adds, and which shared files it never edits, is `references/extending.md`; nothing there runs during a setup journey.

## Related skills

Phases 5–7 name their Crowdin-side delegates in place: `crowdin-cli` (the authoritative surface for every `crowdin` command and for `crowdin.yml` syntax, placeholders, and language mapping), `context-extraction` (phase 6's context work, with the `crowdin context` commands in `crowdin-cli`), `glossary-terms-extraction` (the starting glossary), `translate` (phase 6's optional drafts), `generate-screenshots` (the screenshots add-on, when chosen), and `github-action` (**required** for phase 7's default path). Library-side, the `lingui` plugin from [lingui/skills](https://github.com/lingui/skills) is **required** for phases 3–4 of a Lingui stack; which of its skills covers which role — setup, convert, recall — is the snapshot's `skills` block, and the install commands live in the manifest and `references/delegate.md`. One boundary note: a project already on i18next whose owner wants Lingui is a library migration (upstream's `migrate-i18next-to-lingui`), out of scope for this orchestrator — its i18next path is connect-only.
