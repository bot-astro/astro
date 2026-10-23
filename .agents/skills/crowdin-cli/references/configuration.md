# Crowdin CLI — Installation, Configuration & Environment

## Contents

- [Installation](#installation)
- [The configuration file](#the-configuration-file)
- [Placeholders](#placeholders)
- [Per-file-group options](#per-file-group-options)
- [Language mapping](#language-mapping)
- [Validation rules the CLI enforces](#validation-rules-the-cli-enforces)
- [Environment variables](#environment-variables)
- [Credentials outside crowdin.yml](#credentials-outside-crowdinyml)
- [Argument files](#argument-files)
- [Common trip-ups](#common-trip-ups)

## Installation

| Method | Command |
|---|---|
| npm | `npm i -g @crowdin/cli` |
| Homebrew | `brew tap crowdin/crowdin && brew install crowdin@5` |
| Docker | `docker pull crowdin/cli` |
| WinGet | `winget install Crowdin.CrowdinCLI` |
| Chocolatey | `choco install crowdin-cli` |
| Debian/Ubuntu | Crowdin apt repo (`https://artifacts.crowdin.com/repo/deb/`), then `apt-get install crowdin` |
| RPM (yum/dnf) | Crowdin rpm repo (`https://artifacts.crowdin.com/repo/rpm`), then `yum/dnf install crowdin` |
| Arch (AUR) | `crowdin-cli` package |
| Nix | `nix-shell -p crowdin-cli` |
| Standalone binary | Download from https://github.com/crowdin/crowdin-cli/releases/latest, `chmod +x`, move onto `PATH` |

The CLI is a self-contained binary — no Java/JRE required (unlike the legacy 4.x). Verify with `crowdin --version`.

## The configuration file

Commands look for `crowdin.yml`, then `crowdin.yaml`, in the current working directory; `-c/--config <path>` overrides. Generate one with `crowdin init`.

```yaml
"project_id": "123456"
"api_token_env": "CROWDIN_PERSONAL_TOKEN"
"base_path": "."
"base_url": "https://api.crowdin.com"

"preserve_hierarchy": true

"files": [
  {
    "source": "/locales/**/*",
    "translation": "/%two_letters_code%/%original_file_name%"
  }
]
```

### Top-level keys

| Key | Meaning |
|---|---|
| `project_id` | Numeric Crowdin project ID |
| `api_token` | Personal access token (owner needs at least Manager permissions in the project). Prefer `api_token_env` for committed configs |
| `base_path` | Local project directory (default `.`). **Resolved relative to the config file's directory**, not the cwd; `~` is expanded |
| `base_url` | `https://api.crowdin.com` (default, crowdin.com) or `https://{organization}.api.crowdin.com` (Enterprise) |
| `preserve_hierarchy` | `true`: keep the local directory structure in Crowdin. `false` (default): strip the common parent directory |
| `files` | Array of file groups (see below) |
| `export_languages` | Array of language codes to limit exports to |
| `settings.ignore_hidden_files` | Default `true`: dot-files **and entire dot-directories** (e.g. `.github/`) are skipped on upload. Set `false` to include them |
| `pseudo_localization` | For `download --pseudo`: `length_correction` (-50..100), `prefix`, `suffix`, `character_transformation` (`asian`, `european`, `arabic`, `cyrillic`) |
| `project_id_env`, `api_token_env`, `base_path_env`, `base_url_env` | Names of environment variables to read the respective value from; each wins over its literal counterpart |

## Placeholders

Used in `translation` (and `ignore`/`dest`/`context`) patterns. The full set the CLI recognizes:

**File placeholders**

| Placeholder | Value |
|---|---|
| `%original_file_name%` | Source file name with extension |
| `%file_name%` | Source file name without extension |
| `%file_extension%` | Source file extension |
| `%original_path%` | Path to the source file's parent directory relative to `base_path` |

**Language placeholders** (at least one is required in `translation`, unless the file group is multilingual)

| Placeholder | Example (Ukrainian) |
|---|---|
| `%two_letters_code%` | `uk` |
| `%three_letters_code%` | `ukr` |
| `%locale%` | `uk-UA` |
| `%locale_with_underscore%` | `uk_UA` |
| `%language%` | `Ukrainian` |
| `%android_code%` | `uk-rUA` |
| `%osx_code%` | `uk.lproj` |
| `%osx_locale%` | `uk` |

Language codes reference: https://developer.crowdin.com/language-codes/

## Per-file-group options

Each entry in `files` supports:

| Key | Meaning |
|---|---|
| `source` | Glob for local source files (required). `*`, `**`, `?` supported |
| `translation` | Where exported translations land (required). Must contain a language placeholder unless `multilingual`/`scheme` is set; must not contain `../` or `/./` |
| `ignore` | Array of patterns to exclude; language placeholders are expanded per project language |
| `dest` | File name/path in Crowdin, when it should differ from the local path. Requires `preserve_hierarchy: true`; with a multi-file `source` it must contain a file placeholder or `**` |
| `type` | Explicit Crowdin file type |
| `update_option` | What happens to translations when a source file updates: `update_as_unapproved` or `update_without_changes` |
| `labels` | Array of label names to attach to uploaded strings |
| `excluded_target_languages` | Array of language codes not to translate this group into |
| `languages_mapping` | Per-placeholder overrides of language codes (see below) |
| `translation_replace` | Map of substrings to replace in resolved translation paths |
| `multilingual` | `true` for multilingual formats (e.g. `.xcstrings`) — lets `translation` omit language placeholders (often `translation` = `source`) |
| `scheme` | Column scheme for CSV/XLSX sources (implies multilingual) |
| `first_line_contains_header` | CSV/XLSX: skip the header row |
| `context` | Path to a text file with file-level context (file-based projects; shown in the Editor's File Context tab). Supports file placeholders |
| `content_segmentation` / `custom_segmentation` | Segmentation control for document formats |
| `translate_content` / `translate_attributes` / `translatable_elements` | XML/HTML import tuning |
| `escape_quotes` | Java Properties: `0`–`3` (default `3`) |
| `escape_special_characters` | Java Properties: `0`/`1` (default `1`) |
| `export_quotes` | JavaScript files: `single` (default) or `double` |
| `import_translations` | Import existing translations from a multilingual source on upload |
| `skip_untranslated_strings` | Export: omit untranslated strings (cannot combine with `skip_untranslated_files`) |
| `skip_untranslated_files` | Export: omit not-fully-translated files |
| `export_only_approved` | Export: approved translations only |
| `export_strings_that_passed_workflow` | Export: only strings that passed the workflow (Crowdin Enterprise only) |

Full upstream reference: https://support.crowdin.com/developer/configuration-file/

## Language mapping

Override exported language codes when your directory names differ from Crowdin's codes. The first-level key is the **placeholder name** used in `translation`; the second level maps `crowdin_code: your_code`:

```yaml
"files": [
  {
    "source": "/locale/en/**/*.po",
    "translation": "/locale/%two_letters_code%/**/%original_file_name%",
    "languages_mapping": {
      "two_letters_code": {
        "uk": "ukr",
        "pl": "pol"
      }
    }
  }
]
```

Mapping can also be configured server-side in Project Settings → Languages; the config-file mapping takes precedence.

## Validation rules the CLI enforces

Violations are validation errors (exit code 2) — `crowdin config lint` surfaces them without running anything:

- `files` entries need non-empty `source` and `translation`.
- `translation` needs a language placeholder unless the group is multilingual (`multilingual: true` or a `scheme`).
- `**` may appear in `translation` only if it also appears in `source`.
- `dest` only works with `preserve_hierarchy: true`, and with a wildcard `source` it must vary per file (contain a file placeholder or `**`).
- `skip_untranslated_strings` and `skip_untranslated_files` are mutually exclusive.
- `base_url` must be a Crowdin URL (`https://api.crowdin.com` or `https://<org>.api.crowdin.com`).
- On the command line, `-s/--source` and `-t/--translation` must be passed together; `--dest` requires both.

## Environment variables

Picked up automatically when set (lowest priority — any config value or flag wins):

| Variable | Meaning |
|---|---|
| `CROWDIN_PERSONAL_TOKEN` | Personal access token |
| `CROWDIN_PROJECT_ID` | Numeric project ID |
| `CROWDIN_BASE_URL` | API base URL (crowdin.com default / `https://{org}.api.crowdin.com`) |
| `CROWDIN_BASE_PATH` | Local project directory |

`.env` files in the working directory are loaded automatically (Bun behavior), so a local `.env` with `CROWDIN_PERSONAL_TOKEN=...` works without exporting.

Proxy support, checked on every run: `HTTP_PROXY_HOST`, `HTTP_PROXY_PORT`, `HTTP_PROXY_USER`, `HTTP_PROXY_PASSWORD`.

Full precedence (highest wins): CLI flags → identity file (`--identity` / `~/.crowdin.yml`) → `*_env` config keys → literal config keys → `CROWDIN_*` variables.

## Credentials outside crowdin.yml

Keep resource description (project `crowdin.yml`) and credentials separate:

- `~/.crowdin.yml` / `~/.crowdin.yaml` — picked up automatically; its credentials override the project config.
- `--identity path/to/credentials.yml` — explicit credentials file per invocation.

Either file may contain `project_id`, `api_token`, `base_path`, `base_url` (and their `*_env` variants).

`crowdin login` writes `~/.crowdin.yml` for you after a browser authorization — see the [commands reference](commands.md#login) for what it writes and the token's 30-day lifetime.

## Argument files

Store arguments in a plain text file, one per line, and pass it as `@file`:

```
--branch feat
--directory src
```

```bash
crowdin string list @args.txt
```

Useful for long option lists and for avoiding shell quote-escaping (especially CroQL expressions on Windows shells).

## Common trip-ups

- **"No sources found for '...' pattern"** — `base_path` is resolved relative to the config file's location, not the cwd. Check `base_path` + `source` composition with `crowdin config sources`; a cwd-relative base path can be forced with the `--base-path` flag (higher priority).
- **"Downloaded translations don't match the current project configuration … will be omitted"** — the file's export pattern in Crowdin doesn't match the `translation` pattern (files were uploaded outside the CLI, or the config changed). Fix by running `crowdin upload sources` (it sets the export pattern), or edit the file settings in Crowdin. Also happens when sources live in a branch and `-b` wasn't passed.
- **"Due to missing respective sources, the following translations will be omitted"** — translation export pattern is empty on the Crowdin side; run `crowdin upload sources` to set it.
- **Empty string values in downloaded JSON** — an effect of *Skip untranslated strings* for JSON: keys are exported with empty values by design.
- **Exit code 101 about a month after `crowdin login` or `crowdin init`** — the browser-issued token expires after 30 days. Re-run `crowdin login`; use a personal access token for CI and anything long-lived.
- **Docs vs. code**: the correct export-workflow key is `export_strings_that_passed_workflow` (plural "strings"); the singular form is silently ignored.
