---
name: crowdin-cli
description: Guides correct usage of Crowdin CLI - the `crowdin` command that syncs localization files between a local project and Crowdin. Use whenever the user runs, scripts, or debugs `crowdin` commands, authorizes the CLI, creates or edits a crowdin.yml configuration, uploads sources, downloads translations, checks translation status, auto-translates a project, wires localization into CI/CD, or migrates scripts from CLI v4 to v5 - even if they just say "sync translations" or "push strings to Crowdin" without naming the CLI.
---

# Crowdin CLI

Crowdin CLI is the command-line tool for managing and synchronizing localization resources with a Crowdin project. It ships as a single self-contained binary — no Java or other runtime required.

Check what's installed with `crowdin --version`. If it prints `4.x` or a Java error, the user is on the legacy CLI — commands below still mostly apply, but see [references/migrating-from-v4.md](references/migrating-from-v4.md) for the differences.

Full documentation: https://crowdin.github.io/crowdin-cli

## Install

```bash
npm install -g @crowdin/cli
```

Also available via Homebrew (`brew tap crowdin/crowdin && brew install crowdin@5`), WinGet, Chocolatey, Docker (`crowdin/cli`), Linux package repositories, and as a standalone binary — see [references/configuration.md](references/configuration.md#installation) for every option.

## Authenticate

The CLI needs an **API token** and (for most commands) a **project ID**. On a developer's machine the quickest route is:

```bash
crowdin login
```

It authorizes in the browser and saves the token to the identity file `~/.crowdin.yml`, where every later command — run by the user or by an agent — picks it up. No configuration file is generated or required, and Crowdin Enterprise needs nothing extra: the organization comes from the account signed in. `crowdin init` runs the same browser authorization and then generates a `crowdin.yml`; `login` is the choice when the config is written by hand or already exists. The flow finishes through a browser callback on the same machine, so when the agent's shell may be elsewhere, the user runs it in their own terminal. The browser-issued token expires after 30 days — re-run `crowdin login` when a command exits `101`. Details: [references/commands.md](references/commands.md#login).

For CI and anything long-lived, use a **personal access token** — it does not expire:

- crowdin.com: create the token in **Settings → API** (`https://crowdin.com/settings#api-key`). `base_url` is `https://api.crowdin.com` (the default — no need to set it).
- Crowdin Enterprise: create it in **Account Settings → Access Tokens**. `base_url` must be set to `https://{organization}.api.crowdin.com`.

The project ID is numeric — get it with `crowdin project list` once a token is configured (`project list` alone needs `-i 1` while no `crowdin.yml` carries a `project_id`: validation demands one even though this command ignores it), or from the project page in Crowdin.

Credentials resolve in this priority order (highest wins):

1. CLI flags: `-T/--token`, `-i/--project-id`, `--base-url`, `--base-path`
2. Identity file: `--identity <path>`, or `~/.crowdin.yml` / `~/.crowdin.yaml` if present
3. `*_env` keys in the config file (`api_token_env: MY_VAR` reads `$MY_VAR`)
4. Literal keys in the config file (`api_token: "..."`)
5. `CROWDIN_PERSONAL_TOKEN`, `CROWDIN_PROJECT_ID`, `CROWDIN_BASE_PATH`, `CROWDIN_BASE_URL` environment variables

`.env` files in the working directory are loaded automatically, so `CROWDIN_PERSONAL_TOKEN` can live there. **Never write a real token into `crowdin.yml`** if the file is committed — use `api_token_env` or the `CROWDIN_PERSONAL_TOKEN` variable instead. A committed `api_token_env` and a local `crowdin login` coexist: the unset variable contributes nothing, so the identity file supplies the token on the developer's machine and the variable supplies it in CI.

## Configure — crowdin.yml

Commands read `crowdin.yml` (or `crowdin.yaml`) from the current directory; override with `-c/--config <path>`. Minimal real-world example:

```yaml
"project_id": "123456"
"api_token_env": "CROWDIN_PERSONAL_TOKEN"
"base_path": "."
"preserve_hierarchy": true

files:
  - source: "/src/locales/en/**/*.json"
    translation: "/src/locales/%two_letters_code%/**/%original_file_name%"
```

- `source` is a glob for local source files (relative to `base_path`); `translation` tells Crowdin where exported translations land, using placeholders like `%two_letters_code%`, `%locale%`, `%original_file_name%`, `%original_path%`, `%file_name%`.
- `preserve_hierarchy: true` keeps the local directory structure in Crowdin; without it, the common parent directory is stripped.
- One `files` entry per file group; each entry can add `ignore`, `dest`, `type`, `update_option`, `labels`, `excluded_target_languages`, `languages_mapping`, and more.

The full reference — all placeholders, per-entry options, language mapping, multiple file groups — is in [references/configuration.md](references/configuration.md).

Before the first upload, verify the config instead of debugging a wrong upload after the fact:

```bash
crowdin config lint            # validate syntax
crowdin config sources         # list local files matched by each source pattern
crowdin config translations    # list translation paths that will be produced
crowdin upload sources --dryrun
```

## Core workflow

The everyday cycle is: upload sources → translate → download translations.

```bash
crowdin upload sources           # push local source files (alias: crowdin push)
crowdin status                   # translation & proofreading progress per language
crowdin auto-translate --method tm   # optional: pre-fill via TM, MT, or AI
crowdin download translations    # pull completed translations (alias: crowdin pull)
```

Useful variants:

```bash
crowdin upload sources --cache               # skip unchanged files (checksum cache)
crowdin upload sources --delete-obsolete     # remove files from Crowdin that no longer exist locally
crowdin upload translations                  # import existing local translations
crowdin download translations -l uk -l fr    # only some languages
crowdin download translations --skip-untranslated-strings
crowdin download sources                     # pull sources back from Crowdin
```

Everything is branch-aware: pass `-b <name>` to scope upload/download/status to a Crowdin branch (create it with `crowdin branch add`).

`auto-translate` (v4's `pre-translate`) applies TM (`--method tm`), machine translation (`--method mt --engine-id <id>`), or AI (`--method ai --ai-prompt <id>`; `crowdin ai-prompt list --action pre_translate --assigned` lists the prompt IDs), with fine-grained control: `--scope untranslated|translated|all`, `--skip-approved-translations`, `--label`/`--exclude-label`, `--replace-translations-option`, `--translation-modified-before`, and more — see [references/commands.md](references/commands.md#auto-translate). It waits for the job to finish; `--async` returns the job identifier at once, for `crowdin auto-translate status <id> [--wait]` to check later.

## Integrations through Crowdin apps

When the content lives in a CMS, marketing tool, help center or other platform connected through an installed Crowdin app, the sync runs through the app's API methods (`crowdin app methods`, `crowdin app api`), not `upload`/`download`. That loop, on this command line and through the Crowdin connector, is the `integration-sync` skill: load it before the first `crowdin app api` call. It has the import, job and push steps, how to pass the app's IDs, and which calls replace settings.

## Strings context for AI enrichment

Enriching a project's strings context (download, fill `ai_context`, upload) is the [context-extraction](../context-extraction/SKILL.md) skill's loop. The `crowdin context` commands and the JSONL format: [references/context.md](references/context.md); its `--status` filter does not mean "needs context".

## Output for scripts and agents

The global `-o/--output` option changes any command's output format:

- `-o json` — machine-readable JSON; pipe into `jq` or parse from a script
- `-o toon` — [Token-Oriented Object Notation](https://github.com/toon-format/toon), same data as JSON at a fraction of the size; **prefer this when the consumer is an LLM**
- `-o plain` — minimal processable text (v4's `--plain` flag)

In `json` and `toon` modes stdout carries nothing but data — no spinners, colors, or decorative messages — so output is safe to parse. When running the CLI as an agent, default to `-o toon` for lists you only need to read and `-o json` when a script must parse the result.

```bash
crowdin file list -o json | jq -r '.[].path'
crowdin status -o toon
```

Exit codes are stable and meaningful — branch on them rather than parsing error text:

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | General error |
| 2 | Validation error (bad config or arguments) |
| 101 | Authorization error (bad/expired token) |
| 102 | Not found |
| 103 | Forbidden |
| 129 | Rate limit exceeded |

Mutating file commands accept `--dryrun` to preview what would happen. Add `-v/--verbose` when diagnosing; `--no-progress`/`--no-colors` clean up logs in CI (json/toon modes already imply this).

## Command map

24 top-level commands; most have subcommands. Full option-level reference: [references/commands.md](references/commands.md).

| Command | Purpose |
|---------|---------|
| `upload` (`push`) | Upload sources / translations per crowdin.yml |
| `download` (`pull`) | Download translations / sources per crowdin.yml |
| `init` | Generate crowdin.yml interactively |
| `login` | Authorize in the browser and save the token to `~/.crowdin.yml` |
| `status` | Translation & proofreading progress |
| `auto-translate` | Pre-fill translations via TM / MT / AI (status, list) |
| `file` | Direct file ops without config patterns (list, upload, download, delete) |
| `string` | Manage source strings (list, add, edit, delete; CroQL filters) |
| `branch` | Manage branches (add, list, clone, merge, edit, delete) |
| `task` | Create and list translation/proofreading tasks |
| `tm` | Translation memories (list, upload, download; `list --assigned` scopes to the project) |
| `glossary` | Glossaries (list, upload, download; `list --assigned` scopes to the project) |
| `style-guide` | Style guides (list, download, upload, delete) |
| `ai-prompt` | AI prompts (list; `--action pre_translate --assigned` finds the IDs `auto-translate --method ai` takes) |
| `bundle` | Export bundles (list, add, download, clone, browse) |
| `distribution` | Content distributions (add, list, edit, release) |
| `screenshot` | Screenshots for context (list, upload a file or a directory, delete) |
| `comment` | String comments and issues (list, add, resolve) |
| `label` | Labels (list, add, delete) |
| `language` | List project/supported languages; `--verbose` adds text direction and plural categories |
| `project` | List, add, browse projects |
| `config` | Validate config, preview matched sources/translations |
| `app` | Installed apps: list, install, uninstall, and call their API methods (the import, push and jobs loop is `integration-sync`) |
| `context` | AI context for strings: download, upload, coverage status, reset — see [references/context.md](references/context.md) |

Shell completion: `source <(crowdin complete zsh)` (also `bash`, `fish`, `powershell`).

## CI/CD

- Store the token as a CI secret exposed as `CROWDIN_PERSONAL_TOKEN`; keep `project_id` in the committed `crowdin.yml` (it is not sensitive).
- Typical pipeline: `crowdin upload sources` on merge to the main branch; `crowdin download translations` on a schedule or before release, followed by a commit/PR of the updated files.
- For GitHub Actions prefer the official [crowdin/github-action](https://github.com/crowdin/github-action), which wraps this CLI — covered by the [github-action](../github-action/SKILL.md) skill.
- The CLI never prompts when all required values are provided; a missing value fails with exit code 2 rather than hanging.
- `crowdin status --fail-if-incomplete` exits non-zero when the project isn't fully translated — a ready-made release gate.

## Related skills

- [context-extraction](../context-extraction/SKILL.md) — writes the `ai_context` values that `crowdin context upload` sends
- [croql](../croql/SKILL.md) — CroQL expressions for `--croql` filters on string commands
- [crowdin-api-client](../crowdin-api-client/SKILL.md) — the JS/TS API client, when a CLI command doesn't cover the need
- [translate](../translate/SKILL.md) — drafts translations into local resource files with the project's glossary, TM and style guide, and uploads them as suggestions
