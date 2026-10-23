---
template: lingui
templateVersion: 1
conditions: []
values: [catalogPath, sourceLocale, targetLocales]
budget: { "default": 160 }
---

# Internationalization rules (Lingui)

If `lingui-best-practices` and `enhanced-message-context` are installed in this environment, they are the authority on Lingui idiom and on translator comments; this file records this project's own decisions on top of them.

## 1. This project

- **Message catalogs** live at `<<catalogPath>>`, one populated directory per locale.
- **Source locale**: `<<sourceLocale>>`. Write new source strings in this language, inside macros, so they reach the catalog.
- **Target locales**: <<targetLocales>>. These come back translated from Crowdin — never write to them by hand. The one exception is a draft made through the `translate` skill, which uploads what it wrote to Crowdin as suggestions in the same run.

## 2. Which macro

Work top to bottom and stop at the first match.

1. **The text lives inside JSX** → wrap it in `<Trans>`.

   ✅ `<Trans>Save changes</Trans>`
   ❌ ``<div>{t`Save changes`}</div>`` — reaching for a string macro when JSX was already there to hold the text

2. **The string sits inside a component but outside JSX** — an attribute, an `alert()`, a prop value → call `useLingui()` and tag it with `` t`...` ``.

   ✅ ``const { t } = useLingui(); <img alt={t`Company logo`} />``
   ❌ ``<img alt="Company logo" />`` — the attribute stays a hardcoded literal in every locale, and no build error ever points at it

3. **The string is declared outside any component** — module scope, a constants object, a config array → describe it with `msg`, then resolve it with the reactive binding from `useLingui()` at the call site where it actually renders, not where it's declared.

   ✅ ``const PLAN_LABELS = { free: msg`Home barista`, pro: msg`Cafe pro` };``
   ✅ ``function PlanCard({ plan }) { const { _ } = useLingui(); return <h3>{_(PLAN_LABELS[plan.id])}</h3>; }``
   ❌ ``const PLAN_LABELS = { free: t`Home barista`, pro: t`Cafe pro` };`` — evaluated once at import time and frozen in whatever locale was active then, so `PlanCard` stops updating when the user switches locale

## 3. Plurals

Never branch on a count by hand — always use `<Plural>` so each target language's own plural rules apply instead of the source language's.

✅
```jsx
<Plural value={count} one="# item" other="# items" />
```

❌
```jsx
{count === 1 ? `${count} item` : `${count} items`}
```

## 4. What not to wrap

Leave these unwrapped: identifiers and slugs, enum and status values, URLs and routes, CSS classes, `data-testid` and other test hooks, `console.*` and logger output, SKU-like codes. Add this project's own non-translatables below as you find them — a brand name, a product code, an internal label.

- _(project-specific entries go here)_

The judgment behind the list, for anything not on it: a string stays unwrapped when a translator changing it would break something, or when showing it in another language would be a bug rather than a feature.

## 5. Translator context

Add a `comment` whenever a translator could plausibly get the string wrong without seeing the UI: short or ambiguous words, any `<Plural>` message, and text carrying inline markup or an unnamed placeholder.

✅ `<Trans comment="Icon button that records a new brew entry; used as a verb, not the noun for a written log">Log</Trans>`
❌ `<Trans>Log</Trans>` — "Log" reads as easily as the noun (the brewing journal) as it does the verb this button performs, and nothing here tells the translator which one is meant

Crowdin note: a `comment` becomes that string's context in Crowdin the moment it's uploaded. Richer context — screenshots, longer notes — is added afterward through `ai_context`, as a separate pass; it supplements the comment, it doesn't replace writing one now.

## 6. Crowdin rules

- Never hand-edit a file under the translation path (`<<catalogPath>>`, resolved per target locale) — every sync overwrites it. Fix a wrong translation in Crowdin, not in the repo.
- Keep every new user-facing string wrapped as you write it, so the next extract-and-upload cycle picks it up on its own — don't let strings pile up unwrapped between syncs.
- Never commit a Crowdin token. `crowdin.yml` reads it from an environment variable; the file itself never carries a literal token value.
