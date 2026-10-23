# Verify catalogs

Reached from `SKILL.md`'s phase-4 step vocabulary, for the `verify_catalogs` step that the `runtime-catalog` catalog model emits. It is the whole of that step: what to compare, what counts as a defect, and what to do about one.

## Why this step exists at all

A `compile-time-extraction` stack gets this guarantee free. Extraction reads the code and writes the catalog from it, so the two agree by construction, and the plan's `extract_clean` step is what keeps them agreeing.

A `runtime-catalog` stack has no extraction. The catalog is a hand-authored JSON file, and the only thing keeping it in step with the code is whoever last edited both. Nothing in the project fails when they drift: the code compiles, the build is green, the app runs, and a missing key renders as the key itself somewhere a developer wasn't looking. This is also, almost always, a connect-only run — the catalog arrived with the repository rather than being created by this journey — so no earlier phase has looked at it either.

Two things make the drift Crowdin's business rather than a style question:

- **A key in code but not in the catalog is a string translators never see.** The catalog is what phase 5 uploads. What isn't in it isn't in the project, isn't in anyone's queue, and ships in the source language forever.
- **A key in the catalog with no call site is paid for.** It reaches translators, takes their time, and returns translations nothing renders.

The first is a defect. The second is worth reporting and is not this skill's call to fix — a key may be referenced dynamically, or by code outside the scanned tree.

## The comparison

**1. Resolve the source catalog.** Take the matched `catalogs[]` row's `source` from `manifest-snapshot.json` and resolve it against what is actually on disk, the same substitution phase 5 makes for `crowdin.yml`. Note that one row's source is a glob — `i18next-json`'s `/public/locales/en/*.json` — so "the catalog" there is a set of files, one per namespace, and the namespace is part of the key.

**2. Collect the keys the catalog defines.** JSON catalogs nest, and the key a call site uses is the flattened path:

```json
{ "nav": { "home": "Home", "brews": "My brews" } }
```

defines `nav.home` and `nav.brews`, not `nav`. Flatten every object to dotted paths and keep the leaves. For a namespaced layout, the file's own name is the leading segment (`common.json` → `common.greeting`), because that is how the library resolves it.

**3. Collect the keys the code references.** The call-site patterns per library are in [`references/detect/js-ts.md`](detect/js-ts.md#existingconfigured-and-existingstringswrapped) — `useTranslations(` / `getTranslations(` for next-intl, `$t(` / `useI18n(` for vue-i18n, `useTranslation(` / `t(` for i18next. Scan `sourceDir` from `detection.json`.

Two shapes need care, and both are about what the scan cannot see:

- **Scoped translators.** `useTranslations("nav")` followed by `t("home")` references `nav.home`, not `home`. Resolve the scope from the call that created the translator before matching, and when the scope isn't statically obvious, treat every key that function references as unresolvable rather than guessing a prefix.
- **Computed keys.** `` t(`plan.${tier}.name`) `` names a family, not a key. There is no way to enumerate it from source, so do not try.

**4. Compare, in both directions, and count what you couldn't resolve.** The unresolvable count is part of the result, not a footnote to omit: a check that compared 40 of 120 call sites and reported "no missing keys" has said almost nothing, and the number is what tells the user which it was.

## What to do with the result

- **Keys in code, missing from the catalog** — stop and report them with `file:line` and the key, before phase 5 runs. Uploading a catalog already known to be short bakes the gap into the Crowdin project: the strings are absent from the first upload, so nobody translates them, and the fix later is another upload plus another wait. Adding the missing entries is the user's call, not an edit to make unasked — a missing key can equally mean the call site is wrong.
- **Keys in the catalog, no call site found** — report as a note, with the unresolvable count beside it so the number is read in context. Never delete them.
- **Nothing missing** — say what was actually compared: how many keys, how many call sites, how many unresolved. Tick `verify_catalogs` and continue.

## What this step is not

It is not a translation-completeness check — that is `crowdin status`, after the upload, and it answers a different question (are the *translations* done, not are the *strings* present). It is not a lint of the library's API — that is the library's own skills' job. And it is not a substitute for `recall_self_check`: this step compares keys that exist against keys that are used, while recall looks for user-visible strings that were never given a key at all. A connect-only run on a runtime catalog wants both.
