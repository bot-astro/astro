# CLI reference

`@crowdin/serverless-apps-cli`, binary `crowdin-serverless-apps`. Outside an app directory, run it with `npx @crowdin/serverless-apps-cli@latest`; inside one, `npx crowdin-serverless-apps` resolves the copy pinned as the app's own devDependency, which is both faster and guaranteed to match the scaffold.

## Interactive versus non-interactive

The CLI renders a terminal UI when it can and plain text when it cannot. It switches to plain text under `--lite`, when `CI` is set, or when stdio is not a TTY - which is the normal case for an agent shell, so the terminal UI is not something you will hit.

What you do have to handle: commands that need an answer will still wait for one. `create` without `--template` and `--yes` waits on a picker. Pass the flags and nothing blocks.

## Commands

### Develop

| Command | Notes |
|---|---|
| `create [name]` | `--template`, `--yes`. Always pass `--template projects-dashboard-cli`; `projects-dashboard-standalone` exists for apps that own their build and is not what this workflow assumes. Downloads the template, gives the app and all its modules the name you passed, registers the app remotely, and writes `CROWDIN_APP_ID` into `.env`. No `delete` counterpart exists, so confirm with the user first. |
| `dev` | `--port <n>`, `-y`, `--no-manifest-sync`. Local server with hot reload, and it points the registered app at your machine. Leave this to the user; publishing is what an agent should do. |
| `preview` | `--module <key>`, `--project <id\|identifier>`. Opens the app's page in Crowdin, starts no server. Prints `Opening <url>` and attempts to open a browser. For project modules and `editor-right-panel`, omitting `--project` falls back to your first accessible project. |

### Build and publish

| Command | Notes |
|---|---|
| `build` | `--no-extract`, `--no-bundle`. Produces `dist/app.js` plus `dist/bundle.zip`. |
| `extract` | Scans source for translatable text, updates `locales/*.po`. |
| `publish` | `--no-build`, `-y`, `--no-manifest-sync`. Builds, uploads the zip, switches Crowdin to serving it. `-y` skips the confirmation. |
| `lint` / `format` | Runs the bundled Biome with the shipped config against the app. App-local Biome configs are ignored. |

### Manage

| Command | Notes |
|---|---|
| `list` | `--print`, `--json`. Lists your apps. Doubles as a read-only auth probe. |
| `link` | `--app-id <id>`. Links the working directory to an app that already exists. |
| `manifest status` | Diffs local `manifest.json` against Crowdin. **Exit code 1 means they differ**, which makes it a check you can act on. |
| `manifest validate` | Checks `manifest.json` against the manifest schema, plus forbidden root keys, unknown module types and duplicate keys - and checks `scopes` against the API calls in `src/`, read through the TypeScript compiler and the `@requiredScope` tag of each method in the app's installed `@crowdin/crowdin-api-client`. Offline: no login, no app link; the scope check needs `node_modules` and is skipped with a note without them. **Exit code 1** when invalid, not valid JSON, or a call lacks its scope. Run it after every manifest edit and after writing API calls, before `push`. |
| `manifest push` | Applies the local manifest to Crowdin. |
| `manifest pull` | Overwrites the local manifest from Crowdin. |
| `login` | `--file-storage`, `--port <n>` (one of 26140-26144). OAuth in the browser; stores the token. |
| `logout` | Removes the stored token. |

## What syncs when

This is the single most common way to end up with a published app that does not show up.

- `publish` and `dev` change **only** `bundle.mode` and `bundle.url` - that is, where Crowdin loads the bundle from. `dev` points it at your machine (`external`), `publish` points it back at the uploaded zip (`internal`). Both also rewrite the local `manifest.json` to match, so a modified `manifest.json` after either command is expected, not drift you should revert.
- `modules`, `scopes`, `name`, `logo` and the rest sync **only** through `manifest push`.

So: edit the manifest, run `manifest status`, push if it exits 1, and only then publish.

## Authentication

Credential precedence: OS keychain, then an encrypted file at `~/.crowdin/serverless-apps-cli.json`, then `CROWDIN_PERSONAL_TOKEN` from the environment. The env token is never refreshed, which is fine for a short automated run.

`login` runs OAuth PKCE against `accounts.crowdin.com` with a loopback callback on ports 26140-26144, waits five minutes, and in a non-interactive shell prints the authorize URL as plain text - `open this link (https://accounts.crowdin.com/oauth/authorize?...)`. Start it in the background so you can read that URL and hand it to the user while the command waits.

Crowdin versus Crowdin Enterprise is resolved from the `domain` claim in the token, so there is no `--enterprise` flag to pass. `CROWDIN_BASE_URL` overrides the host if a run needs to target something else.

If the keychain is unavailable in your environment, `login --file-storage` writes to the encrypted file store instead.

## Environment variables

| Variable | Effect |
|---|---|
| `CROWDIN_APP_ID` | Written into the app's `.env` by `create` or `link`; ties the directory to the registered app. |
| `CROWDIN_PERSONAL_TOKEN` | Auth without `login`. Not refreshed. |
| `CROWDIN_BASE_URL` | Override the API host. |
| `CROWDIN_TEMPLATES_REPO` / `CROWDIN_TEMPLATES_REF` | Fetch scaffolding templates from somewhere other than the default repo. |
| `CROWDIN_DEV_CORS_ORIGIN` | Extra allowed origin for the dev server. |
| `CI` | Forces the plain-text path. |

## Toolchain the CLI owns

For `projects-dashboard-cli` apps the CLI supplies Vite, Tailwind, Lingui, Biome and the base tsconfig, and its build settings cannot be overridden: the output is always a single IIFE bundle at `dist/app.js` with CSS injected by JS, because `<bundle-url>/app.js` is the contract the host loads.

A local `vite.config.ts` is merged with those enforced settings; a local `lingui.config.ts` replaces the defaults. Neither is needed unless the user wants something specific, and adding them without a reason only creates a surface that can break. The app's `tsconfig.json` extends `@crowdin/serverless-apps-cli/tsconfig.base.json`.

## App scripts in the scaffold

```bash
pnpm install
pnpm dev            # user-facing local loop
pnpm build
pnpm run publish    # note "run": publish is also an npm builtin
pnpm extract
pnpm lint
pnpm format
pnpm typecheck      # tsc --noEmit
```

There are no tests in the scaffold, so `typecheck`, `lint` and `manifest validate` are the whole automated safety net. All three must be green before the app goes to the user.
