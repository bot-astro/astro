# Recorded Crowdin MCP outputs (Webflow app, project 12345)

Agents call no tools in these evals. When an agent "calls" a Crowdin connector tool, it reads the recorded output here. If a call is not recorded, it says what it expects and continues.

Recorded on 2026-10-21 from the Crowdin MCP server against a live Webflow app; the project is anonymized to 12345, the site to "Example Site", the user to 1001. Webflow IDs, Crowdin file IDs and job IDs are as returned. Every tool takes `applicationIdentifier: "webflow"` and `projectId: 12345`. Sections marked 2026-10-22 were recorded a day later against the same app and project. Two sections, the push response and the `apply_pre_translation` response, are synthesized rather than recorded and say so. The tree is a representative subset of the full tree, which had the same shape with more pages, posts and components.

## list_application_installations, limit 500

Recorded on 2026-10-22. Default fields only, trimmed to five of the 26 installed apps. An identifier is not always the app's name. With the default `limit` of 25 the first page held 25 apps and Webflow, the 26th, was not on it; that page requested with `fields: ["permissions", "modules"]` ran to about 59,000 characters.

```json
{
 "data": [
  {
   "data": {
    "identifier": "github",
    "name": "GitHub",
    "description": null,
    "logo": "/github.svg",
    "baseUrl": "https://system-app.app.crowdin.net"
   }
  },
  {
   "data": {
    "identifier": "contentful",
    "name": "Contentful",
    "description": null,
    "logo": "/logo.png",
    "baseUrl": "https://contentful.app.crowdin.net"
   }
  },
  {
   "data": {
    "identifier": "iterable-app",
    "name": "Iterable",
    "description": null,
    "logo": "/logo.png",
    "baseUrl": "https://iterable-app.awesome-crowdin.com"
   }
  },
  {
   "data": {
    "identifier": "braze-content-translation",
    "name": "Braze Campaigns & Canvas",
    "description": null,
    "logo": "/logo.svg",
    "baseUrl": "https://braze.app.crowdin.net"
   }
  },
  {
   "data": {
    "identifier": "webflow",
    "name": "Webflow",
    "description": null,
    "logo": "/logo.png",
    "baseUrl": "https://webflow.app.crowdin.net"
   }
  }
 ],
 "pagination": {
  "offset": 0,
  "limit": 500
 }
}
```

## get_application_installation, identifier "webflow"

Recorded on 2026-10-22. The tool takes `identifier` and has no `fields` parameter; `permissions` and `modules` come back by default. Trimmed to the fields that matter, each module to its `key` and `type`.

```json
{
 "data": {
  "identifier": "webflow",
  "name": "Webflow",
  "baseUrl": "https://webflow.app.crowdin.net",
  "permissions": {
   "user": {
    "value": "owner",
    "ids": []
   },
   "project": {
    "value": "own",
    "ids": []
   }
  },
  "modules": [
   {
    "key": "webflow-int",
    "type": "project-integrations"
   },
   {
    "key": "crowdin-files-api",
    "type": "api"
   },
   {
    "key": "file-translation-progress-api",
    "type": "api"
   },
   {
    "key": "integration-files-api",
    "type": "api"
   },
   {
    "key": "crowdin-update-api",
    "type": "api"
   },
   {
    "key": "integration-update-api",
    "type": "api"
   },
   {
    "key": "job-list-api",
    "type": "api"
   },
   {
    "key": "job-info-api",
    "type": "api"
   },
   {
    "key": "job-cancel-api",
    "type": "api"
   },
   {
    "key": "settings-schema-api",
    "type": "api"
   },
   {
    "key": "settings-api",
    "type": "api"
   },
   {
    "key": "settings-update-api",
    "type": "api"
   },
   {
    "key": "sync-settings-api",
    "type": "api"
   },
   {
    "key": "sync-settings-update-api",
    "type": "api"
   }
  ]
 }
}
```

## get_application_installation, identifier "contentful"

Recorded on 2026-10-22, trimmed the same way. Contentful has `project-integrations` and `api` but no job keys and no `file-translation-progress-api`; it lists `crowdin-files-api` twice.

