# Plan format — a worked example

Reached from `SKILL.md`'s phase 2, step 7, before `plan.md` is written. The example below is the format contract in full — the header lines, the `## Phase N — Name` groupings, and one `- [ ] step_id — detail` line per step. Emit `plan.md` in exactly this shape; the step vocabulary and the rules for which steps a plan carries are `SKILL.md`'s, and the ids are load-bearing.

A `compile-time-extraction` full-journey plan:

```markdown
# i18n setup plan — js-ts-lingui
Entry point: full journey · Mode: unguided · Source: en · Targets: uk, es-ES
Catalog: lingui-po (compile-time-extraction) · References: from manifest-snapshot.json

## Phase 3 — Setup
- [ ] verify_skill_dependencies — the snapshot's skills.setup / convert / recall are loadable
- [ ] install_packages — library + compiler plugin, main thread
- [ ] create_config — locales written per `decisions.md`'s decided scheme (Crowdin locale codes, the greenfield default here)
- [ ] provider_wiring — provider + activation in the app entry point
- [ ] scaffold_catalogs — locale directories for en, uk-UA, es-ES
- [ ] generate_coding_rules — render .agents/crowdin-i18n-rules.md
- [ ] install_coding_rules — CLAUDE.md import + AGENTS.md pointer
- [ ] extract_smoke — extraction smoke test on the still-unwrapped tree
- [ ] build_verification — build green before any string moves
- [ ] commit_setup — commit config, provider wiring, catalogs, coding rules

## Phase 4 — Wrap
- [ ] wrap_src_App_tsx — 7 candidates
- [ ] wrap_src_components_PlanCard_tsx — 3 candidates
- [ ] module_i18n_approach — decide once how an i18n instance reaches non-render code; governs the module below
- [ ] wrap_src_data_plans_ts — 4 candidates
- [ ] extract_clean — extract once, after all wrapping
- [ ] catalogs_current — the catalogs the app loads agree with the source catalog
- [ ] build_check — build green again
- [ ] recall_self_check — whole-tree scan + bounded cleanup
- [ ] comment_review — translator context for the strings just added
- [ ] commit_wrap — commit the wrapped files and regenerated catalogs

## Phase 5 — Connect
- [ ] create_project — optional; skip when the project id is known
- [ ] write_crowdin_yml
- [ ] config_lint
- [ ] config_sources
- [ ] config_translations
- [ ] upload_dryrun
- [ ] upload_sources
- [ ] commit_connect — commit crowdin.yml

## Phase 6 — Context
- [ ] context_download
- [ ] fill_ai_context
- [ ] context_upload
- [ ] context_status
- [ ] generate_glossary
- [ ] upload_glossary
- [ ] draft_translations — only when decisions.md records the draft-translations add-on; commits the drafted target catalogs

## Phase 7 — CI
- [ ] write_workflow
- [ ] commit_ci — commit the workflow file
- [ ] set_secret_instruction
- [ ] prove_round_trip — translate one string in Crowdin, sync, see it render in the app
```
