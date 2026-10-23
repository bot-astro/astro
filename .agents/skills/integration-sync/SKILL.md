---
name: integration-sync
description: Syncs content between Crowdin and a platform connected through an installed Crowdin integration app (Webflow, HubSpot, Contentful, Zendesk Guide, Braze and the other store integrations), through the Crowdin connector or the Crowdin CLI. Use when someone wants platform content brought into Crowdin ("import the new pages from Webflow", "translate the latest HubSpot campaigns"), translations sent back ("push the German back to Contentful"), content checked against Crowdin ("anything new in Webflow?"), an integration job checked ("has the Zendesk sync finished"), or a schedule set up ("sync our blog posts every night"). Not for choosing which app to install (how-to-crowdin), installing it (crowdin-cli or the Crowdin UI), building one (create-app), repository and design integrations configured inside Crowdin, or syncing local files with crowdin.yml (crowdin-cli).
---

# Integration sync

Integration apps share one set of API methods, and this skill is the loop over them. The steps and their done conditions are below; the tool or command for each step is in one reference per path.

## Pick the path

- **Connector tools** present in the session (the Crowdin MCP server, as in Crowdin Copilot, Cowork, Claude.ai or ChatGPT): follow [references/connector.md](references/connector.md).
- **A shell with the Crowdin CLI** and no connector tools: follow [references/cli.md](references/cli.md).
- **Both**: the connector, unless the user asked for commands or pasted a `crowdin app` command. Say in one line which path you took.
- **Neither**: describe the loop in the user's terms and name both ways to run it.

Everything below holds on both paths.

## The integration loop

Work through the steps in order; each ends on its done condition. An app whose methods are not the shared set is driven through discovery (step 2) and its own list method, with the same rules for bodies, jobs and state changes; the reference says whether the path can drive it at all.

1. **Find the app and the project.** Find the app among the installed ones and read its permissions. `permissions.project` says which projects the app may serve: `own` (every project the user owns) or `restricted` with a list of IDs. It does not say which project the integration is connected in, and an unconnected project answers 403. With one ID in a `restricted` list, use it; otherwise ask the user for the project. Done when you hold the app identifier and the project ID.
2. **Discover.** Learn which methods the app offers and what each takes. Done when every planned call has its method and required parameters from a description. When a description has no parameters, read the app's documentation link or ask the user. A write goes out with field names taken from a description, the app's documentation, or the user, never guessed.
3. **Read the integration tree.** List the platform's content as the app sees it. Files carry a `type`; folders do not. Done when each item to import is matched to exactly one entry with a `type`; match on `path`, since names repeat. An entry's `notSynced: true` marks an item never imported into Crowdin, and it turns false after the import. The other flags (`synced`, `isNew`, `isUpdated`) read false on an app that does not compute them, so whether an item is in Crowdin is read from `notSynced` alone.
4. **Import into Crowdin.** Send the selected entries from step 3's output, in the shape the reference gives. Done when the call returns a job ID.
5. **Follow the job.** Read the job until `status` is `finished` or `failed`. Done when you have seen that status: a job is reported finished only then, and a job you stopped following is reported as still running, with its ID and how to check it. A `failed` job is reported with its `info`; "Finished with errors. See error logs." can still have delivered part of the work, and those logs are not in the API: send the user to the app's page in the Crowdin project to read them. The job also carries `progress`, `payload` and `data`; some apps store `payload` and `data` as JSON text, sometimes encoded more than once, so decode until the result is no longer a string.
6. **Map to Crowdin files.** List the Crowdin-side files the app manages, matched by name under the right parent folder. Crowdin file IDs are different from the integration's IDs. Done when each imported item has its Crowdin file ID.
7. **Translate.** In Crowdin: pre-translation when the user asked for it (the reference names it), or the project's translators. A pre-translation job ends `finished` whether or not it added anything, so read what it applied (the reference says how) before reporting it. When it applied nothing, say so and ask the user to pick another engine or method, or to fix this engine's setup in Crowdin (its credentials, projects and languages) before a second run. Progress is read per Crowdin file ID and reports translation and approval progress per language. Done when every target language shows the progress the user asked for, or the user says to push as is.
8. **Push back.** Send a map of Crowdin file ID to language codes, then step 5 again. The push request has no approval option, so unapproved translations go out with approved ones. Name the languages whose approval progress is below 100 (step 7), and which of them hold machine or AI output from a pre-translation: in the offer when you propose the push, in the report when the user already asked for it. Done when the push job is `finished`.

## What's new on the platform

A question about new or changed content runs steps 1 to 3 and answers from the tree. Done when every entry with a `type` is counted as new or already in Crowdin.

- **New**: the entries with a `type` that step 3 reads as not yet in Crowdin. An app that marks no entry `notSynced: true` may not compute the flag (an app that loads its tree one level at a time does not by default): count against the Crowdin-side files (step 6) instead. List the new items by `path`, grouped by folder, and add how many items are already in Crowdin and whether auto-sync is set (the sync settings for both providers), so the user knows whether new items arrive on their own.
- **Changed**: the tree has no reliable sign of an edit to an item already in Crowdin. `isUpdated` may not be computed at all (step 3), and a date the app shows (Webflow's tooltip "Last update") also moves when a push writes translations back. Say so, and offer to re-import the items the user wants refreshed.

## Opaque IDs

An entry ID is copied, never written. It is whatever string the app returned, and it can be JSON text full of quotes and apostrophes (Webflow's is). A Webflow, HubSpot or other platform ID on its own matches nothing. Select entries from the app's own output and carry the ID byte for byte; the reference says how on each path.

## Calls that change state

Import and push change content in Crowdin or in the external platform. A settings write, a sync-settings write and a job cancel change the app's configuration or stop work. Run a state-changing call only when the user asked for that change.

Import and push both say "update" in their names and point in opposite directions. Name the direction you understood before sending either: "into Crowdin" is an import, "back to the platform" is a push, and a push overwrites the platform's content.

### Auto-sync and schedules

The sync settings hold two lists, one per `provider`:

| `provider` | Direction | `files` shape |
|---|---|---|
| `integration` | Platform to Crowdin: these items are imported on schedule | Array of tree entries, each with `"schedule": true` |
| `crowdin` | Crowdin to platform: these files' translations are pushed on schedule | Map of Crowdin file ID to languages, `{"1024": ["uk"]}` |

A sync-settings write **replaces the whole list** for its provider and turns on a sync schedule when none is set. The schedule's frequency is in the app settings, and a settings write replaces the whole settings object too: a change is sent as the full current settings plus the change, and the reference says which values the path can carry. When the app offers `new-integration-files` in its settings, turning it on adds new platform items to auto-sync as they appear; that is how "every new blog post" is expressed.

A settings change is **show-then-send**:

1. Read the current value: the sync settings for the same `provider`, or the app settings.
2. Build the new value as the current value plus the change.
3. Show the user the before and after and exactly what will be sent, in the form the reference gives for show-then-send.
4. Send it when the user confirms. When the user is away, the run ends at step 3: report the prepared change and how to send it.
