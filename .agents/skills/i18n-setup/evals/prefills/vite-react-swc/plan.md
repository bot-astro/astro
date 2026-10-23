# i18n setup plan — js-ts-lingui
Entry point: full journey · Mode: unguided · Source: en · Targets: uk, es-ES Catalog: lingui-po (compile-time-extraction) · References: from manifest-snapshot.json

## Phase 3 — Setup
- [ ] verify_skill_dependencies — lingui-framework-setup (setup); lingui-best-practices, enhanced-message-context (convert); find-unwrapped-strings (recall) all loadable
- [ ] install_packages — @lingui/core, @lingui/react, @lingui/cli, @lingui/vite-plugin, @lingui/swc-plugin, @lingui/detect-locale; main thread
- [ ] create_config — lingui.config.ts: sourceLocale en, locales [en, uk-UA, es-ES] (Crowdin locale codes)
- [ ] provider_wiring — I18nProvider + locale detection/activation in src/main.tsx
- [ ] scaffold_catalogs — locale directories for en, uk-UA, es-ES under src/locales/
- [ ] generate_coding_rules — render .agents/crowdin-i18n-rules.md from references/libraries/lingui/rules.template.md
- [ ] install_coding_rules — CLAUDE.md import + AGENTS.md pointer
- [ ] extract_smoke — extraction smoke test on the still-unwrapped tree
- [ ] build_verification — build green before any string moves
- [ ] commit_setup — commit config, provider wiring, catalogs, coding rules

## Phase 4 — Wrap
- [ ] wrap_src_App_tsx — 11 candidates
- [ ] wrap_src_components_PlanCard_tsx — 2 candidates
- [ ] module_i18n_approach — decide once how an i18n instance reaches non-render code; governs plans.ts below
- [ ] wrap_src_data_plans_ts — 4 candidates
- [ ] extract_clean — extract once, after all wrapping
- [ ] catalogs_current — the catalogs the app loads agree with the source catalog; this project's Vite plugin reaches that at import, so nothing to run beyond verifying it
- [ ] build_check — build green again
- [ ] recall_self_check — whole-tree scan + bounded cleanup
- [ ] comment_review — translator context for the strings just added
- [ ] commit_wrap — commit the wrapped files and regenerated catalogs

## Phase 5 — Connect
- [ ] write_crowdin_yml — project id 1 (known; no create_project step)
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
- [ ] generate_glossary — starting glossary from the source catalog, to .crowdin/glossary.csv
- [ ] upload_glossary — after the user reviews the terms; glossary list first, then create or --id

## Phase 7 — CI
- [ ] write_workflow
- [ ] commit_ci — commit the workflow file
- [ ] set_secret_instruction
- [ ] prove_round_trip — translate one string in Crowdin, sync, see it render in the app