```json
{
 "data": {
  "identifier": "contentful",
  "name": "Contentful",
  "baseUrl": "https://contentful.app.crowdin.net",
  "permissions": {
   "user": {
    "value": "owner",
    "ids": []
   },
   "project": {
    "value": "own",
    "ids": []
   }
  },
  "modules": [
   {
    "key": "contentful-int",
    "type": "project-integrations"
   },
   {
    "key": "crowdin-files-api",
    "type": "api"
   },
   {
    "key": "crowdin-files-api",
    "type": "api"
   },
   {
    "key": "integration-files-api",
    "type": "api"
   },
   {
    "key": "crowdin-update-api",
    "type": "api"
   },
   {
    "key": "integration-update-api",
    "type": "api"
   },
   {
    "key": "settings-api",
    "type": "api"
   },
   {
    "key": "settings-update-api",
    "type": "api"
   },
   {
    "key": "sync-settings-api",
    "type": "api"
   },
   {
    "key": "sync-settings-update-api",
    "type": "api"
   }
  ]
 }
}
```

## get_application_installation, identifier "github"

Recorded on 2026-10-22, trimmed the same way. GitHub has `project-integrations` without `api`: a native integration, not driven by the integration tools.

```json
{
 "data": {
  "identifier": "github",
  "name": "GitHub",
  "baseUrl": "https://system-app.app.crowdin.net",
  "permissions": {
   "user": {
    "value": "all",
    "ids": []
   },
   "project": {
    "value": "own",
    "ids": []
   }
  },
  "modules": [
   {
    "key": "github",
    "type": "project-integrations"
   }
  ]
 }
}
```

## list_integration_files, applicationIdentifier "webflow", projectId 12345

Folders have no `type`. Files have `type`, `tooltip`, `path` and the four flags. Entry IDs are JSON text. Items under a collection have the whole collection entry as their `parentId`.

```json
{
 "data": {
  "files": [
   {
    "id": "6630fb7a547d803086a8cfda",
    "name": "Example Site",
    "path": "/Example Site"
   },
   {
    "id": "pages",
    "name": "Pages",
    "parentId": "6630fb7a547d803086a8cfda",
    "path": "/Example Site/Pages"
   },
   {
    "id": "collections",
    "name": "Collections",
    "parentId": "6630fb7a547d803086a8cfda",
    "path": "/Example Site/Collections"
   },
   {
    "id": "components",
    "name": "Components",
    "parentId": "6630fb7a547d803086a8cfda",
    "path": "/Example Site/Components"
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d011\",\"name\":\"Password\",\"slug\":\"401\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "Password",
    "tooltip": "Password (Last update: Oct 21, 2026, 14:22)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/Password",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d012\",\"name\":\"404\",\"slug\":\"404\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "404",
    "tooltip": "404 (Last update: Apr 30, 2024, 14:08)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/404",
    "isNew": false,
    "notSynced": true,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d014\",\"name\":\"Blog\",\"slug\":\"blog\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "Blog",
    "tooltip": "Blog (Last update: Oct 16, 2026, 09:25)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/Blog",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d013\",\"name\":\"About\",\"slug\":\"about\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "About",
    "tooltip": "About (Last update: Oct 16, 2026, 09:25)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/About",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d016\",\"name\":\"Blog Posts Template\",\"slug\":\"detail_post\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\",\"collectionTemplate\":true}",
    "name": "Blog Posts Template",
    "tooltip": "Blog Posts Template (Collection: Blog Posts) (Last update: Apr 30, 2024, 14:08)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/Blog Posts Template",
    "isNew": false,
    "notSynced": true,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d010\",\"name\":\"Home\",\"slug\":null,\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "Home",
    "tooltip": "Home (Last update: Oct 16, 2026, 09:29)",
    "parentId": "pages",
    "type": "webxml",
    "path": "/Example Site/Pages/Home",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"collection-item\",\"id\":\"6630fb7a547d803086a8d02a\",\"name\":\"Blog Posts\",\"slug\":\"post\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "Blog Posts",
    "parentId": "collections",
    "path": "/Example Site/Collections/Blog Posts"
   },
   {
    "id": "{\"type\":\"collection-item\",\"id\":\"6630fb7a547d803086a8d061\",\"name\":\"5 Principles Of Effective Web Design\",\"slug\":\"5-principles-of-effective-web-design\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\",\"collectionId\":\"6630fb7a547d803086a8d02a\",\"collectionName\":\"Blog Posts\",\"collectionSlug\":\"post\"}",
    "name": "5 Principles Of Effective Web Design",
    "parentId": "{\"type\":\"collection-item\",\"id\":\"6630fb7a547d803086a8d02a\",\"name\":\"Blog Posts\",\"slug\":\"post\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "type": "webxml",
    "tooltip": "5 Principles Of Effective Web Design (Last update: May 15, 2019, 14:15)",
    "path": "/Example Site/Collections/Blog Posts/5 Principles Of Effective Web Design",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"collection-item\",\"id\":\"6630fb7a547d803086a8d062\",\"name\":\"14 Common Problems with modern Web Design\",\"slug\":\"14-common-misconceptions-about-web-design\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\",\"collectionId\":\"6630fb7a547d803086a8d02a\",\"collectionName\":\"Blog Posts\",\"collectionSlug\":\"post\"}",
    "name": "14 Common Problems with modern Web Design",
    "parentId": "{\"type\":\"collection-item\",\"id\":\"6630fb7a547d803086a8d02a\",\"name\":\"Blog Posts\",\"slug\":\"post\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "type": "webxml",
    "tooltip": "14 Common Problems with modern Web Design (Last update: May 15, 2019, 14:15)",
    "path": "/Example Site/Collections/Blog Posts/14 Common Problems with modern Web Design",
    "isNew": false,
    "notSynced": false,
    "synced": false,
    "isUpdated": false
   },
   {
    "id": "{\"type\":\"components-item\",\"id\":\"18638a64-158e-82a5-ef35-2151b27a4e25\",\"name\":\"Navigation\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}",
    "name": "Navigation",
    "parentId": "components",
    "type": "webxml",
    "path": "/Example Site/Components/Navigation",
    "isNew": false,
    "notSynced": true,
    "synced": false,
    "isUpdated": false
   }
  ],
  "message": ""
 }
}
```

