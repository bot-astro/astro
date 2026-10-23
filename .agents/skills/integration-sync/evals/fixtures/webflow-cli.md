# Recorded CLI outputs (crowdin CLI 5.5.0, project 12345, Webflow app installed)

Agents cannot run commands in these evals. When an agent "runs" a command, it reads the recorded output here. If a command is not recorded, it says what it expects and continues.

Recorded from a live Webflow app; IDs, names and the project are anonymized. Help output omits the config and global options blocks.

## crowdin app --help (commands)
Commands:
  list                         List installed apps
  install <identifier>         Install the application
  uninstall <identifier>       Uninstall the application
  methods <identifier> [path]  List the application's API methods, or describe one
  api <identifier> <path>      Call one of the application's API methods

## crowdin app api --help
Usage: crowdin app api <identifier> <path> [options]

Call one of the application's API methods

Arguments:
  identifier                          Application identifier, as printed by 'crowdin app list'
  path                                API method path, optionally with a query string

Options:
  -X, --method <method>               HTTP method: GET, POST, PUT, PATCH or DELETE
  -f, --field <field...>              Add a string field as key=value. Repeat 'key[]=value' to send an array
  -F, --typed-field <typed-field...>  Add a field as key=value, converting true, false, null and integers, and reading @file
      --input <input>                 Read the JSON request body from a file, or '-' for stdin
      --dryrun                        Print a command output without execution
  -h, --help                          Display help message and exit

## crowdin app list --verbose
◆  webflow Webflow (all projects) https://webflow.app.crowdin.net
◆  zendesk-guide Zendesk Guide (projects: 12, 12345) https://zendesk-guide.app.crowdin.net

## crowdin app methods webflow
◆  GET crowdin-files  Get Crowdin Files
◆  GET file-progress  File Translation Progress
◆  GET integration-files  Get Integration Files
◆  POST crowdin-update  Update Crowdin
◆  POST integration-update  Update Integration
◆  GET all-jobs  Job List
◆  GET job-info  Job Info
◆  DELETE jobs  Cancel Job
◆  GET settings/schema  Get App Settings Schema
◆  GET settings  Get App Settings
◆  POST settings  Update App Settings
◆  GET sync-settings  Get Auto-Sync Files
◆  POST sync-settings  Update Auto-Sync Files

## crowdin app methods webflow crowdin-update
POST crowdin-update  Update Crowdin
Update crowdin data
Docs: https://webflow.app.crowdin.net/api-docs#tag/Files/operation/crowdin.update
Body (application/json):
  projectId           integer        required  Project Identifier…
  files               array                    List of integration files and folders
  entryIds            array<string>            List of integration file IDs to sync…
  uploadTranslations  boolean                  Upload exist translation from integration, default false
Example:
  crowdin app api webflow crowdin-update -X POST -F 'projectId=<integer>'

