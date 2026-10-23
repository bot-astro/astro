# Migrating from Crowdin CLI 4.x

CLI 5 is a rewrite of the Java-based 4.x, but the essentials are unchanged: the command tree, the `crowdin.yml` configuration file, and the exit codes stay the same. Most workflows carry over as-is. Java/JRE is no longer required — v5 ships as a self-contained binary.

Fix scripts with the find-and-replace changes below.

## Renamed: `pre-translate` → `auto-translate`

There is no alias — update scripts:

```diff
-crowdin pre-translate --method tm
+crowdin auto-translate --method tm
```

## `auto-translate`: `--translate-untranslated-only` removed

Replaced by the more flexible `--scope` option. Translating only untranslated strings is the default:

```diff
-crowdin auto-translate --method tm --translate-untranslated-only
+crowdin auto-translate --method tm

-crowdin auto-translate --method tm --no-translate-untranslated-only
+crowdin auto-translate --method tm --scope all
```

## `--plain` → `--output plain`

The standalone `--plain` flag is gone; use the global `-o, --output` option:

```diff
-crowdin status --plain
+crowdin status --output plain
```

## Negatable options collapsed to single flags

Many boolean options accepted both a positive and a `--no-` form in 4.x. In 5.0, only the form that changes the default behavior remains. Defaults are unchanged, so the removed form was always redundant — drop it:

| Command | Removed form | Migration |
|---|---|---|
| `upload sources`, `file upload` | `--auto-update` | Drop it — source files are updated by default. Use `--no-auto-update` to disable updating. |
| `file upload` | `--no-cleanup-mode`, `--no-update-strings` | Drop them — disabled is the default. |
| `upload translations` | `--no-auto-approve-imported`, `--no-import-eq-suggestions`, `--no-translate-hidden` | Drop them — disabled is the default. |
| `task add` | `--no-skip-assigned-strings`, `--no-include-pre-translated-strings-only` | Drop them — disabled is the default. |
| `screenshot upload` | `--no-auto-tag` | Drop it — disabled is the default. |
| `init` | `--preserve-hierarchy` | Drop it — the generated configuration already sets `preserve_hierarchy: true`. Use `--no-preserve-hierarchy` to generate `false` instead. |

Two commands intentionally keep both forms:

- `string edit` keeps `--hidden` and `--no-hidden` — they trigger different actions.
- File-based commands keep both `--preserve-hierarchy` and `--no-preserve-hierarchy` — they override the configuration-file value; passing neither leaves the configured value untouched.

## Source cache location

The cache used by `upload sources --cache` (`.crowdin/cache.json`) is now resolved relative to the configured `base_path` instead of the current working directory. The first upload after upgrading may rebuild the cache.

## `ignore_hidden_files` now ignores dot-directories

Previously only files whose own name starts with a dot were ignored — files inside a hidden directory (e.g. `.github/config.json`) were still uploaded. Now entire dot-directories are skipped. To keep uploading files from hidden directories, set `ignore_hidden_files: false` in the configuration.

## `distribution add` / `distribution edit`

The deprecated `--export-mode` and `--file` options were removed — use `--bundle-id` instead. The `--branch` option was dropped as well:

```diff
-crowdin distribution add "My Distribution" --export-mode bundle --file strings.xml
+crowdin distribution add "My Distribution" --bundle-id 12
```

## `bundle add`: options renamed

Renamed to avoid clashing with the global config options and to match the underlying API fields:

| Removed | Use instead |
|---|---|
| `--source` | `--source-pattern` |
| `--ignore` | `--ignore-pattern` |
| `--translation` | `--export-pattern` |

```diff
-crowdin bundle add "My Bundle" --format json --source "**/*.json" --ignore "node_modules/**" --translation "%locale%/%file_name%"
+crowdin bundle add "My Bundle" --format json --source-pattern "**/*.json" --ignore-pattern "node_modules/**" --export-pattern "%locale%/%file_name%"
```

## `context download`: `--format` removed

`jsonl` is the sole format, so the flag was redundant — drop it:

```diff
-crowdin context download --format jsonl
+crowdin context download
```

## `config sources`: `--branch` removed

The `--branch` option was removed from `crowdin config sources` — it had no effect.

## New since 4.x (not breaking, worth adopting)

- `crowdin login` (5.1) — browser authorization on its own, saving the token to `~/.crowdin.yml` without generating a configuration file; in 4.x that flow existed only inside `init`.
- Global `-o, --output <json|toon|plain>` on every command — machine-readable output for scripts and agents.
- `auto-translate` gains `--scope`, `--priority`, `--skip-approved-translations`, `--replace-translations-option`, `--reset-approval-status`, `--translation-modified-before`, `--exclude-label`, `--source-language`.
- Shell completions for zsh, bash, fish, and powershell: `crowdin complete <shell>`.
