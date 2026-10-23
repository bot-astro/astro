# Drafting translations

Reached from `SKILL.md`'s Phase 6 — Context, for the `draft_translations` step of `plan.md`, which exists only when the user asked for translations during the journey and the add-on is recorded in `decisions.md`. The step is one delegation: the **`translate`** skill syncs, drafts into the local target files, verifies, and uploads the drafts to Crowdin as unapproved suggestions. This file is the handoff — what to pass it, and what to record when it returns.

## Delegate

Confirm the `translate` skill is loadable first. It ships alongside this one, so a run that installed the whole `crowdin/skills` set — or the plugin — already has it; otherwise the fix is `npx skills add crowdin/skills --skill translate`. If it is missing, ask the user to install it and wait — do not draft translations in its place; phase 3's dependency-gate reasoning carries over unchanged.

Then invoke it under its own invoked-by-another-workflow contract, passing what this journey already knows so it asks the user nothing twice:

| Input | Value | Source |
|---|---|---|
| Target languages | every target locale, with its Crowdin language id | `decisions.md` |
| Catalog paths | the matched row's resolved `source` and `translation` paths | `manifest-snapshot.json`, resolved on disk |
| The go | the approved plan | phase 2 |

The step runs after `upload_glossary`, so the glossary the drafts must respect already exists in the project. The delegate's own rules hold unchanged: it shows the per-language counts before uploading, never approves, never invents a glossary translation.

## What comes back

A per-language table — drafted, skipped, flagged. Record the drafted counts in `decisions.md`, commit the target catalogs the delegate drafted into (the approved plan is the consent, as for every phase's commit step; the drafts are also in Crowdin as suggestions, so the next sync brings back whatever reviewers decide), and tick the step. Skipped strings are not a failure; they are the ones a reviewer in Crowdin answers. A run that stops before upload leaves the ledgers in `.crowdin/translate/`, and the delegate resumes from them.

## The rules file

The rendered coding rules say the target locales come back from Crowdin and are never written by hand. Drafts written through the `translate` skill are the sanctioned exception: they go up to Crowdin as suggestions in the same step, so the loop still closes there. The rules template says so; nothing here changes it per project.
