# Phases 3–4 — the delegation procedure

Reached from `SKILL.md`'s Phases 3–4 — Setup and Wrap, when phase 3 starts. The whole of both phases' execution: the dependency gate, the adapter handoff, the per-role invocations, the post-condition verification, and the one step that never delegates.

## Step one: `verify_skill_dependencies`

Phase 3 opens with this step. Read the `skills` block of `manifest-snapshot.json` — `plugin`, `install`, `installClaudeCode`, and the per-role lists `setup`, `convert`, and `recall` — then probe every skill named in those lists. A skill is available when it is loadable here: installed as part of its plugin, or present in a skills directory this environment reads. On a miss:

1. Name exactly which skills are missing, and which role needs them.
2. Run the snapshot's own install command yourself — `skills.install` (`npx skills add lingui/skills`). It installs into the user's skills directory, and it is exactly the dependency the manifest declares, not an improvisation. Re-probe when it finishes.
3. When that command fails, is declined, or cannot run in this environment, stop and hand the user the install command — `skills.install`, plus the agent's own plugin-manager equivalent when it manages skills as plugins (`skills.installClaudeCode`, `/plugin marketplace add lingui/skills && /plugin install lingui@lingui-skills`, is the Claude Code one) — then wait, and re-probe before continuing.

This is a hard gate, and the important half of it is what you must *not* do: **never substitute your own library guidance for a missing skill.** Not a summary of it, not "the general approach", not a best-effort config you are fairly confident about — improvised library instructions are stale the moment they're written. A missing dependency makes the correct outcome a paused run, never an improvised one. (A role whose manifest list names reference files rather than skills is satisfied by those files — the gate applies to named skills.)

The phase-2 probe recorded in `decisions.md` is advisory; a run resumed days later re-probes here rather than trusting it.

## Step two: read the adapter, then delegate

Before invoking anything, read the stack's adapter — `references.adapter` in the snapshot, for the v1 stack `references/libraries/lingui/crowdin.md`. It is the only library-specific file this skill still owns, it is per library rather than per framework, and it exists precisely to be handed over: locale codes, catalog layout, format, and the post-condition checklist.

Pass these as constraints on the delegated skill's output, stated up front in the invocation — not as context it may weigh against its defaults:

| Input | Value | Source |
|---|---|---|
| Locales | source + targets, in the decided code scheme | `decisions.md` |
| Catalog path and directory shape | the matched row's `source` / `translation` | `manifest-snapshot.json` |
| Catalog format | the row's `type` (`po` for the v1 stack) | `manifest-snapshot.json` |
| Scope of the wrapping pass | `sourceDir` and the plan's `wrap_<path>` entries | `detection.json`, `plan.md` |
| Context, not constraints | `framework`, `compiler`, `react`, `packageManager` — what was detected, handed over so the skill need not re-derive it, and overridden by its own findings if they disagree | `detection.json` |

This orchestrator, not the library, chooses the locale codes, because the Crowdin side has to agree with them; the delegated skill is told which codes to write, not asked to prefer a scheme. What proves the paths correct is gate three (`crowdin config translations`, phase 5), never the scheme by itself.

Then invoke, by role:

- **Phase 3, `skills.setup`** — framework and compiler routing, installation, library config, build-plugin wiring, catalog scaffolding, and the first extraction. Covers `install_packages`, `create_config`, `provider_wiring`, `scaffold_catalogs`, and `extract_smoke`. Ask it to confirm it supports this project's toolchain **before it installs anything**, and to say so rather than proceeding if it doesn't: the manifest matches on language and library only, so the first place an unsupported framework can be caught is inside this delegation, and catching it after `install_packages` leaves exactly the half-configured project the phase-2 stops exist to avoid.
- **Phase 4, `skills.convert`** — the wrapping pass, file by file, following the plan's `wrap_<path>` entries and ticking each as it completes. This is the only phase eligible for the optional parallel workers in the execution model; the partition unit is the file, and no worker runs extraction.
- **Phase 4, `skills.recall`** — invoked through `references/recall-self-check.md`: when it runs, and how residuals are recorded in the workspace and reported with file and line rather than silently dropped.

## Step three: verify the post-conditions on disk

A delegation that reports success has not finished; a delegation whose output you have read has. After `skills.setup` returns, verify by reading files against the adapter's post-conditions checklist — the locale set and scheme, the catalog path, shape, and format, the green extraction and build.

After `skills.convert` returns: every `wrap_<path>` step in `plan.md` is ticked, `build_check` is green, and the strings the plan enumerated actually appear as entries in the source catalog. That last one is the breadth check that matters — a convert pass can return success over an empty catalog, and only reading the catalog catches it.

A failed post-condition is a stop-and-ask, then a re-invocation with the violated constraint restated. Correcting a Crowdin-side value the adapter specifies — the locale list, the catalog path — is in scope for you. Re-implementing the library's setup yourself is not, for the same reason the dependency gate exists.

## Not delegated: the rules artifact

`generate_coding_rules` renders `references.rulesTemplate` (`references/libraries/lingui/rules.template.md`) into `.agents/crowdin-i18n-rules.md` following the procedure in `references/rules-template-format.md` — including its fail-closed rule: a render that cannot resolve a condition or a value writes nothing at all. `install_coding_rules` then adds the `CLAUDE.md` import and the `AGENTS.md` pointer, and only after a successful render. The template DSL is library-agnostic and much of the rendered file is Crowdin policy — key naming, never hand-editing downloaded translations, writing translator context while the code is still in front of you — so this step runs here, not in the delegation.
