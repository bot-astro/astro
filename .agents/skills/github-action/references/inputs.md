# Inputs and outputs

Every input of `crowdin/github-action`, its default, and the CLI flag or git behavior it maps to — the mapping is what [`action.yml`](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/action.yml) alone can't tell you, since the flags are assembled in [`entrypoint.sh`](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/entrypoint.sh). Verified against both at `v3.0.0`; the legacy `v2` line (Crowdin CLI 4) declares an identical interface.

- [Upload](#upload)
- [Download](#download)
- [Git and pull request](#git-and-pull-request)
- [Global](#global)
- [Crowdin credentials and patterns](#crowdin-credentials-and-patterns)
- [GitHub Enterprise and commit identity](#github-enterprise-and-commit-identity)
- [Arbitrary CLI command](#arbitrary-cli-command)
- [Environment variables](#environment-variables)
- [Outputs](#outputs)

All boolean inputs take the strings `true` / `false`. Anything unset falls back to the default in the tables below — inputs whose default is `true` act even when you never mention them.

## Upload

| Input | Default | Effect |
|---|---|---|
| `upload_sources` | `true` | Runs `crowdin upload sources`. **Set it to `false` on download-only steps** |
| `upload_translations` | `false` | Runs `crowdin upload translations` |
| `upload_language` | — | `--language=<code>`, one language only, case-sensitive |
| `auto_approve_imported` | `false` | `--auto-approve-imported` — approves the translations it imports |
| `import_eq_suggestions` | `false` | `--import-eq-suggestions` — imports translations equal to the source |
| `upload_sources_args` | — | Appended verbatim to `upload sources` (e.g. `--cache`, `--no-auto-update`, `--label web`) |
| `upload_translations_args` | — | Appended verbatim to `upload translations` (e.g. `--translate-hidden`) |

## Download

| Input | Default | Effect |
|---|---|---|
| `download_translations` | `false` | Runs `crowdin download`, then commits and pushes unless `push_translations: false` |
| `download_sources` | `false` | Runs `crowdin download sources`, then commits and pushes unless `push_sources: false` |
| `download_bundle` | — | Numeric bundle ID → `crowdin bundle download <id>`. The only way to get translations out of a **string-based** project |
| `download_language` | — | `--language=<code>`, one language only |
| `skip_untranslated_strings` | `false` | `--skip-untranslated-strings`. No effect on document formats (`.docx`, `.html`, `.md`) |
| `skip_untranslated_files` | `false` | `--skip-untranslated-files` — omits files that aren't fully translated |
| `export_only_approved` | `false` | `--export-only-approved`. Unapproved strings fall back to the source text unless combined with `skip_untranslated_strings` |
| `download_translations_args` | — | Appended verbatim to the download command (e.g. `--all`) |
| `download_sources_args` | — | Appended verbatim to `download sources` (e.g. `--reviewed`) |

## Git and pull request

These configure the **git half** — the commit-push-PR unit that runs after a download in the same step ([pipeline](../SKILL.md#the-step-is-a-pipeline-not-a-command)).

| Input | Default | Effect |
|---|---|---|
| `push_translations` | `true` | Commit and push downloaded translations to the localization branch |
| `push_sources` | `true` | Commit and push downloaded sources to the localization branch |
| `localization_branch_name` | `l10n_crowdin_action` | Branch the action commits to. Force-pushed on every run with changes |
| `commit_message` | `New Crowdin translations by GitHub Action` | Commit subject |
| `create_pull_request` | `true` | Open a PR from the localization branch |
| `pull_request_title` | `New Crowdin translations by GitHub Action` | PR title |
| `pull_request_body` | — | PR body; newlines are preserved |
| `pull_request_base_branch_name` | triggering ref | PR base. Unset, it falls back to `GITHUB_HEAD_REF` or `GITHUB_REF` stripped of `refs/heads/` |
| `pull_request_labels` | — | Comma-separated labels |
| `pull_request_assignees` | — | Comma-separated usernames, up to 10 |
| `pull_request_reviewers` | — | Comma-separated usernames |
| `pull_request_team_reviewers` | — | Comma-separated team slugs |
| `skip_ref_checkout` | `false` | Skip the action's own `git checkout` of `GITHUB_REF`. Needed when the job checked out a different ref ([recipe](recipes.md#more-patterns-upstream)) |

In order: check out `GITHUB_REF` (unless `skip_ref_checkout`) → fetch and force-check-out the localization branch, reusing the remote branch when it exists → `git add .` → commit → force-push → open the PR → restore the original checkout.

- Commits and pushes use `--no-verify`, so local hooks never run.
- With nothing to commit it logs `NOTHING TO COMMIT` and exits successfully, leaving the branch and the PR untouched.
- When a PR already exists for the same head and base, it logs `PULL REQUEST ALREADY EXIST` and creates nothing.

## Global

| Input | Default | Effect |
|---|---|---|
| `config` | `crowdin.yml` in the repo root | `--config=<path>`, no leading `/` |
| `crowdin_branch_name` | — | `--branch=<name>` — the Crowdin **version branch** (unrelated to `localization_branch_name`). Not applied to `download_bundle` |
| `dryrun_action` | `false` | `--dryrun` — lists the files that would be managed, changing nothing |
| `user` | root | `uid:gid`, or `auto` to take the workspace's owner. Use it so downloaded files aren't root-owned |

Every pipeline call also gets `--no-progress --no-colors`, so logs stay readable in Actions. A `command` step does not.

## Crowdin credentials and patterns

Alternatives to `crowdin.yml` and to the environment variables, passed as CLI flags.

| Input | Maps to |
|---|---|
| `token` | `--token=` — prefer the `CROWDIN_PERSONAL_TOKEN` env var, which keeps the value out of the step's rendered inputs |
| `project_id` | `--project-id=` |
| `base_url` | `--base-url=` — required for Enterprise: `https://{organization}.api.crowdin.com` |
| `base_path` | `--base-path=` |
| `source` | `--source=` — source pattern, no leading `/` |
| `translation` | `--translation=` — translation pattern |

`source` and `translation` exist for the [no-`crowdin.yml`](recipes.md#no-crowdinyml-configuration) mode. Don't combine them with a `crowdin.yml` in the repository.

## GitHub Enterprise and commit identity

| Input | Default | Effect |
|---|---|---|
| `github_base_url` | `github.com` | Git host used for the push URL |
| `github_api_base_url` | `api.<github_base_url>` | REST host used for PR calls |
| `github_user_name` | `Crowdin Bot` | `git config user.name` |
| `github_user_email` | `support+bot@crowdin.com` | `git config user.email` |
| `gpg_private_key` | — | ASCII-armored private key, for signed commits |
| `gpg_passphrase` | — | Passphrase for that key |

For a signed commit to be marked verified, the key's email, the GPG account's email, and `github_user_email` must all match. Export the key with `gpg --armor --export-secret-key <KEY_ID>`.

## Arbitrary CLI command

| Input | Effect |
|---|---|
| `command` | CLI command to run instead of the pipeline — the step exits after it |
| `command_args` | Arguments appended to that command |

A `command` step receives **only** `command` and `command_args`. The action's `config`, `token`, `project_id`, `base_url`, `base_path`, `crowdin_branch_name`, and `dryrun_action` inputs are not forwarded, so pass those as flags inside `command_args` and supply credentials through `env:`.

## Environment variables

| Variable | Needed for |
|---|---|
| `CROWDIN_PERSONAL_TOKEN` | Every Crowdin call, unless `token` is passed |
| `CROWDIN_PROJECT_ID` | Every Crowdin call, unless `project_id` is passed |
| `GITHUB_TOKEN` or `GH_TOKEN` | Pushing and PR creation only. `GH_TOKEN` wins when both are set |
| `RUNNER_DEBUG` | Set by GitHub when you re-run with debug logging; makes the action dump its environment and add `--verbose --debug` |

## Outputs

| Output | Value |
|---|---|
| `pull_request_url` | URL of the PR created by this run |
| `pull_request_number` | Its number |
| `pull_request_created` | `true` when a new PR was opened, `false` when one already existed or nothing was committed |
| `command_output` | stdout of the `command` step (multiline) |

Both PR outputs are empty unless *this* run created the PR — a run that finds an existing PR, or has nothing to commit, reports `pull_request_created=false` and no URL. Gate follow-up steps on `if: steps.<id>.outputs.pull_request_url` when they should only act on a fresh PR.

`pull_request_url` arrives wrapped in double quotes (the action doesn't use `jq -r`). Shell interpolation in a `run:` step strips them, but a workflow-expression comparison such as `== 'https://...'` will not match.
