# Screenshots for translators

Reached from `SKILL.md`'s Phase 6 — Context, for the `generate_screenshots` step of `plan.md` — a step the plan carries only when the user chose the screenshots add-on at Decide. The step is one delegation: the **`generate-screenshots`** skill plans the screens with the user, writes a capture definition into the project, captures the running app, and uploads the images. This file is the handoff — what to pass it, what it needs from the user, and what to record when it returns.

## Delegate

Confirm the `generate-screenshots` skill is loadable first. It ships alongside this one, so a run that installed this skill as a Claude Code plugin already has it; otherwise the fix is one install command (`npx skills add crowdin/skills --skill generate-screenshots`). If it is missing, ask the user to install it and wait — do not improvise screenshot capture or `crowdin screenshot` calls in its place; phase 3's dependency-gate reasoning carries over unchanged.

Then invoke it under its own invoked-by-another-workflow contract, passing what this journey already knows so it asks the user nothing twice:

| Input | Value | Source |
|---|---|---|
| Connected project | `crowdin.yml` written and verified by phase 5's gates — the delegate does no connection work | phase 5 |
| Capture locale | the source locale | `decisions.md` |
| Workspace | `.crowdin/` already exists and is gitignored; the delegate keeps its batch folder at `.crowdin/screenshots/` | phase 1 |
| Stack | `detection.json`'s framework and dev-server command, so the delegate's engine choice and `webServer` need no second detection | phase 1 |

What the delegate owns is not this journey's to shortcut: the engine choice and its install consent, the publish-only fallback when no engine can run, and the user's review of the screen plan before anything is written or captured — this journey never waives that gate on the user's behalf.

## First-run expectations

The project was connected minutes ago, so the delegate's coverage query (`count of screenshots = 0`) matches every uploaded string. That is the expected starting point, not a sign something failed — say so when handing over.

The step also needs the user at the keyboard — they start the app, or let the engine start it, and they sign in once into the session the engine reuses — which is why it runs last in the phase. Like `prove_round_trip`, it is a step to ask for and wait on, never to report as done.

## What comes back

- **Completed** — the capture definition committed (its path, and the two commands that re-run it), images uploaded, coverage reported before → after. Record the definition's path in `decisions.md` and tick the step. The commit is the delegate's: it lands on the branch `decisions.md` names, like every other phase's files.
- **Declined or deferred** — the user chose not to capture now; the standalone skill runs the same way any day later, so deferring costs nothing. Note the decision in `decisions.md` and tick the step: a declined capture is a decision, not a failure.
- **Blocked** — the app would not start, the session was signed out, or no engine could run and the user did not want publish-only mode. Leave the step unticked and say what is missing; nothing uploads from a broken session.