## crowdin app methods webflow crowdin-update -o json   (requestBody.properties descriptions only)
projectId: Project Identifier. Get via [List Projects](https://developer.crowdin.com/api/v2/#operation/api.projects.getMany)
files: List of integration files and folders
entryIds: List of integration file IDs to sync. The framework resolves these IDs to files via getIntegrationFiles and runs the normal sync path. Takes precedence over files when both are provided. Returns 501 if the integration does not support filtering by entry ID. Either files or entryIds is required for API calls.
uploadTranslations: Upload exist translation from integration

## crowdin app methods webflow integration-update
POST integration-update  Update Integration
Update integration data
Docs: https://webflow.app.crowdin.net/api-docs#tag/Files/operation/integration.update
Body (application/json):
  projectId  integer  required  Project Id…
  files      object   required  - **{fileId}** _(integer)_: Crowdin File Id…
Example:
  crowdin app api webflow integration-update -X POST -F 'projectId=<integer>' -F 'files=<object>'

## crowdin app methods webflow sync-settings
■  'sync-settings' is declared for GET and POST. Pass --method to choose one

## crowdin app methods webflow sync-settings -X POST
POST sync-settings  Update Auto-Sync Files
Configure which files should be automatically synchronized
Docs: https://webflow.app.crowdin.net/api-docs#tag/Settings/operation/sync.settings.update
Body (application/json):
  projectId  integer  required  Project Identifier…
  provider   string   required  Sync direction: "crowdin" for Crowdin→Integration, "integration" for Integration→Crowdin
  files      any      required  Format depends on provider…

## crowdin app api webflow integration-files -F projectId=12345   (abridged; real output has 26 entries)
{
  "files": [
    {"id": "5f3c2a1b9d8e7f6a0b1ccfda", "name": "Example Site", "path": "/Example Site"},
    {"id": "pages", "name": "Pages", "parentId": "5f3c2a1b9d8e7f6a0b1ccfda", "path": "/Example Site/Pages"},
    {"id": "collections", "name": "Collections", "parentId": "5f3c2a1b9d8e7f6a0b1ccfda", "path": "/Example Site/Collections"},
    {"id": "{\"type\":\"dom\",\"id\":\"5f3c2a1b9d8e7f6a0b1cd013\",\"name\":\"About\",\"slug\":\"about\",\"siteId\":\"5f3c2a1b9d8e7f6a0b1ccfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"5f3c2a1b9d8e7f6a0b1cd020\"}", "name": "About", "tooltip": "About (Last update: Apr 30, 2024, 14:08)", "parentId": "pages", "type": "webxml", "path": "/Example Site/Pages/About", "isNew": false, "notSynced": true, "synced": false, "isUpdated": false},
    {"id": "{\"type\":\"collection-item\",\"id\":\"5f3c2a1b9d8e7f6a0b1cd02a\",\"name\":\"Blog Posts\",\"slug\":\"post\",\"siteId\":\"5f3c2a1b9d8e7f6a0b1ccfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"5f3c2a1b9d8e7f6a0b1cd020\"}", "name": "Blog Posts", "parentId": "collections", "path": "/Example Site/Collections/Blog Posts"},
    {"id": "{\"type\":\"collection-item\",\"id\":\"5f3c2a1b9d8e7f6a0b1cd060\",\"name\":\"10 Quick Tips About Blogging\",\"slug\":\"10-quick-tips-about-blogging\",\"siteId\":\"5f3c2a1b9d8e7f6a0b1ccfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"5f3c2a1b9d8e7f6a0b1cd020\",\"collectionId\":\"5f3c2a1b9d8e7f6a0b1cd02a\",\"collectionName\":\"Blog Posts\",\"collectionSlug\":\"post\"}", "name": "10 Quick Tips About Blogging", "tooltip": "10 Quick Tips About Blogging (Last update: Apr 30, 2024, 14:08)", "parentId": "{...Blog Posts id...}", "type": "webxml", "path": "/Example Site/Collections/Blog Posts/10 Quick Tips About Blogging", "isNew": false, "notSynced": true, "synced": false, "isUpdated": false},
    {"id": "{\"type\":\"collection-item\",\"id\":\"5f3c2a1b9d8e7f6a0b1cd061\",\"name\":\"5 Principles Of Effective Web Design\",\"slug\":\"5-principles-of-effective-web-design\",\"siteId\":\"5f3c2a1b9d8e7f6a0b1ccfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"5f3c2a1b9d8e7f6a0b1cd020\",\"collectionId\":\"5f3c2a1b9d8e7f6a0b1cd02a\",\"collectionName\":\"Blog Posts\",\"collectionSlug\":\"post\"}", "name": "5 Principles Of Effective Web Design", "tooltip": "5 Principles Of Effective Web Design (Last update: Apr 30, 2024, 14:08)", "parentId": "{...Blog Posts id...}", "type": "webxml", "path": "/Example Site/Collections/Blog Posts/5 Principles Of Effective Web Design", "isNew": false, "notSynced": true, "synced": false, "isUpdated": false}
  ],
  "message": ""
}

## Any crowdin-update call that sends entryIds
■  'webflow' returned 501: This integration does not support syncing by entry ID. Use the files parameter instead.
(exit code 1)

## Any crowdin-update or integration-update call without -X POST and without --input
■  Application 'webflow' declares 'crowdin-update' for POST, not GET. Pass -X POST or --input
(exit code 102)
(integration-update answers the same with its own path)

## Any crowdin app api webflow call to a path the app does not declare (e.g. a typo: crowdin-updates)
■  Application 'webflow' does not declare 'GET crowdin-updates'. Run 'crowdin app methods webflow' to list its methods
(exit code 102)

## Any crowdin app api webflow call with --input <file> --dryrun   (here sync-settings; nothing is sent)
POST https://api.crowdin.com/api/v2/applications/webflow/api/sync-settings
Body:
{ ...the file's JSON, pretty-printed... }
(exit code 0)

## crowdin app api webflow crowdin-update --input <file>   (when the body has projectId and a files array of file objects)
{ "jobId": "0f8e7d6c-1111-4222-8333-444455556666" }

## crowdin app api webflow job-info -F projectId=12345 -f jobId=0f8e7d6c-1111-4222-8333-444455556666
{
  "id": "0f8e7d6c-1111-4222-8333-444455556666",
  "info": null,
  "data": "{}",
  "attempt": 0,
  "payload": "[{\"id\":\"{\\\"type\\\":\\\"collection-item\\\",...}\",\"name\":\"5 Principles Of Effective Web Design\",...}]",
  "progress": 100,
  "status": "finished",
  "title": "Sync files to Crowdin",
  "type": "updateCrowdin",
  "createdAt": "2026-10-16T09:19:21.539Z",
  "updatedAt": "2026-10-16T09:19:28.328Z",
  "finishedAt": "2026-10-16T09:19:28.328Z"
}

## crowdin app api webflow crowdin-files -F projectId=12345 -o json   (abridged)
[
  {"id": "1018", "name": "Example Site"},
  {"id": "1020", "parentId": "1018", "name": "Collections"},
  {"id": "1022", "parentId": "1020", "name": "Blog Posts"},
  {"id": "1024", "parentId": "1022", "name": "5 Principles Of Effective Web Design", "type": "webxml", "excludedTargetLanguages": null}
]

## crowdin app api webflow file-progress -F projectId=12345 -F fileId=1024   (language object abridged)
{ "1024": [ {"languageId": "uk", "eTag": "677d6a5b3eccc9de27fc3364b5cb97c6", "language": {"id": "uk", "name": "Ukrainian", ...}, "words": {"total": 287, "translated": 287, "preTranslateAppliedTo": 287, "approved": 0}, "phrases": {"total": 47, "translated": 47, "preTranslateAppliedTo": 47, "approved": 0}, "translationProgress": 100, "approvalProgress": 0, "qaChecksStatus": {"total": 0, "inProgress": 0, "passed": 0, "failed": 0}} ] }

## crowdin app api webflow integration-update --input <file>   (body {projectId, files: {"1024": ["uk"]}})
{ "jobId": "d1e2f3a4-0000-4000-8000-000000000001" }

## crowdin app api webflow sync-settings -F projectId=12345 -f provider=integration
[ {"id": "{...About page id...}", "name": "About", "schedule": true} ]