## list_crowdin_files, applicationIdentifier "webflow", projectId 12345

Crowdin-side IDs are strings here, and unrelated to the integration's IDs. Folders again have no `type`. The `Components` folder and `Footer` are from 2026-10-22, after Footer was imported.

```json
{
 "data": [
  {
   "id": "34770",
   "name": "Example Site"
  },
  {
   "id": "34772",
   "parentId": "34770",
   "name": "Collections"
  },
  {
   "id": "34774",
   "parentId": "34772",
   "name": "Blog Posts"
  },
  {
   "id": "34786",
   "parentId": "34770",
   "name": "Pages"
  },
  {
   "id": "34798",
   "parentId": "34770",
   "name": "Components"
  },
  {
   "id": "34776",
   "parentId": "34774",
   "name": "5 Principles Of Effective Web Design",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34778",
   "parentId": "34774",
   "name": "10 Quick Tips About Blogging",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34784",
   "parentId": "34774",
   "name": "14 Common Problems with modern Web Design",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34788",
   "parentId": "34786",
   "name": "Blog",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34790",
   "parentId": "34786",
   "name": "About",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34792",
   "parentId": "34786",
   "name": "Home",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34794",
   "parentId": "34786",
   "name": "Password",
   "type": "webxml",
   "excludedTargetLanguages": null
  },
  {
   "id": "34800",
   "parentId": "34798",
   "name": "Footer",
   "type": "webxml",
   "excludedTargetLanguages": null
  }
 ]
}
```

## list_jobs, applicationIdentifier "webflow", projectId 12345, limit 3

The server returned more than `limit` asked for. `updateCrowdin` is an import, `updateIntegration` a push.

```json
{
 "data": [
  {
   "id": "ecab29c8-a8b4-4184-a8a5-5be204d49d92",
   "progress": 100,
   "status": "finished",
   "title": "Sync files to Webflow",
   "type": "updateIntegration",
   "initiatedBy": "1001",
   "createdAt": "2026-10-21T14:22:15.449Z",
   "finishedAt": "2026-10-21T14:22:16.812Z"
  },
  {
   "id": "ac8bec24-b34b-4672-8c39-840e6e90b03f",
   "progress": 100,
   "status": "finished",
   "title": "Sync files to Crowdin",
   "type": "updateCrowdin",
   "initiatedBy": "1001",
   "createdAt": "2026-10-21T14:18:17.505Z",
   "finishedAt": "2026-10-21T14:18:19.688Z"
  },
  {
   "id": "9e88755c-11fb-449d-b9cf-582f0f8acac2",
   "progress": 100,
   "status": "failed",
   "title": "Sync files to Webflow",
   "type": "updateIntegration",
   "initiatedBy": "1001",
   "createdAt": "2026-10-16T09:25:38.407Z",
   "finishedAt": "2026-10-16T09:25:41.723Z"
  },
  {
   "id": "fbc12064-e834-4de8-863a-f3a5a600e78e",
   "progress": 100,
   "status": "finished",
   "title": "Sync files to Crowdin",
   "type": "updateCrowdin",
   "initiatedBy": "1001",
   "createdAt": "2026-10-16T09:19:21.539Z",
   "finishedAt": "2026-10-16T09:19:28.328Z"
  }
 ]
}
```

