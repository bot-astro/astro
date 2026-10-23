# Scopes

`scopes` in the manifest is the list of Crowdin REST permissions the host will allow the app to exercise. The host runs every call under the session of whoever opened the app and rejects anything outside this list with a 403 `Endpoint isn't allowed for token scopes.` - at runtime, inside the iframe, where `typecheck`, `lint` and `publish` see nothing. `manifest validate` is the check that does see it: it matches every call the app makes through the API client to the scope that method needs and compares the result with the manifest. So the list is not composed from memory - write the calls, run `manifest validate`, add the strings it names.

Keep the list to what the app's calls actually need. A reviewer sees it, and so does every admin who installs the app - and the validator notes every declared scope no call uses, so there is no reason to leave one in.

## Postfixes

Most scopes take `:read` or `:write`; the bare scope grants both. `:write` does **not** imply read, so an app that lists and edits needs both or the bare scope - when the validator has asked for both forms of one scope, the bare form is the same grant in one entry. A parent covers its descendants: the bare `project` covers every `project.*` call, `project:read` covers their `:read` calls only, and `project:write` covers all of them, reads included. The narrower child is still the better declaration when it fits - the scaffold ships `project.status.progress:read`, not `project.status`. Read-only scopes such as `vendor` or `security-log` take `:read` like any other; that is the form the validator names.

## Where a call's scope comes from

Every method of `@crowdin/crowdin-api-client` carries a `@requiredScope` tag in its JSDoc - `project.member:read`, `glossary:write`, or `none` for the few endpoints any session may call. Your editor shows it on hover; `manifest validate` reads it from the copy installed in the app, so a newer client brings newer scopes without a CLI update. `createStorage()` counts as a call needing `application.storage`.

Trust the tag over the verb of the operation. Some read-shaped endpoints are `POST` calls the backend guards at write level - project-level glossary and TM concordance search are `glossary:write` and `tm:write` - so a `:write` in the `fix` line under a search feature is the platform's rule, not a mistake to argue with. Add exactly what the validator names; never reach for `*`, which covers everything, passes the check silently and asks every admin who installs the app for the whole organization.

For what a scope actually grants, the authority is [understanding-scopes](https://support.crowdin.com/developer/understanding-scopes/).

## What it prints

A call the manifest does not cover is a problem under `scopes`, with the call that needs it and the string to add:

```
✗ scopes
    "glossary:read" is needed by glossariesApi.listGlossaries at src/corpus.tsx:78
    fix    add "glossary:read" to scopes
```

Notes come after the problems, and a green run can carry notes too, so read past the tick:

```
3 notes
- computed method on usersApi at src/corpus.tsx:67; scope cannot be determined
- possible call usersApi.listUsers at src/untyped.js:3 (type unknown); needs user:read if this is the Crowdin client
- "tm:read": no call in src/ uses it
```

An unused-scope note appears when the scan found calls, or found nothing at all. It stays quiet when it found no typed call but did print a note - an untyped receiver, a computed method name or an untagged method - because that call may be the one using the scope.

## What the check cannot see

Five outputs are not a clean bill:

- `scope check skipped: ...` - the SDK is not installed, or the installed `@crowdin/crowdin-api-client` has no tags (the note says so: `update it to 1.59.2 or later`). `pnpm install`, or `pnpm update @crowdin/serverless-apps-cli @crowdin/crowdin-api-client`, then run it again: a green manifest with this note has not been checked for scopes. If the note survives the update - a pinned or age-gated install - take the scopes from [understanding-scopes](https://support.crowdin.com/developer/understanding-scopes/) for now and validate again once the note goes.
- `possible call <api>.<method> at ... (type unknown)` - a receiver the compiler could not type: a `.js` file, or an explicit `any`. Give it the `Client` type from `@crowdin/crowdin-api-client` and run again.
- `computed method on <api> at ...; scope cannot be determined` - the method name is a variable, as in `client.usersApi[name]()`. Call it by name so the tag can be read.
- `<api>.<method> at ... has no scope annotation in @crowdin/crowdin-api-client <version>; scope cannot be determined` - the installed api-client carries tags, but not on this method (1.59.2 left a handful untagged, `uploadStrings` among them). Nothing in the app clears it: take that call's scope from [understanding-scopes](https://support.crowdin.com/developer/understanding-scopes/) and keep the note in mind when reading an unused-scope note beside it.
- An app that predates the check: its pinned CLI validates the schema only and prints nothing about scopes, which looks exactly like a pass. The same update brings the check and the tags together; validate after it.

The scan follows types, not names: files the tsconfig neither includes nor reaches through an import are not scanned, and no check can see a viewer's rights: a 403 without `Endpoint isn't allowed for token scopes.` is the person, not the manifest.

## Checking what an installed app actually got

```bash
npx crowdin-serverless-apps list --json
```

Each app carries its `scopes` and its resolved `permissions`, so this is how you confirm what the organization granted rather than what the manifest asked for.
