# Prefill provenance

Hand-authored, not captured from a live `[maintainer]` Layer A run (`cp -R <workdir>/.crowdin/. ...` per the Task 11 brief) — implementers spawn live agent runs rather than run `[maintainer]` steps, and a live-agent artifact would make Layer B non-deterministic anyway. Built deterministically from sources already committed in this repo:

- `evals/expectations/detection/vite-react-swc.json` — `detection.json`'s matched fields (`language`, `framework`, `compiler`, `react`, `typescript`, `packageManager`, `existing`).
- `evals/expectations/plan/vite-react-swc.json` — the step ids `plan.md` must contain (`planStepsContain`) and the `variant` `manifest-snapshot.json` must name.
- `manifest.json` — `manifest-snapshot.json` is the `js-ts-lingui` stack entry verbatim plus the `lingui-po` catalog row it names.
- `SKILL.md` phases 1 and 2 — the `detection.json` schema, the `decisions.md` content (entry point, locales, library, edition, scope, branch, mode, add-ons, skill-availability probe), and the `plan.md` checklist format (`- [ ] step_id — detail`, grouped under `## Phase N — Name` headings).
- `evals/fixtures/vite-react-swc/` — the actual candidate files/paths, and `evals/fixtures/overlays/lingui-configured/` (the already-converted end-state fixture used elsewhere in this eval suite) — cross-checked to ground the per-file candidate counts in `detection.json`/`plan.md` against real `msgid` entries rather than guessing them.

## Refresh trigger

**Refresh this prefill whenever plan step ids change** — i.e. whenever `SKILL.md`'s phase-2 step vocabulary (the "Shared by every stack" / "Added by model" tables) or `evals/expectations/plan/vite-react-swc.json`'s `planStepsContain` list changes. Regenerating means: re-derive `plan.md` so every asserted step id still appears as a `- [ ] step_id` checklist line (the form `run-layer-a.sh`'s verifier matches), and re-check `detection.json` / `manifest-snapshot.json` against their sources above for drift.

This file is copied into the target workdir's `.crowdin/` alongside the four schema files when `run-layer-b.sh` runs — harmless, since nothing reads `.crowdin/` exhaustively.
