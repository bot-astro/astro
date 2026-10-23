# Module catalog

A module is one slot in the Crowdin UI that your app fills. One bundle serves every module in the manifest: the host picks which one to run by sending `context.app.type` (plus `key` when the app declares several of the same type), and the SDK runs the registration that matches.

The current inventory lives in the docs, not here: fetch [building-app/modules.md](https://crowdin.github.io/serverless-apps/building-app/modules.md) for the types and their `prepare*` functions, and [reference/manifest.md](https://crowdin.github.io/serverless-apps/reference/manifest.md) for the field shapes. If a type appears there that is missing from the table below, the docs are right and this page is stale - use it.

What this page adds on top of the docs is the part that decides a run: where each slot actually sits in plain words, which edition it exists in, whether `preview` can open it, and whether it needs a project. Ordered by how often each type appears among the 435 apps in the public Crowdin store, so the common cases come first.

## The table

| Type | `prepare*` | Where the user sees it | Extra manifest fields | What `preview` can open |
|---|---|---|---|---|
| `project-integrations` | `prepareProjectIntegrations` | Project → Integrations | - | needs `--project` |
| `project-tools` | `prepareProjectTools` | Project → Tools | - | needs `--project` |
| `organization-menu` | `prepareOrganizationMenu` | Left sidebar, organization level | - | opens directly, **Enterprise only** |
| `profile-resources-menu` | `prepareProfileResourcesMenu` | User profile → Resources | - | opens directly, **crowdin.com only** |
| `editor-right-panel` | `prepareEditorRightPanel` | Translation editor, right-hand panel next to the string | `modes` | needs `--project` |
| `modal` | `prepareModal` | A dialog opened by another module (usually a context-menu entry) | - | no page of its own |
| `organization-settings-menu` | `prepareOrganizationSettingsMenu` | Organization settings | - | opens directly, **Enterprise only** |
| `project-reports` | `prepareProjectReports` | Project → Reports, alongside the built-in reports | - | needs `--project` |
| `context-menu` | `prepareContextMenu` | Right-click menu on a TM, glossary, language, screenshot, style guide or file | `options` | no page of its own |
| `project-menu` | `prepareProjectMenu` | Project navigation, its own page | - | needs `--project` |
| `profile-settings-menu` | `prepareProfileSettingsMenu` | User profile settings | - | opens directly, **crowdin.com only** |
| `editor-translations-panel` | `prepareEditorTranslationsPanel` | Translation editor, translations area | `modes` | no link - open the editor by hand |
| `editor-asset-panel` | `prepareEditorAssetPanel` | Translation editor, asset preview for matching files | `modes`, `fileNamePattern` | no link - open the editor by hand |
| `editor-background-worker` | `prepareEditorBackgroundWorker` | Nothing visible: runs alongside the editor | `modes` | no link, nothing to see |
| `navbar-extension` | `prepareNavbarExtension` | Top navigation bar | - | no page of its own |
| `chat` | `prepareChat` | Chat surface | - | no page of its own |
| `organization-menu-crowdsource` | `prepareOrganizationMenuCrowdsource` | Organization menu, crowdsourcing projects | - | opens directly, **Enterprise only** |
| `project-menu-crowdsource` | `prepareProjectMenuCrowdsource` | Project menu, crowdsourcing projects | - | **Enterprise only**, needs a project that has a public URL |

Only these 18 types exist in the manifest schema. Anything else (`custom-mt`, `custom-file-format`, `ai-provider`, `webhook`, `file-pre-import` and friends) belongs to classic apps with a backend, because Crowdin calls those over HTTP.

Two consequences worth planning around rather than discovering at the end:

- **`preview` produces a link for 11 of the 18 types**, and six of those are locked to one edition: `organization-menu`, `organization-menu-crowdsource`, `organization-settings-menu` and `project-menu-crowdsource` are Enterprise only, `profile-resources-menu` and `profile-settings-menu` are crowdin.com only. For the other seven `preview` fails with an error rather than degrading, so choosing one of them means the run cannot end on a link - say that to the user before scaffolding it and offer the linkable alternative. The scaffold ships `organization-menu` plus `profile-resources-menu` precisely because that pair covers both editions.
- **Only `editor-right-panel` gets an editor link.** The other three editor types are reachable, just not linkable, so the handover for those is "open a project in the editor and look at ...", not a URL.

## Where the project for `--project` comes from

`preview` resolves it through the CLI's own login, not through the app: it calls `GET /projects` (paginated) with the token from `login`, which is why a session without the `project` scope fails with `Could not list your projects`. Your `--project` value is matched against each project's `identifier` or its numeric `id`, and the only type-specific filtering is for `project-menu-crowdsource`, which additionally needs `hasCrowdsourcing` and a public URL.

In an interactive terminal that list becomes a picker. Non-interactively there is no picker: with `--project` the value is matched, and without it the CLI takes the first project on the list. The `Opening <url>` line then shows which project it used.

That fallback keeps a run from dead-ending, but it does not know which project makes sense. For an app whose content depends on the project - a glossary panel, a progress report - confirm the project with the user or verify the data is there, because the first project alphabetically or by id is as likely as not to render an empty screen.

## Asking the user where it goes

Describe the place, not the type. "A panel on the right in the translation editor, next to the string" lands; `editor-right-panel` does not. The two choices worth an actual question are Tools versus Reports inside a project, and organization level versus project level - the rest usually follows from what the user already said.

## Manifest shape

`modules` is a map from type to an array of entries. Every entry needs `key` (unique in the app, `^[0-9a-zA-Z_-]+$`, 3-255 chars) and `name` (3-255 chars); `logo` is optional and root-relative. Declaring `url` is invalid - serverless apps have no backend to point at.

Plain module:

```json
{
  "modules": {
    "project-tools": [
      { "key": "label-manager", "name": "Label Manager" }
    ]
  }
}
```

Editor module - `modes` is required, and the enum is `translate`, `review`, `assets`, `comfortable`, `side-by-side`, `multilingual`:

```json
{
  "modules": {
    "editor-right-panel": [
      {
        "key": "glossary-panel",
        "name": "Glossary for this string",
        "modes": ["translate", "review"]
      }
    ]
  }
}
```

Asset panel - additionally declares which files it handles:

```json
{
  "modules": {
    "editor-asset-panel": [
      {
        "key": "psd-preview",
        "name": "PSD preview",
        "modes": ["assets"],
        "fileNamePattern": "*.psd"
      }
    ]
  }
}
```

Context menu - `options.module` names the target as a single `{"<module type>": "<module key>"}` pair, so a menu entry always points at another module of your own app:

```json
{
  "modules": {
    "context-menu": [
      {
        "key": "sync-glossary",
        "name": "Sync this glossary",
        "options": {
          "location": "glossary",
          "type": "modal",
          "module": { "modal": "sync-dialog" }
        }
      }
    ],
    "modal": [
      { "key": "sync-dialog", "name": "Sync glossary" }
    ]
  }
}
```

`location` is one of `tm`, `glossary`, `language`, `screenshot`, `style_guide`, `source_file`, `translated_file`; `type` is `modal` or `redirect`.

## Registration has to match

For each module type in the manifest, call its `prepare*` once, synchronously, at the top level of `src/index.tsx`:

```tsx
import { prepareProjectTools } from "@crowdin/serverless-apps-sdk";
import labelManager from "./modules/label-manager";

prepareProjectTools(labelManager);
```

With several modules of the same type, pass the manifest `key` as the second argument - a bare string, not an options object - so the host can tell them apart:

```tsx
prepareProjectTools(labelManager, "label-manager");
prepareProjectTools(bulkImport, "bulk-import");
```

If no registration matches what the host sent, the SDK deliberately does not call `prepare()` at all: claiming registration would cancel the host's own "app did not start" panel and leave the user staring at an empty iframe with no explanation. So a blank panel almost always means the manifest and the `prepare*` calls disagree.

## Other manifest fields

`name`, `description`, `logo`, `bundle` (managed by the CLI), `scopes`, `stringBasedAvailable`, and `default_permissions`.

`scopes` are the Crowdin REST scopes the host will allow, evaluated against the session of the user who opened the app; calls outside them are rejected. `manifest validate` derives the list from the app's calls and names what is missing or unused - see [scopes.md](scopes.md). The scaffold ships a realistic example:

```json
"scopes": ["project.settings:read", "project.status.progress:read", "group:read"]
```

`default_permissions` sets who sees the app right after installation: `user` is `owner` (the default), `managers`, `all` or `guests`; `project` is `own` or `restricted`.
