# Decisions — vite-react-swc

Frozen at the end of phase 2. Disk beats records: if any value here disagrees with the repository as it stands when phase 3 resumes, re-resolve from disk and correct this file rather than trusting the stale value.

## Entry point

**Full journey.** `existing.library` is `"none"` in `detection.json`, so there is no already-internationalized state to offer connect-only against.

## Locales

- Source: English — id `en`, locale `en`
- Targets: Ukrainian — id `uk`, locale `uk-UA`; Spanish — id `es-ES`, locale `es-ES`
- Scheme: **Crowdin locale codes** (`uk-UA`, `es-ES`) — the greenfield default: directories are named with exactly what `%locale%` resolves to, since the fixture has no pre-existing catalogs and asked for no custom codes.
- Placeholder: `%locale%`
- Mapping: none — the spelling equals the locale codes, so no `languages_mapping` block is written.

Two codes per language, per phase 2 step 3: ids from `crowdin language list --all -o toon` (the default code), locale codes from the `--code locale` run, joined on language name — the ids feed the CLI's `-l/--language` options, any `languages_mapping` key, and the glossary source language. No token or `crowdin.yml` needed at this stage.

## Library

**Lingui**, matched to the `js-ts-lingui` stack entry in `manifest.json`. `manifest-snapshot.json` holds the frozen copy of that entry (plus the `lingui-po` catalog row) this run executes against.

## Crowdin edition

**crowdin.com** (not Enterprise — no `base_url` override, no organization to record).

## Project

Existing Crowdin project, id `1`. `create_project` is therefore omitted from `plan.md` entirely; `write_crowdin_yml` writes the known id rather than creating a new project.

## Product description

BrewLog is a coffee-brewing log: home baristas track the brews they make, log cups as they go, and pick a plan (Home barista or Cafe pro) for more tasting notes and recipe sharing. Phase 6 passes this one-liner to the context-extraction skill alongside each string's `#:` source reference.

## Scope

All three candidate files named in `detection.json`: `src/App.tsx`, `src/components/PlanCard.tsx`, `src/data/plans.ts`. No exclusions requested.

## Branch

Skip branch creation — work directly on the current branch (the eval harness prepares a throwaway repo per run; there is nothing to protect with a branch).

## Mode

**Unguided.** Run the approved plan to completion; stop only on ambiguity or a hard stop.

## Add-ons

None selected. `crowdin auto-translate` stays off — never machine-translate a fresh project unasked.

The unwrapped-string lint rule was asked at Decide and declined — recorded either way, so phase 4 hands the recall delegate a decision instead of inheriting one. The audit-only path runs; no dev dependency is left behind.

## Skill availability probe (phase 2, advisory only)

Probed for loadability in this environment. The real gate is `verify_skill_dependencies` at the top of phase 3, which re-probes rather than trusting this record — a user can install the plugin between plan approval and execution.

| Role | Skill(s) | Probed here | Install command if missing |
|---|---|---|---|
| setup | `lingui-framework-setup` | not yet loaded | `npx skills add lingui/skills` (Claude Code: `/plugin marketplace add lingui/skills && /plugin install lingui@lingui-skills`) |
| convert | `lingui-best-practices`, `enhanced-message-context` | not yet loaded | same as above |
| recall | `find-unwrapped-strings` | not yet loaded | same as above |
