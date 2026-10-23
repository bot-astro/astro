# Evals — what they do and how to run them

Most skills in this repository are evaluated with a declarative `evals.json`: a prompt, and a sentence describing what a good answer looks like. This skill can't be checked that way. It doesn't answer a question — it inspects a repository, writes files, and executes a plan — so the evals here run the skill against a throwaway copy of a real project and then check what it wrote.

That is the only reason there is shell here at all. Everything below is what those scripts actually do.

## Running them

```bash
# offline, ~1 minute each — planning only, no installs, no network
./skills/i18n-setup/evals/run-layer-a.sh vite-react-swc
./skills/i18n-setup/evals/run-layer-a.sh cra-react
./skills/i18n-setup/evals/run-layer-a.sh lingui-configured
./skills/i18n-setup/evals/run-layer-a.sh already-connected

# the full run: installs packages, wraps strings, needs the lingui plugin
./skills/i18n-setup/evals/run-layer-b.sh vite-react-swc

# no agent involved, runs in a second — checks the rules template's syntax
./skills/i18n-setup/evals/lint-rules-template.sh

# no agent involved, runs in a second — schema, paths, and cross-file invariants of manifest.json
./skills/i18n-setup/evals/verify-manifest.sh
```

Each prints `PASS: …` or `FAIL: …` per check and exits non-zero if anything failed. Add `KEEP_WORKDIR=1` before a Layer A command to keep the temporary directory and look at what the agent actually produced:

```bash
KEEP_WORKDIR=1 ./skills/i18n-setup/evals/run-layer-a.sh vite-react-swc
# → prints "workdir kept: /var/folders/…/tmp.XXXX"
```

Nothing here touches this repository or any project of yours: every run happens in a `mktemp -d` directory that is deleted on exit.

## What each layer proves

**Layer A — planning, offline.** Copies a fixture project into a temp directory, copies this skill in beside it, and runs the agent with every question pre-answered so it never stops to ask. The agent is told to stop once `plan.md` is written. Then the verifier compares two artifacts against the expected files in `expectations/`:

- `detection.json` — every field the golden names must match exactly. Fields the golden doesn't name are ignored, so a fixture can grow without breaking the check.
- `plan.md` — every step id in `planStepsContain` must appear as a `- [ ] step_id` line, every id in `planStepsAbsent` must not, and any phase heading in `phasesAbsent` must be missing entirely. That last one is how connect-only is checked: phases 3 and 4 aren't there at all.

The `cra-react` fixture is the opposite test. It expects a **refusal**, and the verifier asserts that the run wrote `detection.json`, wrote no `plan.md` and no `crowdin.yml`, left `package.json` byte-identical, and said the words "Create React App". A skill that helpfully proceeds anyway fails here.

**Layer B — a real run.** Everything Layer A does, then lets the agent actually execute the plan: install packages, wrap strings, extract. It needs the `lingui` plugin available (it shallow-clones `lingui/skills` if you don't point `LINGUI_SKILLS_DIR` at a checkout). It finishes by running `library-checks/lingui.sh`, which is a list of eight things that must be true on disk afterwards — config present, catalogs per locale, extraction clean, type-check green, `crowdin.yml` shaped correctly, `.po` files not gitignored.

Layer B needs no Crowdin credentials. Its two credentialed checks — `crowdin config sources` and `crowdin config translations`, both of which reach a real project — are skipped with a WARN unless you set `CROWDIN_EVAL_PROJECT_ID` to a project you own.

## The files

| File | What it is |
|---|---|
| `fixtures.json` | The list of test projects. Each names its category, where it lives, and which expectation files apply. |
| `fixtures/` | Real project trees. `vite-react-swc` is BrewLog, a small React app with hardcoded strings; `hard-stop/cra-react` is a Create React App project. |
| `fixtures/overlays/` | Files layered on top of a base fixture to make a variant — `lingui-configured` turns BrewLog into an already-internationalized project, `already-connected` adds a `crowdin.yml` and a workflow on top of that. |
| `expectations/` | The goldens: what `detection.json` must contain, which plan steps must and must not appear, and what the CRA refusal must look like. |
| `run-layer-a.sh` | The whole Layer A story in one file: assemble the temp workspace, run the agent with a pre-answered prompt, then verify — plan fixtures against the goldens, `cra-react` against the refusal contract. |
| `run-layer-b.sh` | The same setup, but lets the plan execute, then runs the library checks. |
| `helpers/prepare-workdir.sh` | Copies a fixture (plus overlays, for derived ones) into the temp directory. |
| `library-checks/lingui.sh` | The eight on-disk checks after a Layer B run. |
| `lint-rules-template.sh` | Checks `rules.template.md` against the template format — declared conditions and values match what the body uses, no nesting, nothing unclosed. Needs no agent. |
| `verify-manifest.sh` | Static verifier for `manifest.json`: JSON-Schema validation (when `jsonschema` is installed), every path-shaped string resolves on disk, and the cross-file invariants a schema can't express — unique variants and catalog ids, every stack's `catalog` resolving, `%locale%` in every translation pattern and in no source path, install commands present wherever a role list names real skills, and every plan golden's `variant` existing in the manifest. Needs no agent. |
| `prefills/` | A hand-authored `.crowdin/` workspace, so Layer B can start from phase 3 without spending a Layer A run first. |

## Adding a fixture

1. Put the project tree under `fixtures/`, or an overlay under `fixtures/overlays/` if it's a variant of one that exists.
2. Add an entry to `fixtures.json` naming its `category` — `positive`, `hard-stop`, `connect-only`, or `collapse` — and the expectation files.
3. Write those expectation files under `expectations/`.
4. Run it. The first run of a new fixture is expected to fail; that failure is what tells you the golden and the skill disagree, which is the point.

## When something fails

The failure line names the artifact and the mismatch — `FAIL: detection framework: want "vite", got "unknown"`, or `FAIL: plan missing step extract_smoke`. Re-run with `KEEP_WORKDIR=1` and read `.crowdin/plan.md` and `.eval-agent-output.txt` in the kept directory: the second is the agent's own transcript, which usually says plainly why it did what it did.

A `FAIL` means the skill and the golden disagree. Which one is wrong is a judgment call — a golden that encodes an old step vocabulary is just as likely to be the stale one as the skill is.
