# Starting glossary

Reached from `SKILL.md`'s Phase 6 — Context, for the `generate_glossary` and `upload_glossary` steps of `plan.md`. Both steps are one delegation: the **`glossary-terms-extraction`** skill drafts the terms, holds the review gate, and runs the upload. This file is the handoff — what to pass it, and what to record when it returns.

## Delegate

Confirm the `glossary-terms-extraction` skill is loadable first. It ships alongside this one, so a run that installed the whole `crowdin/skills` set — or the plugin, in any agent that installs it as one — already has it; otherwise the fix is one install command (`npx skills add crowdin/skills --skill glossary-terms-extraction`). If it is missing, ask the user to install it and wait — do not improvise a glossary in its place; phase 3's dependency-gate reasoning carries over unchanged.

Then invoke it under its own invoked-by-another-workflow contract, passing what this journey already knows so it asks the user nothing twice:

| Input | Value | Source |
|---|---|---|
| Source strings | the matched row's resolved `source` path — the same file phase 5 just uploaded | `manifest-snapshot.json`, resolved on disk |
| Product description | the app's one-line description | `decisions.md` |
| Draft path | `.crowdin/glossary.csv` — inside the gitignored workspace, so a rejected draft never lands in the repository | this file |
| Source language | the source locale's Crowdin language id | `decisions.md` |

The two plan steps map onto its two stages: `generate_glossary` is done when the draft exists and the user has seen the terms; `upload_glossary` runs only after their explicit go. The review gate is that skill's own rule — this journey never waives it on the user's behalf.

## The project's own glossary already exists

On a full journey, phase 5's `create_project` leaves an empty glossary behind — Crowdin creates one with every new project — so the delegate's duplicate check finds a match for a project this run made minutes ago. Expect that, and say so when handing over: it is not the collision the check exists to catch, and the delegate reads the term count to tell the two apart.

## What comes back

One of two outcomes, and both are complete:

- **Uploaded** — record the printed glossary id in `decisions.md`: it is what a later run passes to `--id` instead of creating a duplicate. Tick both steps.
- **Declined** — the draft stays at `.crowdin/glossary.csv`; note that in `decisions.md`, tick the step, and move on. A declined upload is a decision, not a failure.