## get_job_info, jobId "9e88755c-11fb-449d-b9cf-582f0f8acac2"

A failed push. `payload` and `data` are JSON text. `payload` is the file-to-languages map that was requested; `data` is a smaller map, whose meaning the server does not state.

```json
{
 "data": {
  "id": "9e88755c-11fb-449d-b9cf-582f0f8acac2",
  "info": "Finished with errors. See error logs.",
  "data": "{\"34788\":[\"uk\"],\"34790\":[\"uk\"],\"34792\":[\"uk\"]}",
  "attempt": 0,
  "payload": "{\"34788\":[\"fr\",\"es-ES\",\"de\",\"ja\",\"ko\",\"pt-PT\",\"uk\",\"zh-CN\",\"zz\"],\"34790\":[\"fr\",\"es-ES\",\"de\",\"ja\",\"ko\",\"pt-PT\",\"uk\",\"zh-CN\",\"zz\"],\"34792\":[\"fr\",\"es-ES\",\"de\",\"ja\",\"ko\",\"pt-PT\",\"uk\",\"zh-CN\",\"zz\"]}",
  "progress": 100,
  "status": "failed",
  "title": "Sync files to Webflow",
  "type": "updateIntegration",
  "createdAt": "2026-10-16T09:25:38.407Z",
  "updatedAt": "2026-10-16T09:25:41.723Z",
  "finishedAt": "2026-10-16T09:25:41.723Z"
 }
}
```

## get_job_info, jobId "ecab29c8-a8b4-4184-a8a5-5be204d49d92"

Recorded on 2026-10-22. The push of Password (Crowdin file 34794) in Ukrainian, at 14:22 on 2026-10-21. Password's tooltip in the tree reads "Last update: Oct 21, 2026, 14:22": the date moved when this push wrote the translation into Webflow, with no edit to the source.

```json
{"data":{"id":"ecab29c8-a8b4-4184-a8a5-5be204d49d92","info":null,"data":"{}","attempt":0,"payload":"{\"34794\":[\"uk\"]}","progress":100,"status":"finished","title":"Sync files to Webflow","type":"updateIntegration","createdAt":"2026-10-21T14:22:15.449Z","updatedAt":"2026-10-21T14:22:16.812Z","finishedAt":"2026-10-21T14:22:16.812Z"}}
```

## get_sync_settings, provider "integration"

Nothing scheduled: an empty array.

```json
{"data":[]}
```

## get_sync_settings, provider "crowdin"

Nothing scheduled: an empty object.

```json
{"data":{}}
```

## get_application_settings

Values are typed (booleans, strings). `update_application_settings` takes `config` as a map of strings; the write sections below show how a string value comes back. `schedule` and `new-integration-files` are the auto-sync frequency and the "add new platform items automatically" switch.

```json
{
 "data": {
  "betaApi": false,
  "site": "6630fb7a547d803086a8cfda",
  "uk": "6630fbd09f0b021e3bab4053-6630fbd09f0b021e3bab4058",
  "nonTranslatableFields": "id,slug,fileId",
  "dontTranslateSlugForPages": false,
  "translateAsset": false,
  "schedule": "0",
  "new-crowdin-files": false,
  "new-integration-files": false,
  "importEqSuggestions": false,
  "autoApproveImported": false,
  "translateHidden": false,
  "condition": "0",
  "includeByFilePath": "",
  "excludeByFilePath": "",
  "inContext": false,
  "deleteObsoleteSourceFiles": false
 }
}
```

## update_application_settings, config {"translateHidden": "false"}

Sent while `translateHidden` already read `false`, intended as a no-op. It was not one: the write returned a job, and the settings afterwards held only the key that was sent. A settings write replaces the whole settings object; a change is sent as the full current settings plus the change, every value as a string.

```json
{"data":{"jobId":"86a6e565-cd7f-4062-9417-11b4c0230fc7"}}
```

## get_application_settings after the settings write

The boolean `false` came back as the string `"false"`; nothing else came back at all.

```json
{"data":{"translateHidden":"false"}}
```

## get_job_info for the settings job

A settings write is a job of type `integrationSettingsSave`, finished at once, with the sent body in `payload` as JSON text.

