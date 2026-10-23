# CLI path

The loop in SKILL.md, run with `crowdin app api`. Syntax for every `app` subcommand: `crowdin app --help` and `crowdin app api --help`; the `crowdin-cli` skill's command reference lists them too.

## Requirements

`crowdin app methods` and `crowdin app api` need the Crowdin CLI 5.4.0 or later: `unknown command 'methods'` means an older install, fixed with `npm install -g @crowdin/cli`. The token needs the `application.proxy` scope; exit code 103 means it lacks it.

## Step by step

The shared method set is `integration-files`, `crowdin-files`, `crowdin-update`, `integration-update`, `job-info`, `all-jobs`, `jobs`, `file-progress`, `settings`, `sync-settings`. Every call takes `projectId`.

1. **Find the app and the project**: `crowdin app list --verbose -o json`. `permissions.project` is in that output.
2. **Discover**: `crowdin app methods <app>`, then `crowdin app methods <app> <path>` for each method you will call. The text view cuts a parameter's description at its first sentence (`…`); `-o json` carries the full text. See "Discovery" below for an app outside the shared set.
3. **Read the integration tree**: `crowdin app api <app> integration-files -F projectId=<id> -o json`. "Not yet in Crowdin" is `jq '.files[] | select(.type and .notSynced)'`.
4. **Import into Crowdin**: build the body from step 3's output with jq and send it with `--input` (see "Request bodies"). Send `entryIds` first; when the app answers 501, send the `files` form.
5. **Follow the job**: `crowdin app api <app> job-info -F projectId=<id> -f jobId=<jobId>`, repeated until `status` is `finished` or `failed`. A job you stopped polling is reported with this command for the user to run.
6. **Map to Crowdin files**: `crowdin app api <app> crowdin-files -F projectId=<id> -o json`.
7. **Translate**: `crowdin auto-translate` (in the `crowdin-cli` skill). `auto-translate --file` takes the file's Crowdin path, not its ID: `crowdin file list -o json` pairs each `id` with its `path`. Progress: `file-progress -F projectId=<id> -F fileId=<crowdinFileId>`, keyed by the file ID: `jq --arg id <crowdinFileId> '.[$id][] | {languageId, translationProgress, approvalProgress}'`. What a run applied shows in this progress read before and after it: a `translationProgress` that did not move means it applied nothing.
8. **Push back**: `integration-update` with `{"projectId": <id>, "files": {"<crowdinFileId>": ["uk"]}}` through `--input`, then step 5 again.

## Request bodies

- **The verb comes from `-X` or `--input` only.** Without either the call is GET, whatever fields it carries. Writes (`crowdin-update`, `integration-update`, `settings`, `sync-settings`) need `-X POST` or `--input`.
- **Fields build flat bodies** (`-f`, `-F`, `key[]=value` for arrays; see `--help`). Put them after `<app> <path>`: a field flag takes every following word as a value.
- **Nested bodies go through `--input`**: a file, or `-` for stdin. That covers `files` arrays of objects and `integration-update`'s map.
- **Opaque IDs survive only through jq.** Select entries with jq from the app's own output so every byte of the ID survives:

```bash
crowdin app api webflow integration-files -F projectId=12345 -o json \
  | jq --argjson p 12345 --arg path "/Example Site/Collections/Blog Posts/5 Principles Of Effective Web Design" \
      '{projectId: $p, files: [.files[] | select(.type and .path == $path)]}' \
  > body.json
crowdin app api webflow crowdin-update --input body.json --dryrun   # shows the request, sends nothing
crowdin app api webflow crowdin-update --input body.json
```

Webflow takes this `files` form, whole entries as the app returned them. An app that syncs by entry ID takes their `id` instead: `entryIds: [.files[] | select(...) | .id]`. Pass names and paths with `--arg`, never inside the jq program, so quotes in them cannot break the shell.

- **`--dryrun` is the show-then-send step.** The user sees the `--dryrun` output and the exact command before it is sent.

## Settings and sync settings

- `sync-settings -F projectId=<id> -F provider=integration` (or `crowdin`) reads the current list; the same path with `--input` replaces it. Build the new body as the current output plus the change, with jq.
- `settings -F projectId=<id>` reads the app settings; `settings/schema` describes the fields. A `settings` POST through `--input` carries typed JSON, so booleans stay booleans.
- `jobs` with `-X DELETE` stops a running job.

## Discovery

An app whose methods are not the shared set is driven through `crowdin app methods <app>` and its own list method, with the same rules for bodies, jobs and state changes. A method description with no parameters means the app publishes no OpenAPI document; its field names come from where SKILL.md step 2 says.

## Errors

The CLI prints the app's own message when the app sends one. Exit codes are the CLI's usual ones (the `crowdin-cli` skill lists them).

| What you see | Meaning | Next |
|---|---|---|
| `'<app>' returned 4xx: <message>` | The app rejected the request | Fix the request per the message |
| `'<app>' returned 501: ... does not support syncing by entry ID` | The app cannot filter by entry ID | Send the `files` form (step 4) |
| `Application '<app>' declares '<path>' for POST, not GET. Pass -X POST or --input` (exit 102) | The path is declared for another verb; a write sent without `-X POST` or `--input` goes out as GET | Add the flag the message names |
| `Application '<app>' does not declare '<VERB> <path>'` (exit 102) | The app has no such path, usually a typo | Check the path against `crowdin app methods <app>` |
| `Application '<app>' is not installed` (exit 102) | No installed app has that identifier | Check it against `crowdin app list` |
| Exit 103 | The token lacks `application.proxy` | A token with that scope |
| `Request to '<app>' failed with status N` | The response carried no readable message | Report the status and the command; the app gave no details |

## Job results

`job-info` returns the job as SKILL.md describes. Decode `payload` and `data` with `jq '.payload | fromjson'`, repeating `fromjson` while the result is still a string.
