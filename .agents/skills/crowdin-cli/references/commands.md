# Crowdin CLI — Command Reference

Condensed from the generated command documentation (https://crowdin.github.io/crowdin-cli/commands/crowdin). Synopsis: `crowdin [SUBCOMMAND] [OPTIONS]`.

## Contents

- [Common options](#common-options)
- [init](#init) · [login](#login)
- [upload (push)](#upload-alias-push) · [download (pull)](#download-alias-pull)
- [status](#status) · [auto-translate](#auto-translate)
- [file](#file) · [string](#string) · [branch](#branch)
- [task](#task) · [tm](#tm) · [glossary](#glossary) · [style-guide](#style-guide) · [ai-prompt](#ai-prompt)
- [bundle](#bundle) · [distribution](#distribution)
- [screenshot](#screenshot) · [comment](#comment) · [label](#label)
- [language](#language) · [project](#project) · [app](#app)
- [context](#context) · [config](#config) · [complete](#complete)
- [Aliases, renames, and scope notes](#aliases-renames-and-scope-notes)

## Common options

**Global options** — accepted by every command and subcommand:

| Option | Meaning |
|---|---|
| `-v, --verbose` | More information about command execution |
| `-c, --config <path>` | Path to the configuration file (default: `crowdin.yml` / `crowdin.yaml` in cwd) |
| `--identity <path>` | Path to user-specific credentials file |
| `--no-colors` | Disable colors and styles |
| `--no-progress` | Disable progress indicators |
| `-o, --output <fmt>` | Output format: `json`, `toon`, `plain` |
| `--debug` | Additional debugging information on errors (stack traces) |
| `-h, --help` | Help for the command |
| `-V, --version` | (root `crowdin` only) Print version and exit |

**Config option sets** — commands accept one of three tiers instead of repeating them below:

- **Set A (files tier)**: `-T/--token`, `--base-url`, `--base-path`, `-i/--project-id`, `-s/--source`, `-t/--translation`, `--dest`, `--preserve-hierarchy`, `--no-preserve-hierarchy`. Accepted by `upload*`, `download*`, `config lint/sources/translations`. `--source`/`--translation` must be passed as a pair and define a single file group that replaces the config's `files`; `--dest` requires both and implies `--preserve-hierarchy`. Without that pair a `crowdin.yml` must exist (exit 102 names the missing file).
- **Set B (project tier)**: `-T/--token`, `--base-url`, `--base-path`, `-i/--project-id`. Accepted by `status*`, `auto-translate*`, `file*`, `string*`, `task*`, `bundle*`, `branch*`, `comment*`, `distribution*`, `screenshot*`, `label*`, `language list`, `project*`, `context*`.
- **Set C (account tier)**: `-T/--token`, `--base-url`, `--base-path` (no project ID). Accepted by `tm*`, `glossary*`, `style-guide*`, `ai-prompt*` and `app*`; an app call names its project in the request (`projectId`).

Parent commands (`crowdin file`, `crowdin string`, …) only dispatch subcommands. `crowdin upload`, `crowdin download`, and `crowdin status` also run their default subcommand directly with the same options.

## init

```
crowdin init [options]
```

Generate the `crowdin.yml` configuration skeleton. Interactive by default (browser authorization → project selection → config written); note a browser-issued token expires after 30 days. The browser step times out after ~2 minutes, and the flow aborts with `No projects with manager access found` when the authorized account has no project with manager access. The credential is written to `~/.crowdin.yml` only at the very end, so an aborted init leaves nothing behind.

- `-d, --destination <path>` — where to save the skeleton (default: `crowdin.yml`)
- `-T, --token`, `-i, --project-id`, `--base-path`, `--base-url`, `-s, --source`, `-t, --translation` — pre-fill values instead of prompting
- `--no-preserve-hierarchy` — generate `preserve_hierarchy: false` (default generates `true`)
- `--quiet` — generate without interactive prompts

To authorize without generating a configuration file, run [`login`](#login) instead.

## login

```
crowdin login
```

Authorize the CLI in the browser and save the token to the identity file — the authorization half of `init` on its own, with no configuration file generated or read. No options beyond the global ones, no prompts.

- Opens the OAuth page on `accounts.crowdin.com`, waits for the callback on `localhost:46221`, and gives up after ~2 minutes. When no browser can be opened it prints the URL to open by hand — so the browser and the shell running `login` must be the same machine.
- Writes `~/.crowdin.yml`, merging `api_token` into whatever the file already holds; for a Crowdin Enterprise account it also writes `base_url`, with the organization taken from the sign-in. Always that default file — `--identity` does not redirect it.
- Text output ends with `Authorized as <username>` and the file path; `-o json`/`-o toon` return `username`, `id`, `baseUrl`, `identityFile`.
- The browser-issued token expires after 30 days: re-run `login`. CI and long-lived automation use a personal access token instead.

## upload (alias: push)

```
crowdin upload [sources|translations] [options]     # bare `upload` = `upload sources`
```

### upload sources

Upload source files per the `files` configuration. Config: set A.

- `-b, --branch <name>` — branch name
- `--label <label>` — attach labels to uploaded strings (repeatable)
- `--excluded-language <code>` — languages the sources should not be translated into (repeatable)
- `--delete-obsolete` — delete files/folders from Crowdin that no longer match the source config or were deleted locally
- `--no-auto-update` — only upload new files; do not update existing ones (updating is the default)
- `--cache` — checksum-cache sources and skip unchanged files (cache: `.crowdin/cache.json` relative to `base_path`)
- `--dryrun` [`--tree`] — preview without executing

### upload translations

Upload existing local translation files. Config: set A.

- `-b, --branch <name>` — branch name
- `-l, --language <code>` — a single target language (default: all)
- `--auto-approve-imported` — approve added translations automatically
- `--import-eq-suggestions` — add translation even if identical to the source string
- `--translate-hidden` — upload translations for hidden source strings
- `--dryrun` [`--tree`] — preview without executing

## download (alias: pull)

```
crowdin download [translations|sources] [options]   # bare `download` = `download translations`
```

### download translations

Build and download the latest translations. Config: set A.

- `-b, --branch <name>` — branch name
- `-l, --language <code>` — only these languages (repeatable; default all)
- `-e, --exclude-language <code>` — skip these languages (repeatable)
- `--pseudo` — download pseudo-localized files (see `pseudo_localization` config)
- `--skip-untranslated-strings` — omit untranslated strings from exports (not for document formats like .docx/.html/.md)
- `--skip-untranslated-files` — omit files that are not fully translated
- `--export-only-approved` — approved translations only (unapproved strings fall back to source text unless combined with `--skip-untranslated-strings`)
- `--keep-archive` — keep the downloaded archive after extraction
- `--all` — download even when local source files are missing
- `--ignore-match` — suppress the configuration-change warning
- `--dryrun` [`--tree`] — preview without executing

### download sources

Download source files from Crowdin. Config: set A. Mirrors Crowdin's file structure locally — `preserve_hierarchy: true` is strongly recommended.

- `-b, --branch <name>` — branch name
- `--reviewed` — only reviewed sources (Crowdin Enterprise only)
- `--dryrun` — preview without executing

## status

```
crowdin status [translation|proofreading] [options]   # bare `status` = both
```

Show translation and proofreading progress. Config: set B.

- `-l, --language <code>` — a single language (default: all)
- `-b, --branch <name>` — branch name
- `-f, --file <path>` — source file path in Crowdin
- `-d, --directory <path>` — directory path in Crowdin
- `--fail-if-incomplete` — exit non-zero if the project is not fully translated (`translation`), approved (`proofreading`), or both (bare `status`); useful as a CI gate

## auto-translate

```
crowdin auto-translate [options]
```

Apply Translation Memory, Machine Translation, or AI to project strings. Renamed from v4's `pre-translate` — no alias. Config: set B; outside a project, `-i <id>` stands in for `crowdin.yml`.

- `--method <mt|tm|ai>` — auto-translation method
- `--engine-id <id>` — MT engine (for `mt`)
- `--ai-prompt <id>` — AI prompt (required for `ai`)
- `-l, --language <code>` / `-e, --exclude-language <code>` — target languages (repeatable; default all)
- `--file <path>` (repeatable) / `--directory <path>` / `-b, --branch <name>` — what to translate
- `--scope <untranslated|translated|all>` — which strings (default: `untranslated`)
- `--auto-approve-option <all|except-auto-substituted|perfect-match-only>` — auto-approve TM translations (default: none)
- `--duplicate-translations` / `--no-duplicate-translations` — add or skip when the same translation already exists
- `--skip-approved-translations` — leave strings with approved translations untouched
- `--priority <low|normal|high>` — queue priority (default: `normal`)
- `--translation-modified-before <datetime>` — only strings whose translations were modified before (e.g. `2024-01-01T00:00:00Z`)
- `--replace-translations-option <none|auto-translated|all>` — replace existing translations (default: `none`)
- `--reset-approval-status` — remove approval from replaced translations
- `--translate-with-perfect-match-only` / `--no-translate-with-perfect-match-only` — TM perfect-match behavior
- `--label <label>` / `--exclude-label <label>` — include/exclude by labels (repeatable)
- `--source-language <code>` — translate from a specific source language
- `--async` — start the job and exit without waiting; prints the job identifier (alone with `-o plain`; `identifier` and `status` in json/toon)

```bash
crowdin auto-translate -l fr -l uk --method tm --file src/values/strings.xml
crowdin auto-translate -l fr --method mt --engine-id 5 --file src/values/strings.xml
```

Without `--async` the command blocks until the job finishes, which on a large project or with `--method ai` can outlast an agent's shell timeout. Start long runs with `--async` and check on them:

- `crowdin auto-translate status <id>` — status and progress; `--wait` blocks until the job finishes; `-v` adds the report totals (files, phrases, words, skipped) once it has. Exits 1 when the job failed or was canceled. Config: set B.
- `crowdin auto-translate list` — the project's auto-translation jobs with status, progress, method and timestamps; `-v` adds languages and file or branch counts. Config: set B.

```bash
id=$(crowdin auto-translate -l fr --method ai --ai-prompt 7 --async -o plain)
crowdin auto-translate status "$id" --wait
```

**AI prompts.** `crowdin ai-prompt list --action pre_translate --assigned` lists the prompts `--ai-prompt` takes for this project (see [ai-prompt](#ai-prompt)). Before the run, tell the user the prompt and target languages it will use; when more than one `pre_translate` prompt fits, ask which.

## file

Direct file operations, no `files` patterns needed. Config: set B.

- `crowdin file list [-b <branch>] [--tree]` — list project source files
- `crowdin file upload <file>` — upload one file:
  `-b/--branch`, `-l/--language` (for translation files), `--xliff` (file for offline translation), `--no-auto-update` (don't update existing), `--label` (repeatable), `-d/--dest <path>` (destination in Crowdin), `--context <text>`, `--type <type>` + `--parser-version <n>`, `--excluded-language <code>` (repeatable), `--cleanup-mode` (string-based projects: delete strings absent from the file), `--update-strings` (string-based projects: update strings with same keys)
- `crowdin file download <file> [-d <dest>] [-l <language>] [-b <branch>]` — download one file (source, or translation with `-l`); a translation comes back whole, untranslated and unapproved strings included, whatever the project's export settings
- `crowdin file delete <file> [-b <branch>]` — delete a file

## string

Manage source strings. Config: set B.

- `crowdin string list` — `--file <path>` (repeatable), `--filter <text>` (by identifier/text/context), `--scope <fields>` (fields to filter on), `-b/--branch`, `--directory <path>` (can't be used together with file or branch), `--label` (repeatable), `--croql <expr>` (cannot combine with other filters)
- `crowdin string add <text>` — `--identifier <key>`, `--file <path>` (repeatable), `--context <text>`, `--max-length <n>`, `--label` (repeatable), `-b/--branch`, `--hidden`, plural forms: `--one`, `--two`, `--few`, `--many`, `--zero` (the `<text>` argument is the `other` form)
- `crowdin string edit <id>` — `--text`, `--identifier`, `--context`, `--max-length`, `--label` (repeatable), `--hidden` / `--no-hidden` (both exist; they trigger different actions)
- `crowdin string delete <id>`

CroQL quoting per shell — Bash: single quotes `--croql 'identifier CONTAINS "label"'`; PowerShell: escape `"` with backticks; cmd.exe: double the quotes. Or put arguments in a file and pass `@args.txt`.

## branch

Manage Crowdin project branches. Config: set B.

- `crowdin branch list`
- `crowdin branch add <name>` — `--title <text>` (details for translators), `--export-pattern <pattern>`, `--priority <low|normal|high>`
- `crowdin branch edit <name>` — `--name` (rename), `--title`, `--priority`
- `crowdin branch clone <source> <target>` — string-based projects only
- `crowdin branch merge <source> <target>` — `--dryrun`, `--delete-after-merge`; string-based projects only
- `crowdin branch delete <name>`

## task

Manage translation/proofreading tasks. Config: set B.

- `crowdin task list` — `--status <todo|in_progress|done|closed>`, `--assignee-id <id>`
- `crowdin task add <title>` — `--type <translate|proofread>`, `--language <code>`, `--file <path>` (repeatable), `-b/--branch`, `--workflow-step <id>` (Enterprise only), `--description <text>`, `--skip-assigned-strings`, `--include-pre-translated-strings-only`, `--label <id>` (repeatable)

## tm

Manage translation memories. Config: set C (account-level, no project ID).

- `crowdin tm list` — `--assigned` (only the TMs assigned to the project in `crowdin.yml`; needs `project_id`; without it the list is account-wide)
- `crowdin tm upload <file>` — `--id <id>` (existing TM), `--language <code>`, `--scheme <scheme...>` (required for CSV/XLSX; constants: `{language_code}`, `{column_number}` from 0), `--first-line-contains-header`
- `crowdin tm download <id>` — `--source-language-id <code>`, `--target-language-id <code>`, `--format <tmx|csv|xlsx>`, `--to <path>`

## glossary

Manage glossaries. Config: set C.

- `crowdin glossary list` — `--assigned` (only the glossaries assigned to the project in `crowdin.yml`; needs `project_id`; without it the list is account-wide)
- `crowdin glossary upload <file>` — `--id <id>` (import into an existing glossary; omitting it **creates a new one**, which makes `--language <code>` — the glossary's source language, a Crowdin language id — required), `--scheme <scheme...>` (mandatory for CSV/XLSX, rejected for TBX; maps column names to zero-based indexes, e.g. `term_en=0,description_en=1`; constants: `term_{language_code}`, `description_{language_code}`, `partOfSpeech_{language_code}`, `{column_number}`), `--first-line-contains-header` (CSV/XLSX; keeps the header row from importing as a term). Success prints `Imported in #<id> '<name>' glossary`.
- `crowdin glossary download <id>` — `--format <tbx|csv|xlsx>`, `--to <path>`

## style-guide

Manage style guides. Config: set C.

- `crowdin style-guide list` — `--assigned` (only the style guides assigned to the project; needs `project_id`), `-v/--verbose` (adds `projectIds` and `languageIds` — comma-joined; an empty `languageIds` means the guide applies to every language — and whether AI instructions exist)
- `crowdin style-guide download <id>` — `--to <path>` (default: the guide's name plus the original file's extension)
- `crowdin style-guide upload <file>` — `--id <id>` (replace the file of an existing guide), `--name <text>` (default: file name without extension), `--project <id>` (repeatable), `--language <code>` (repeatable), `--shared` (all projects)
- `crowdin style-guide delete <id>`

## ai-prompt

List AI prompts. Config: set C.

- `crowdin ai-prompt list` — `--action <pre_translate|alignment|qa_check>` (only prompts for that action), `--assigned` (only the prompts the project in `crowdin.yml` may use; needs `project_id`; without it the list is every prompt the account sees), `-v/--verbose` (adds the projects each prompt is enabled in — `all` in text, `null` in json — provider, model and last use). Text shows `#<id> <name> (<action>)` and marks disabled prompts; json/toon keys are `id`, `name`, `action`, `isEnabled`, plus `enabledProjectIds`, `aiProviderId`, `aiModelId`, `lastUsedAt` with `-v`.

## bundle

Manage export bundles. Config: set B.

- `crowdin bundle list`
- `crowdin bundle add <name>` — `--format <fmt>`, `--source-pattern` (repeatable), `--ignore-pattern` (repeatable), `--export-pattern` (bundle file naming), `--label <id>` (repeatable), `--include-source-language`, `--include-pseudo-language` (default: true for `add`), `--multilingual`
- `crowdin bundle clone <id>` — same options as `add` plus `--name`; original values used as the base (no `--include-pseudo-language` default)
- `crowdin bundle download <id>` — `--keep-archive`, `--dryrun`
- `crowdin bundle browse <id>` — open in browser
- `crowdin bundle delete <id>`

## distribution

Manage content distributions. Config: set B.

- `crowdin distribution list`
- `crowdin distribution add <name>` — `--bundle-id <id>` (repeatable)
- `crowdin distribution edit <hash>` — `--name`, `--bundle-id` (repeatable)
- `crowdin distribution release <hash>`

## screenshot

Manage screenshots that give translators context. Config: set B.

- `crowdin screenshot list` — `--string-id <id>` (repeatable), `--search <term>` (matches name, tagged strings, or file names), `--label` / `--exclude-label` (repeatable)
- `crowdin screenshot upload <path>` — `--auto-tag`, and with it: `-f/--file <path>`, `-b/--branch <name>`, `-d/--directory <path>`; `--label` (repeatable). An upsert keyed by file name: a screenshot with the same name is replaced. `<path>` can be a directory: every `jpeg`, `jpg`, `png` and `gif` under it, subdirectories included, hidden ones skipped, with the options applied to each. File names must be unique across the tree — two `login.png` in different folders abort the run before anything is uploaded. A failed image does not stop the rest; the command exits non-zero at the end. With `--auto-tag` the images go one at a time, since auto-tagging locks the project.
- `crowdin screenshot delete <id>`

## comment

Manage string comments and issues. Config: set B.

- `crowdin comment list` — `--string-id <id>`, `--type <comment|issue>`, `--issue-type <general_question|translation_mistake|context_request|source_mistake>`, `--status <resolved|unresolved>`
- `crowdin comment add <text>` — `--string-id <id>`, `-l/--language <code>`, `--type`, `--issue-type`
- `crowdin comment resolve <id>`

## label

Config: set B.

- `crowdin label list`
- `crowdin label add <title>`
- `crowdin label delete <title>`

## language

Config: set B.

- `crowdin language list` — `--code <id|two_letters_code|three_letters_code|locale|android_code|osx_code|osx_locale>` (output format, default `id`), `--all` (all supported languages, not just the project's), `-v/--verbose` (adds each language's text direction and plural categories; in `json`/`toon` output the keys are `textDirection` and `pluralCategoryNames`, the latter a comma-joined string such as `one,few,many,other`)

## project

Config: set B.

- `crowdin project list` — projects with manager access
- `crowdin project add <name>` — `-l/--language <code>` (repeatable), `--source-language <code>` (default `en`), `--public`, `--string-based`
- `crowdin project browse` — open the project in the browser

## app

Manage installed Crowdin apps and call their API methods. Config: set C.

- `crowdin app list` — `-v/--verbose` adds the base URL and the projects each app serves
- `crowdin app install <identifier>` — identifier from the Crowdin Store
- `crowdin app uninstall <identifier>` — `--force`
- `crowdin app methods <identifier> [path]` and `crowdin app api <identifier> <path>` — the loop is in the `integration-sync` skill

## context

AI context for strings: `crowdin context download`, `upload`, `status` and `reset`. Options, the JSONL format and the enrichment loop: [context.md](context.md).

## config

Validate configuration and preview pattern matching. Config: set A.

- `crowdin config lint` — analyze the configuration file for errors
- `crowdin config sources [--tree]` — list local files matching the `source` patterns. Fetches project info before listing, so it needs a valid token and `project_id` even though the output is local
- `crowdin config translations [--tree]` — list translation paths that will be produced

## complete

```
crowdin complete <zsh|bash|fish|powershell>
```

Print the shell completion script for the given shell (e.g. `source <(crowdin complete zsh)` in `~/.zshrc`). This command is registered by the completion library and accepts none of the global or config options.

## Aliases, renames, and scope notes

| Command | Notes |
|---|---|
| `crowdin push` | Alias of `upload` |
| `crowdin pull` | Alias of `download` |
| `auto-translate` | v4 `pre-translate`, renamed, **no alias** |
| `init` | v4 `generate` |
| `branch list` / `language list` / `file list` | v4 `list branches` / `list languages` / `list project` |
| `config sources` / `config translations` / `config lint` | v4 `list sources` / `list translations` / `lint` |

Enterprise-only: `download sources --reviewed`, `task add --workflow-step`. String-based projects only: `branch clone`, `branch merge`, `file upload --cleanup-mode`, `file upload --update-strings`.