```json
{"data":{"id":"86a6e565-cd7f-4062-9417-11b4c0230fc7","info":null,"data":null,"attempt":0,"payload":"{\"projectId\":12345,\"config\":{\"translateHidden\":\"false\"}}","progress":100,"status":"finished","title":"Save settings","type":"integrationSettingsSave","createdAt":"2026-10-21T15:04:51.152Z","updatedAt":"2026-10-21T15:04:51.186Z","finishedAt":"2026-10-21T15:04:51.186Z"}}
```

## update_application_settings, the full settings as strings (restore)

Sent to restore the settings after the write above, with every recorded value as a string. The read-back has every key, all strings. The tree listed afterwards contains an `Assets` folder it did not contain before, while `translateAsset` reads `"false"`: the app treats the non-empty string as true. Strings are stored as sent and not coerced, so a boolean setting cannot be written correctly through this tool at all. Boolean settings are changed on the app's settings page in Crowdin.

```json
{"data":{"jobId":"218d41fe-171e-42ce-9ac4-3f8ac0ab2b17"}}
```

Read-back:

```json
{"data":{"betaApi":"false","site":"6630fb7a547d803086a8cfda","uk":"6630fbd09f0b021e3bab4053-6630fbd09f0b021e3bab4058","nonTranslatableFields":"id,slug,fileId","dontTranslateSlugForPages":"false","translateAsset":"false","schedule":"0","new-crowdin-files":"false","new-integration-files":"false","importEqSuggestions":"false","autoApproveImported":"false","translateHidden":"false","condition":"0","includeByFilePath":"","excludeByFilePath":"","inContext":"false","deleteObsoleteSourceFiles":"false"}}
```

## update_crowdin_files, About page

The entry trimmed to `id`, `name`, `parentId`, `type`. The response carries the job ID under `data.jobId`.

```json
{"data":{"jobId":"15d42989-2393-4b2a-9b47-1d8798e25841"}}
```

## get_job_info for the import job

Finished within seconds. `payload` is the sent `files` array as JSON text; `data` is `"{}"`.

```json
{"data":{"id":"15d42989-2393-4b2a-9b47-1d8798e25841","info":null,"data":"{}","attempt":0,"payload":"[{\"id\":\"{\\\"type\\\":\\\"dom\\\",\\\"id\\\":\\\"6630fb7a547d803086a8d013\\\",\\\"name\\\":\\\"About\\\",\\\"slug\\\":\\\"about\\\",\\\"siteId\\\":\\\"6630fb7a547d803086a8cfda\\\",\\\"siteName\\\":\\\"Example Site\\\",\\\"primaryLocale\\\":\\\"6630fb7a547d803086a8d020\\\"}\",\"name\":\"About\",\"parentId\":\"pages\",\"type\":\"webxml\"}]","progress":100,"status":"finished","title":"Sync files to Crowdin","type":"updateCrowdin","createdAt":"2026-10-21T15:07:11.742Z","updatedAt":"2026-10-21T15:07:14.691Z","finishedAt":"2026-10-21T15:07:14.691Z"}}
```

## list_integration_files after the import, the About entry

The flags are unchanged after a re-import of an already-imported page: `notSynced` stays `false`, `synced` stays `false`, `isUpdated` stays `false`. Only a never-imported item changes, from `notSynced: true` to `false`.

```json
{"id":"{\"type\":\"dom\",\"id\":\"6630fb7a547d803086a8d013\",\"name\":\"About\",\"slug\":\"about\",\"siteId\":\"6630fb7a547d803086a8cfda\",\"siteName\":\"Example Site\",\"primaryLocale\":\"6630fb7a547d803086a8d020\"}","name":"About","tooltip":"About (Last update: Oct 16, 2026, 09:25)","parentId":"pages","type":"webxml","path":"/Example Site/Pages/About","isNew":false,"notSynced":false,"synced":false,"isUpdated":false}
```

## update_crowdin_files with an untrimmed entry

The About entry sent whole, with `tooltip`, `path`, `isNew`, `notSynced`, `synced`, `isUpdated`. Neither this client nor the server refused it: the call returned a job, and the job's `payload` holds only the four declared keys, so the extra keys were stripped before the import ran. A client that validates inputs against the schema may still refuse them.

```json
{"data":{"jobId":"5e5acc2e-b403-406f-9c8c-a6869eebc41a"}}
```

## get_job_info for the untrimmed probe

