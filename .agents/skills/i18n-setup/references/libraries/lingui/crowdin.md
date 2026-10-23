# Lingui + Crowdin adapter

What Crowdin needs from a Lingui setup. `lingui-framework-setup` (see the `skills` block of this stack's manifest entry) installs Lingui, wires the compiler, and configures extraction; `lingui-best-practices` and `enhanced-message-context` run the wrapping pass; `find-unwrapped-strings` runs recall. Hand those skills the facts below as constraints on their output — instructions to honor, not context to weigh.

Nothing below varies with the project's framework or compiler. Lingui runs on several of both, and routing between them — with the packages and plugin wiring each needs — is what `lingui-framework-setup` detects and decides; the requirements here are locale codes, catalog paths, and file format, and they read the same for every framework Lingui supports.

## Locale codes

The Lingui config's `locales` array and `sourceLocale` — `lingui.config.ts`, or `.js` in a project without TypeScript — must equal the locale set **and code scheme** frozen in `decisions.md` — source plus targets, spelled however that file says — because `crowdin.yml`'s `%locale%` placeholder (and any `languages_mapping` rows) were derived from exactly that scheme. For the greenfield default, the scheme is the targets' Crowdin locale codes (e.g. `['en', 'uk-UA', 'es-ES']`) — exactly what `%locale%` resolves to, so no `languages_mapping` exists. A different scheme — codes the user chose for routing, or the codes an existing project's locale directories already use — is equally fine: it gets one mapping row per differing code, never a directory rename to dodge them. Whatever the scheme, this stack's config carries it exactly; `references/connect.md` and the `crowdin-cli` skill's configuration reference own the mapping mechanics.

## Catalog layout

Catalogs live one directory per locale under the project's source root, named `messages`, so the `catalogs` entry in `lingui.config.ts` resolves to:

```
catalogs: [{ path: '<rootDir>/<sourceDir>/locales/{locale}/messages', include: ['<sourceDir>'] }]
```

`<sourceDir>` is `detection.json`'s `sourceDir` — `src` in most JavaScript and TypeScript projects, which is why the manifest's `lingui-po` row spells the common case literally: source `/src/locales/en/messages.po`, translation `/src/locales/%locale%/%original_file_name%`. A project whose sources sit elsewhere keeps its own root and the same shape beneath it; `crowdin.yml` is then written from the paths that are actually on disk, which is what `crowdin config sources` proves in phase 5.

PO is what `crowdin.yml` uploads, so PO on disk is the requirement here, not a config key. How a project's config expresses that is `lingui-framework-setup`'s call to make — leaving the format at its default, or naming a PO formatter instance from `@lingui/format-po` explicitly, both satisfy this row equally; the only thing this row forbids is a formatter that isn't PO, because that would leave `crowdin.yml` pointed at a file shape that no longer exists on disk. If a delegated skill proposes a different catalog path, directory shape, or format, this row is authoritative — restate this exact layout in the instructions handed to that skill rather than accepting its default.

## Reference comments are a phase-6 input

The catalogs keep their default `#:` file:line reference comments. Phase 5's upload carries them into each string's context on Crowdin, and phase 6's context extraction opens the referenced code instead of guessing what surrounds the string. `lingui-best-practices` offers `formatter({ lineNumbers: false })` for smaller catalog diffs; that option keeps the file path but drops the line, trading phase 6's precision away — the trade is not taken. State this with the other constraints when delegating.

## A route restructure creates follow-up work

A setup that restructures routes — a locale URL segment renames every page path — leaves work only the setup skill can enumerate: internal links that still point at unprefixed paths, a locale switcher the UI now needs, tests that assume the old paths. Ask the delegated skill what the restructure broke or left undone, and append each answer to `plan.md` as its own step.

## Versions

Lingui's version policy is `lingui-framework-setup`'s call — its gate is conditional on the project's Node version, so a flat major asserted on the Crowdin side could contradict a correct upstream decision. The one version pin in this journey is the Crowdin CLI's, in phase 5.

## Compiled output

`.po` sources are never gitignored: they are the files `crowdin.yml` uploads, so hiding them from git silently breaks the upload. Whether the project also writes compiled catalogs alongside them is `lingui-framework-setup`'s call — some bundler integrations compile at import and emit nothing, others need a compile command. Either way, any compiled catalog gets gitignored **by extension**, never by a directory-scoped rule, because a directory-scoped ignore under the catalog root takes the `.po` sources with it. And when compiled catalogs are **already tracked** — routine in connect-only and re-run cases — an ignore rule alone does not untrack them: offer `git rm --cached <paths>` (the files stay on disk) and run it only with the user's consent.

## Post-conditions checklist

After the delegated setup/convert skills run, verify on disk before moving on:

- the Lingui config's `locales` matches the locale set and scheme `decisions.md` recorded; `sourceLocale` matches the decided source.
- the source catalog exists at the resolved `source` path with the `.po` extension and contains `msgid` entries, and the `catalogs` path matches the layout above.
- A per-locale directory exists under the catalog root for every decided locale after `lingui extract`.
- `lingui extract`, the project's type-check, and the build that consumes the catalogs all exit green. That last one is what makes `catalogs_current` verifiable instead of asserted: a pipeline that needs a separate compile command fails its build without one, so the outcome is checked rather than the command being prescribed here.
- Strings introduced by the convert pass — including any from data modules — appear as `msgid`s in the source catalog.
