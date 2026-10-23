# Detection rules — JavaScript / TypeScript

Reached from the language router in `SKILL.md` when the repository root has a `package.json`. This file is the rules half of phase 1: it says how to resolve every field of `detection.json`. The schema itself, and the rule that phase 1 writes nothing else, live in `SKILL.md`.

Phase 1 is read-only. Everything below is a file read, a `git` query, or a grep — no installs, no `node_modules` resolution, no build.

**Monorepo:** detect from the `package.json` closest to the working directory — the package being internationalized — and never aggregate dependencies across workspace packages. `sourceDir`, `candidateFiles`, and every `existing.*` field resolve against that same package. A workspace root whose `package.json` only lists workspaces is a stop-and-ask: which package is the target?

## Inputs to read once, up front

| Input | Used for |
|---|---|
| Root `package.json` | `framework`, `compiler`, `react`, `typescript`, `packageManager`, `existing.library` |
| Lockfiles at root | `packageManager` |
| `vite.config.*`, `next.config.*`, `tsconfig.json` | `framework`, `typescript` |
| `manifest.json` → `catalogs[].detect` | `existing.library`, `existing.configured` |
| `crowdin.yml` / `crowdin.yaml`, `.github/workflows/*` | `existing.crowdinYml`, `existing.workflow` |
| Source tree under `sourceDir` | `candidateFiles`, `existing.stringsWrapped`, `localeSignals` |

Throughout, **"in deps"** means present in `dependencies` or `devDependencies` of the root `package.json`. Merge the two before matching; the distinction never changes a detection outcome.

## Field rules

Every rule is first-match-wins in the order written.

| Field | Rule |
|---|---|
| `language` | `"js-ts"` — this file was only reached because a root `package.json` exists. |
| `framework` | `react-scripts` in deps → `"cra"`; else `next` in deps or a `next.config.*` at root → `"next"`; else `vite` in deps or a `vite.config.*` at root → `"vite"`; else `"unknown"`. Check `cra` first: it is the one value that ends the run. |
| `compiler` | `@vitejs/plugin-react-swc` in deps → `"swc"`; else `@vitejs/plugin-react` in deps → `"babel"`; else `framework: "next"` → `"swc"` (Next 12+ compiles with SWC by default); else `null`. A Vite project with no React plugin, and any non-React project, is `null` — not `"babel"`. |
| `react` | `react` in deps. |
| `typescript` | `typescript` in deps, or a root `tsconfig.json` exists. |
| `packageManager` | `packageManager` field in `package.json` → the name in it; else by lockfile: `pnpm-lock.yaml` → `"pnpm"`, `yarn.lock` → `"yarn"`, `bun.lockb` or `bun.lock` → `"bun"`, `package-lock.json` → `"npm"`; else `"npm"`. |
| `sourceDir` | `"src"` when it exists; else `"app"` when it exists (Next app router without `src/`); else `"."`. |
| `git` | `isRepo` from `git rev-parse --is-inside-work-tree`; `branch` from `git branch --show-current`; `remote` from `git remote get-url origin`, `null` when there is no remote. All three are informational — never gate a decision on them. |
| `existing.crowdinYml` | `crowdin.yml` or `crowdin.yaml` exists at the repository root. A config nested in a subdirectory does not count; note it in the summary instead. |
| `existing.workflow` | Any file under `.github/workflows/` whose contents mention `crowdin/github-action`. A workflow that only runs `crowdin` CLI commands directly counts too — record it as `true` and describe which it is. |

## `existing.library`

Match deps against the union of two sources: the library names below, and every `catalogs[].detect` block in `manifest.json` (each carries `deps` and `files`). The manifest is authoritative — when a catalog row is added there, this detection follows without an edit here.

| Value | Signals |
|---|---|
| `"lingui"` | `@lingui/core` in deps, or `lingui.config.js` / `lingui.config.ts` at root |
| `"next-intl"` | `next-intl` in deps |
| `"vue-i18n"` | `vue-i18n` in deps |
| `"i18next"` | `i18next` in deps (including via `react-i18next` or `next-i18next`) |
| `"none"` | no signal above matched |