```json
{"data":{"id":"5e5acc2e-b403-406f-9c8c-a6869eebc41a","info":null,"data":"{}","attempt":0,"payload":"[{\"id\":\"{\\\"type\\\":\\\"dom\\\",\\\"id\\\":\\\"6630fb7a547d803086a8d013\\\",\\\"name\\\":\\\"About\\\",\\\"slug\\\":\\\"about\\\",\\\"siteId\\\":\\\"6630fb7a547d803086a8cfda\\\",\\\"siteName\\\":\\\"Example Site\\\",\\\"primaryLocale\\\":\\\"6630fb7a547d803086a8d020\\\"}\",\"name\":\"About\",\"parentId\":\"pages\",\"type\":\"webxml\"}]","progress":100,"status":"finished","title":"Sync files to Crowdin","type":"updateCrowdin","createdAt":"2026-10-21T15:10:35.674Z","updatedAt":"2026-10-21T15:10:37.161Z","finishedAt":"2026-10-21T15:10:37.161Z"}}
```

## apply_pre_translation, fileIds [34800], languageIds ["uk"], method "mt", engineId 280492

Not recorded: the call was made from another session, which kept only the job identifier. Synthesized from the job's recorded status below. Engine 280492 is the project's DeepL engine, which lists `uk` among its supported languages.

```json
{"data":{"identifier":"4cc5b169-1f15-4357-b2a0-b1583c26dc73","status":"created","progress":0,"attributes":{"scope":"untranslated","method":"mt","fileIds":[34800],"engineId":280492,"languageIds":["uk"],"autoApproveOption":"none"},"createdAt":"2026-10-22T08:29:36+00:00","updatedAt":"2026-10-22T08:29:36+00:00","startedAt":null,"finishedAt":null,"eta":"1 second"}}
```

## pre_translation_status, jobIdentifier "4cc5b169-1f15-4357-b2a0-b1583c26dc73"

Recorded on 2026-10-22, trimmed to the attributes that matter. Finished at 100.

```json
{"data":{"identifier":"4cc5b169-1f15-4357-b2a0-b1583c26dc73","status":"finished","progress":100,"attributes":{"scope":"untranslated","method":"mt","fileIds":[34800],"engineId":280492,"languageIds":["uk"],"autoApproveOption":"none"},"createdAt":"2026-10-22T08:29:36+00:00","updatedAt":"2026-10-22T08:29:39+00:00","startedAt":"2026-10-22T08:29:37+00:00","finishedAt":"2026-10-22T08:29:39+00:00","eta":"2 seconds"}}
```

## pre_translation_report, jobIdentifier "4cc5b169-1f15-4357-b2a0-b1583c26dc73"

Recorded on 2026-10-22. The finished DeepL job applied nothing: no language in the report.

```json
{"data":{"languages":[],"preTranslateType":"mt"}}
```

## pre_translation_report, a run that applied translations

Recorded on 2026-10-22 for the same file and language with engine 1479 (Crowdin's own MT), run after the user chose it, and trimmed to the Footer file. For the shape of a non-empty report.

```json
{"data":{"languages":[{"id":"uk","files":[{"id":34800,"statistics":{"phrases":2,"words":4}}],"skipped":[],"skippedQaCheckCategories":[]}],"preTranslateType":"mt"}}
```

## get_integration_file_sync_progress, fileId 34790

Keyed by the Crowdin file ID. One entry per target language; each carries `languageId`, `words`, `phrases`, `translationProgress`, `approvalProgress` and `qaChecksStatus`, plus a full `language` object omitted here. Trimmed to two languages; the live response had nine.

```json
{"data":{"34790":[{"languageId":"de","words":{"total":342,"translated":342,"preTranslateAppliedTo":0,"approved":0},"phrases":{"total":27,"translated":27,"preTranslateAppliedTo":0,"approved":0},"translationProgress":100,"approvalProgress":0,"qaChecksStatus":{"total":22,"inProgress":0,"passed":13,"failed":9}},{"languageId":"uk","words":{"total":342,"translated":342,"preTranslateAppliedTo":0,"approved":0},"phrases":{"total":27,"translated":27,"preTranslateAppliedTo":0,"approved":0},"translationProgress":100,"approvalProgress":0,"qaChecksStatus":{"total":22,"inProgress":0,"passed":12,"failed":10}}]}}
```

## update_integration_files, files {"34790": ["uk"]}

Not recorded: no push was made. Synthesized from the import's response shape; the `list_jobs` section above shows the `updateIntegration` job such a push leaves behind.

```json
{"data":{"jobId":"00000000-0000-4000-8000-000000000001"}}
```
