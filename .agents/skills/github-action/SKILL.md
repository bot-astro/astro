---
name: github-action
description: Sets up and debugs the Crowdin GitHub Action (crowdin/github-action), which syncs a repository with a Crowdin project and opens the translation pull request. Use whenever the user wires Crowdin into GitHub Actions, writes or reviews a .github/workflows/crowdin.yml step, tunes its inputs (upload_sources, download_translations, localization_branch_name, create_pull_request, command), or debugs a run that opened no pull request, opened one that gets no CI checks, or hit a 409 while a build was in progress - including bare phrasings like "automate translations in CI" or "my Crowdin workflow is broken".
---

# Crowdin GitHub Action

`crowdin/github-action` runs [Crowdin CLI](https://crowdin.github.io/crowdin-cli) inside a Docker container as a single workflow step: it uploads source files to Crowdin, downloads finished translations, commits them to a localization branch, and opens a pull request.

Because it is a Docker action it only runs on **Linux** runners — `ubuntu-latest`, or a self-hosted Linux runner with Docker available. It will not run on `macos-*` or `windows-*`.

Reference: [README](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/README.md) · [examples](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/docs/EXAMPLES.md) · [inputs and outputs](references/inputs.md) · [workflow recipes](references/recipes.md)

## The step is a pipeline, not a command

One step performs up to five operations, always in this fixed order, and each one is gated by its own input:

| Order | Input | Runs |
|---|---|---|
| 1 | `upload_sources: true` *(default)* | `crowdin upload sources` |
| 2 | `upload_translations: true` | `crowdin upload translations` |
| 3 | `download_sources: true` | `crowdin download sources`, then the **git half** |
| 4 | `download_translations: true` | `crowdin download`, then the **git half** |
| 5 | `download_bundle: <id>` | `crowdin bundle download <id>`, then the **git half** |

The **git half** is a single unit of work: `git add .`, commit, force-push the localization branch, open the PR. Its parts are individually switchable (`push_translations`, `push_sources`, `create_pull_request`), and only rows 3–5 can reach it.

That order explains nearly every surprise the action produces:

- **`upload_sources` defaults to `true`.** A step you intended as download-only also pushes your sources to Crowdin unless you write `upload_sources: false`. Set every flag you care about explicitly rather than relying on defaults.
- **The git half runs only after a download.** `create_pull_request: true` on an upload-only step is inert — there is nothing to commit.
- **Two downloads in one step run the git half twice** — two commits to the same localization branch, under one PR.
- **The git half commits with `git add .`** — everything dirty in the workspace goes in, including files earlier steps generated. Keep the Crowdin step ahead of any build steps, or give it its own job.
- **Every run with changes force-pushes**, so an open translation PR is rewritten rather than appended to — which is why its checks start over each time.

## Setting it up

Two of the five steps belong to a human: the repository secrets in step 2 and the pull-request setting in step 4 need someone with repository admin access clicking through the GitHub UI. Hand that list over explicitly rather than assuming it is already in place — a workflow that is perfect on disk still fails its first run without them.

### 1. Configure `crowdin.yml`

The action reads the same `crowdin.yml` as the CLI — the [crowdin-cli](../crowdin-cli/SKILL.md) skill covers `files`, placeholders, and language mapping. Three parts matter specifically here:

```yaml
"project_id_env": "CROWDIN_PROJECT_ID"
"api_token_env": "CROWDIN_PERSONAL_TOKEN"
"preserve_hierarchy": true

files:
  - source: "/src/locales/en/**/*.json"
    translation: "/src/locales/%two_letters_code%/**/%original_file_name%"
```

- `project_id_env` / `api_token_env` point at the environment variables the workflow supplies, so no credential is ever committed.
- `preserve_hierarchy: true` keeps Crowdin's file tree identical to the repository's. Without it Crowdin strips the common parent directory, and downloaded translations land in paths that don't match the repo — the usual cause of a PR full of files in the wrong place. It is also required when moving off the native GitHub (OAuth) integration.
- Keep the file at the repository root, or pass its path via the `config` input (no leading `/`).

Repositories with a single source and translation pattern can skip the file entirely and pass `source` and `translation` as inputs instead — see [no-`crowdin.yml`](references/recipes.md#no-crowdinyml-configuration). Pick one or the other: a `crowdin.yml` in the repository conflicts with those inputs.

### 2. Create the repository secrets

In **Settings → Secrets and variables → Actions**, add:

- `CROWDIN_PROJECT_ID` — the numeric project ID, from **Tools → API** in the Crowdin project.
- `CROWDIN_PERSONAL_TOKEN` — a Crowdin personal access token ([crowdin.com](https://crowdin.com/settings#api-key), or **Account Settings → Access Tokens** in Enterprise).

Scope the token to what the workflow actually does — for a normal sync that is Projects → Read, Translation Status → Read, Source files & strings → Read and Write, Translations → Read and Write.

### 3. Write the workflow

`.github/workflows/crowdin.yml`:

```yaml
name: Crowdin

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: write
  pull-requests: write

concurrency:
  group: crowdin
  cancel-in-progress: false

jobs:
  synchronize:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7

      - name: Synchronize with Crowdin
        uses: crowdin/github-action@v3
        with:
          upload_sources: true
          upload_translations: false
          download_translations: true

          localization_branch_name: l10n_crowdin
          create_pull_request: true
          pull_request_title: 'New Crowdin translations'
          pull_request_base_branch_name: main
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

Details worth keeping when you adapt it:

- Credentials go in `env:`, not `with:` — the CLI and the action's git code read them from the environment. (`token` and `project_id` inputs exist as an alternative, but a token in `with:` shows up in the step's rendered inputs.)
- `workflow_dispatch` is what lets you test the workflow without waiting for a push.
- `concurrency` with `cancel-in-progress: false` queues runs instead of racing them. A second run that starts while Crowdin is still building the previous export fails with a 409.
- `pull_request_base_branch_name` is worth setting explicitly: unset, the base branch is whatever ref triggered the run, which is correct for a push to `main` but broken for tag and release triggers.
- A single sync step is the simplest shape, but the two directions have different natural rhythms — uploading on every push to `main`, downloading on a schedule. See [split workflows](references/recipes.md#split-upload-and-download).
- `actions/checkout@v7` was current in August 2026; check its releases rather than copying a pin from an older example.

### 4. Grant the token write access

Pushing a branch and opening a PR needs `contents: write` and `pull-requests: write`. With the default `GITHUB_TOKEN` that means both the job-level `permissions:` block above **and** the repository setting **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**. Without the setting, PR creation fails even though the permissions look right.

The default token has one limitation no permission fixes: **pull requests it opens do not trigger workflows**, so the translation PR arrives with no CI checks. If checks must run on it, authenticate as something other than the Actions bot — a [GitHub App](references/recipes.md#github-app-authentication) (scoped, auto-rotating, the better default) or a classic PAT with the `repo` scope, passed as `GH_TOKEN` or `GITHUB_TOKEN`.

### 5. Prove it runs

Valid YAML is not the finish line. This action fails at runtime — on credentials, file patterns and permissions — so the work is done when a run has printed the stages you expected, not when the file is written:

1. Set `dryrun_action: true` and dispatch the workflow. The CLI prints the files it would manage, so wrong `source` patterns surface here, before anything reaches Crowdin.
2. Drop the flag, dispatch again, and read the log through to the end: `UPLOAD SOURCES`, then `DOWNLOAD TRANSLATIONS`, then either a PR URL or `NOTHING TO COMMIT`.
3. Re-run a misbehaving run with **Enable debug logging** — the action detects `RUNNER_DEBUG`, dumps its environment, and adds `--verbose --debug` to every CLI call.

Dispatching needs the human who holds the secrets; ask for the run's log rather than stopping at the workflow file.

## What breaks, and why

Read the step's log first: it prints `UPLOAD SOURCES`, `DOWNLOAD TRANSLATIONS`, `PUSH TO BRANCH`, `NOTHING TO COMMIT`, and `CREATE PULL REQUEST` as it goes, so the pipeline stage that failed is visible before you start guessing.

| Symptom | Cause | Fix |
|---|---|---|
| Green run, no PR, log says `NOTHING TO COMMIT` | Downloaded translations are byte-identical to what's committed | Working as intended — nothing to do |
| Green run, no PR, no download in the log | `download_translations` left at its `false` default | Set `download_translations: true` |
| `ERROR: Either 'GITHUB_TOKEN' or 'GH_TOKEN' must be set` | Token missing from the step's `env:` | Add `GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}` |
| `FAILED TO AUTHENTICATE WITH GITHUB`, or 403 on push | Job lacks write permissions, or the repo blocks Actions from creating PRs | Add the `permissions:` block **and** enable the repository setting (step 4) |
| PR opens but no CI checks run on it | PRs from the default `GITHUB_TOKEN` don't trigger workflows | Use a GitHub App token or a PAT |
| Checks restart on every sync run | The localization branch is force-pushed each time | Pass a PAT to `actions/checkout` so the pushes come from a user |
| PR creation fails on a tag or release trigger | Base branch defaults to the triggering ref, which is `refs/tags/...` | Set `pull_request_base_branch_name` |
| Later steps hit permission errors on downloaded files | The container runs as root, so the files it writes are root-owned | Add `user: auto` to detect the workspace owner |
| `409` / build already in progress | Parallel runs against one Crowdin project | Serialize with `concurrency`, or `max-parallel: 1` in a matrix |
| Build artifacts in the translation PR | `git add .` commits the whole workspace | Move the Crowdin step before the build, or into its own job |
| Nothing downloads, project is string-based | `download_translations` doesn't apply to string-based projects | Download a bundle: `download_bundle: <id>` |
| `crowdin.yml` not found | The action looks in the repository root | `config: path/to/crowdin.yml`, no leading `/` |
| Enterprise: 404 or auth failure from the CLI | Enterprise needs an organization-specific API host | `base_url: 'https://{organization}.api.crowdin.com'` |

## `command` — running any CLI command

`command` (plus optional `command_args`) turns the step into an arbitrary CLI invocation: `status`, `auto-translate`, `bundle`, `string`, anything the CLI supports.

```yaml
- name: Fail the build unless translations are complete
  uses: crowdin/github-action@v3
  with:
    command: 'status translation'
    command_args: '--fail-if-incomplete'
  env:
    CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
    CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

Two consequences of how it is implemented, neither of them documented upstream:

- **`command` replaces the pipeline.** The step runs the command and exits — no upload, no download, no commit, no PR, whatever the other inputs say. Sequencing a command between an upload and a download takes three separate steps ([AI pre-translation](references/recipes.md#ai-pre-translation) is the canonical example).
- **The action's config inputs don't reach it.** `config`, `token`, `project_id`, `base_url`, `base_path`, `crowdin_branch_name`, and `dryrun_action` are not forwarded to a `command` step — only `command_args` is. Supply credentials through `env:` and put everything else in `command_args` (`--config path/to/crowdin.yml`, `--branch main`, `--dryrun`).

The command's stdout is available to later steps as the `command_output` output.

## Which version to pin

`@v3` is the current stable line and runs Crowdin CLI 5.

`@v2` is the legacy line, bundling the Java-based Crowdin CLI 4. Every input, output, and default is identical between the two, so upgrading a pipeline-style step is a one-line pin change; what changes is the CLI underneath, and therefore the strings you pass to `command` and `*_args`. Most notably v4's `pre-translate` became `auto-translate`, and `--plain` became `-o plain`; the [migration reference](../crowdin-cli/references/migrating-from-v4.md) lists the rest. Stay on `@v2` only while a `command` step still depends on v4-specific CLI syntax.

## Related skills

- [crowdin-cli](../crowdin-cli/SKILL.md) — `crowdin.yml` configuration and the commands and flags behind every input
- [crowdin-api-client](../crowdin-api-client/SKILL.md) — for automation the CLI doesn't cover
- [croql](../croql/SKILL.md) — CroQL filters for `command_args` on string commands