If two libraries match, prefer the one whose catalog `source` path also has files on disk, and record the ambiguity in the detection summary — two i18n libraries in one project is a stop-and-ask for phase 2, not something to resolve silently.

## `existing.configured` and `existing.stringsWrapped`

`configured` is `true` only when both halves are real: a config or init file from the matched catalog's `detect.files` (or the library's documented config location) exists, **and** at least one catalog file exists at the catalog's `source` path. A dependency in `package.json` with no config is `false`.

`stringsWrapped` is a three-way judgement:

| Value | Condition |
|---|---|
| `"no"` | `existing.library` is `"none"` — always, with no scanning. Also `"no"` when a library is present but zero call sites appear in the source tree. |
| `"yes"` | Call sites are present and `candidateFiles` is empty or holds only skip-list residuals (SKUs, class names, keys). |
| `"partial"` | Call sites are present and real candidate strings remain. |

Count call sites with a grep for the library's own entry points — `t\`` / `<Trans` / `i18n._(` for Lingui, `useTranslations(` / `getTranslations(` for next-intl, `$t(` / `useI18n(` for vue-i18n, `useTranslation(` / `t(` for i18next.

## `candidateFiles`

A grep tuned for recall, not precision — phase 4 gets precision from the rendered rules file's skip-list, and `recall_self_check` catches whatever this missed. A file wrongly listed costs one skipped step; a file wrongly omitted ships an untranslated string.

Scan `sourceDir` for `*.{js,jsx,ts,tsx,vue,svelte}`, excluding `node_modules/`, `dist/`, `build/`, `coverage/`, `.next/`, `*.d.ts`, and test files (`*.test.*`, `*.spec.*`, `__tests__/`).

Four match classes:

| Class | Pattern shape | Example hit |
|---|---|---|
| JSX/template text | A text node between tags containing at least one letter and not purely an expression | `<h1>Track every brew you make</h1>` |
| User-visible attributes | `placeholder=`, `aria-label=`, `title=`, `alt=`, `label=`, plus `content=` on meta tags | `placeholder="Search your brews…"` |
| Display copy in data modules | A display-copy key assigned a string literal, in any exported object or array: `name`, `title`, `label`, `description`, `heading`, `subtitle`, `summary`, `message`, `text`, `tooltip`, `cta`, `error`, `body` | `name: 'Home barista'` in `src/data/plans.ts` |
| String literals in copy helpers | Literal arguments to `toast(`, `alert(`, `confirm(`, `new Error(`, and similar user-facing helpers | `toast('Brew saved')` |

The third class is the one that matters most, because it is where hand detection usually fails: copy lives in a plain `.ts` data module with no JSX anywhere in the file, so a JSX-only scan reports the file as clean. Match the key list against string literals regardless of quote style, including template literals.

Record one entry per file with at least one hit, as `{ "path": "<repo-relative path>", "matchCount": <total hits in that file> }`, sorted by `matchCount` descending. Cap the array at 300 entries and say so in the summary if you truncate.

Use `rg` when available and fall back to `grep -rE`; both are fine, the patterns are not tool-specific.

## `localeSignals`

Two informational lists that phase 2 uses to propose locales and to spot an existing layout that will need `languages_mapping`.

- `existingLocaleDirs` — repo-relative directories that look like catalog homes: `locales/`, `messages/`, `i18n/`, `lang/`, `translations/`, `public/locales/`, wherever they sit in the tree. Include their locale-named children (`locales/uk`, `messages/pt-BR.json`) so phase 2 can see which code style the project already uses — bare, hyphenated, or underscored.
- `readmeHints` — lines from `README*` that mention translations, i18n, supported languages, or a Crowdin badge. Copy the line, do not interpret it.

Both default to `[]`. Neither one is ever the sole basis for a decision; they are prompts for the questions phase 2 asks.
