# Connector path

The loop in SKILL.md, run with the Crowdin MCP server's tools. The tools wrap the shared method set; every integration tool takes `applicationIdentifier` and `projectId`.

## Which apps these tools drive

`get_application_installation` returns an app's `permissions` and `modules`. It takes `identifier` alone (the integration tools take `applicationIdentifier`). An identifier often differs from the app's name (`iterable-app`, `braze-content-translation`). When it is not known yet, find it with `list_application_installations`, `limit: 500` and the default fields, which carry `identifier` and `name`, then read that one app. An app whose `modules` contain both `api` and `project-integrations` is on this path: Webflow, Contentful, Braze, Iterable, MoEngage and the other store integrations. Its `api` module keys name the tools it offers: `integration-files-api` is `list_integration_files`, `crowdin-files-api` is `list_crowdin_files`, `crowdin-update-api` is `update_crowdin_files`, `integration-update-api` is `update_integration_files`, `job-info-api`, `job-list-api` and `job-cancel-api` are `get_job_info`, `list_jobs` and `cancel_job`, `file-translation-progress-api` is `get_integration_file_sync_progress`, `settings-api` and `settings-update-api` are the two settings tools, `sync-settings-api` and `sync-settings-update-api` the two sync-settings tools. A tool whose key is absent is not offered by that app (Contentful has no job keys): an import on such an app is reported as submitted with its job ID, since the job cannot be followed. An app with `project-integrations` alone (GitHub, GitLab and the other repository integrations) is a native integration these tools do not drive; its sync is configured in the Crowdin UI. An app with `api` but not `project-integrations` has methods of its own outside the shared set, and this path cannot call them: with no shell in the session, answer in one line that this app needs the Crowdin CLI (`crowdin app methods` lists what it offers); with a shell, follow `references/cli.md`.

## Tool per step

| Step | Tool | What to know |
|---|---|---|
| 1. Find the app | `get_application_installation` with `identifier`; `list_application_installations` first when the identifier is not known | see "Which apps these tools drive"; `permissions.project` as SKILL.md describes |
| 2. Discover | none | see "Which apps these tools drive" |
| 3. Read the tree | `list_integration_files` | entries as step 3 describes, plus `path`; see "Trim tree entries" before sending any on |
| 4. Import | `update_crowdin_files` | `files` is an array of trimmed entries, see below; returns `data.jobId` |
| 5. Follow the job | `get_job_info` with `jobId`, repeated; `list_jobs` for history; `cancel_job` to stop one | `list_jobs` returns more than its `limit` asks for |
| 6. Map to Crowdin files | `list_crowdin_files` | IDs are strings here and integers in the tools that take a `fileId` |
| 7. Translate | `apply_pre_translation`, only when the user asked for pre-translation, then `pre_translation_status` and `pre_translation_report`; `get_integration_file_sync_progress` with the Crowdin `fileId` for progress | The job's `identifier` from `apply_pre_translation` is what the other two take as `jobIdentifier`. The report lists what was applied per language and file; `languages: []` means nothing was |
| 8. Push back | `update_integration_files` | `files` is the map of Crowdin file ID to language codes, `{"34790": ["uk"]}` |
| Auto-sync | `get_sync_settings`, `update_sync_settings`, each with `provider` | `files` is the array shape for `integration`, the map shape for `crowdin`; the read returns `[]` or `{}` when nothing is scheduled |
| App settings | `get_application_settings`, `update_application_settings` | see "Changing settings" |

## Rules

- **Direction.** `update_crowdin_files` is the import; `update_integration_files` is the push.
- **Trim tree entries.** The `update_crowdin_files` schema declares `id`, `name`, `parentId`, `type` per entry (and the aliases `node_type`, `parent_id`); `update_sync_settings` with `provider: "integration"` declares the same four plus `schedule`, without the `parent_id` alias. The server strips any other key from an entry copied whole from `list_integration_files` and runs the import, but a client that validates tool inputs against the schema refuses the extra keys, so select by `path` and send the declared keys only, with the `id` byte for byte. There is no entry-ID form on this path.
- **Show-then-send** has no dry run here: the exact tool input, as JSON in the conversation, is what the user sees before the call.
- **Follow a job by repeating `get_job_info`.**
- **Paged lists.** A page of exactly `limit` items may have more after it: read the next `offset` before concluding that an item is absent.
- **Saved results.** When a result too large to show is saved to a file, query the file with jq for the records you need; its lines are too long to read whole.
- **A settings write is a job too.** `update_application_settings` returns `data.jobId`, of type `integrationSettingsSave`, finished at once. `update_sync_settings` is expected to answer the same way from its schema, which is unverified.

## Changing settings

`update_application_settings` takes `config` as a map of strings: every value is stored as the string it was sent, and an app can read a non-empty string such as `"false"` as true (Webflow does). A boolean setting therefore cannot be written correctly through this tool, and an app's settings are mostly booleans (`translateHidden`, `new-integration-files`, `autoApproveImported`). Change app settings on the app's page in the Crowdin project, and tell the user which setting to flip; use the tool only for an app whose settings are all strings. There is no settings-schema tool: field meanings come from `get_application_settings` and the app's documentation.

`update_sync_settings` is different: its `files` shapes carry booleans and arrays as JSON, so the auto-sync lists in SKILL.md are written through it.
