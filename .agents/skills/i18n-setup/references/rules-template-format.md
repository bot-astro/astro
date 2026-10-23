# Rules Template Format

This document is the contract for anyone authoring a `rules.template.md` file under a library's `references/libraries/<library>/` directory. Templates in this format are the source the setup skill renders into a target repository; this page defines the frontmatter, the conditional-marker grammar, the placeholder syntax, and the exact rendering steps a conforming renderer must perform.

## Purpose

A template does not ship rules directly — it ships instructions for producing them. When the setup skill runs against a target repository, it renders the template for the detected library and stack, and writes the result to `.agents/crowdin-i18n-rules.md` in that repository. That file is committed like any other project file; it is the artifact developers and their agents actually read day to day, and it has no dependency on the skill that produced it.

Because most agent workflows already read one of `CLAUDE.md` or `AGENTS.md`, the render step also wires up two small bridges so the rendered file gets picked up automatically:

- `CLAUDE.md` receives the import line `@.agents/crowdin-i18n-rules.md`, added if it isn't already present.
- `AGENTS.md` receives a short pointer section titled `## Internationalization` that tells the reader where the generated rules live and what they cover.

Both bridges are additive — they point at the generated file rather than duplicating its content, so the generated file stays the single source of truth and can be regenerated without editing either bridge file again.

## Frontmatter contract

Every template opens with a YAML frontmatter block bounded by `---` lines. Five keys are recognized:

| Key | Type | Meaning |
|---|---|---|
| `template` | string | Stable identifier for this template, e.g. the library or stack name. Echoed into the generated header. |
| `templateVersion` | string/number | Version of this template's content. Bump it whenever the body or its variables change meaningfully; echoed into the generated header. |
| `conditions` | array | Every condition key the body's `<!-- if: … -->` markers are allowed to reference. |
| `values` | array | Every placeholder name the body's `<<name>>` tokens are allowed to reference. |
| `budget` | object | Caps how large the *rendered* output may be. Should carry a `default` key giving the maximum rendered line count — the linter only warns if it's missing, the hard cap is enforced later, at render time. |

`conditions` and `values` are declarations, not documentation — they're checked in both directions. A condition key that appears in an `<!-- if -->` marker but isn't listed in `conditions` is an error, and a key listed in `conditions` that no marker in the body ever tests is equally an error. The same two-way rule applies to `values` against `<<name>>` placeholders: an undeclared placeholder in the body fails, and a declared name the body never uses fails too. The intent is that the frontmatter is a complete, exact inventory of every dynamic point in the template — nothing more, nothing less — so a template can be validated without evaluating it.

`budget.default` bounds the rendered file, not the template source; markers, frontmatter, and any branch that gets deleted during rendering don't count against it, since they never reach the output.

Enforcement is split: at lint time a missing `default` is a warning only; the hard limit is enforced at render time, by step 7 of the rendering procedure below. A `budget.default` absent from the frontmatter entirely leaves step 7 no cap to confirm against, and it treats that the same as exceeding one — the self-check fails closed rather than silently passing an unbounded render.

## Markers

Conditional content is delimited with HTML-comment markers so it stays invisible when the template is previewed as plain Markdown. The grammar is intentionally narrow:

```
<!-- if: key == "value" -->
... content shown when key equals "value" ...
<!-- else -->
... content shown otherwise ...
<!-- /if -->
```

`<!-- else -->` is optional; `<!-- /if -->` is required and must close every `<!-- if -->`. A marker line holds exactly one condition key, exactly one operator (`==` or `!=`), and exactly one double-quoted literal — nothing else is valid on that line. Blocks do not nest, and there is no `&&` or `||`: a marker tests one key against one value and no more. If a rule genuinely depends on two axes at once — say, a framework and a bundler — the template doesn't nest two `<!-- if -->` blocks to express it. Instead it declares one composite condition key (for example `stack`) whose values are resolved from both axes before rendering begins, and the marker tests that single composite key. This keeps every marker a one-line, one-comparison statement that a static tool can check without understanding the domain.

## Placeholders

Scalar and list values are substituted with `<<name>>` tokens in the body. A list value renders as its items joined with `, ` (comma-separated); there is no loop or block syntax for lists — if a body needs different formatting per item, that's a sign the value should be split into multiple declared placeholders instead.

The delimiter is `<<` and `>>` because template bodies quote real application code — ICU MessageFormat (`{name}`, `{count, plural, ...}`), Vue/Handlebars (`{{ }}`), JS/TS template literals (`${ }`), Rails I18n (`%{ }`) — and every familiar alternative collides with one of those. `<<name>>` collides with none, so samples stay real and unescaped.

## Rendering procedure

A conforming renderer processes a template in this order:

1. Resolve every condition key declared in the frontmatter to a concrete value for the current render.
2. Delete each `<!-- if -->`/`<!-- else -->`/`<!-- /if -->` block's losing branch, along with *all* marker lines themselves — winning branches keep only their content, never the comments that guarded them.
3. Resolve values, but only for placeholders that survived step 2 — a `<<name>>` inside a branch that was deleted is never looked up.
4. Substitute each surviving `<<name>>` with its resolved value.
5. Strip the frontmatter block from the output.
6. Prepend the generated header (below) as the new first lines of the file.
7. Run the self-check: zero marker comments remain, zero `<<` sequences remain, the generated header occupies line 1, and the total line count is within `budget.default`.
8. Confirm the destination path isn't ignored by the target repository's own Git configuration, via `git check-ignore`.

Any failure at step 7 or step 8 means the render did not succeed — see Fail-closed behavior below for what happens next.

## Value sources

Step 3 says values resolve for surviving placeholders; it doesn't say from where. By the time `generate_coding_rules` runs, every value a template can declare resolves from one of two places already on disk:

| Value | Resolves from | Rendered form |
|---|---|---|
| `sourceLocale` | `decisions.md`'s frozen source locale | the locale as recorded, in the decided scheme — e.g. `en`, which is what a greenfield run records |
| `targetLocales` | `decisions.md`'s frozen target locale list | the locales as recorded, in the decided scheme, joined per the list rule above — e.g. `uk-UA, es-ES`, which is what a greenfield run records |
| `catalogPath` | the matched `catalogs[]` row carried in `manifest-snapshot.json` | the row's per-locale catalog layout, expressed in the library's own placeholder syntax rather than Crowdin's |

`catalogPath` is the one that needs unpacking, because no single field of the row is what it means. The row's `source` names one file — the source locale only, not a per-locale pattern — and its `translation` pattern resolves through Crowdin's own language placeholder (`%locale%`, with `languages_mapping` rows for spellings it doesn't produce), which the rendered file's reader has no reason to recognize: `.agents/crowdin-i18n-rules.md` is read by developers working in the library's own code, not by `crowdin.yml`. So `catalogPath` is the row's layout re-expressed in that library's placeholder form — for `lingui-po`, `src/locales/{locale}/messages.po`, the same layout `lingui.config.ts`'s `catalogs` entry already gives in the stack's adapter. A renderer that cannot resolve a value this way has nothing to fall back to; per the fail-closed rule below, that is a stop, not a guess.

## Generated header

A successful render prepends exactly these two lines, with `<templateVersion>`, `<template>`, and `<variant>` substituted for the current render's values:

```
<!-- crowdin-i18n-rules v<templateVersion> | template=<template> | variant=<variant> | generated by the Crowdin i18n setup skill -->
<!-- Generated file. Re-running the setup overwrites it. Put your own rules in CLAUDE.md or AGENTS.md. -->
```

The first line is a machine-checkable fingerprint (it's what step 7's line-1 check looks for) and a record of exactly which template and variant produced the file. The second line is the human-facing warning: it tells anyone reading the committed file that hand edits belong elsewhere, because the next render will discard them.

`<variant>` is not one of this template's own frontmatter declarations — a renderer resolves it from the matched stack's `variant` field in `manifest-snapshot.json` for the run producing this render, the same value `plan.md`'s header line already carries.

## Fail-closed behavior

Rendering must fail closed. If any condition or value that survives step 2 cannot be resolved to a concrete answer, or if the self-check in step 7 or the ignore check in step 8 does not pass, the renderer does not write a partial or best-effort file. Concretely:

- Any output already written for this render is deleted rather than left half-substituted.
- In guided mode, the renderer asks the user how to proceed rather than guessing.
- In unguided mode, where there's no one to ask, the renderer skips that template and prints a loud, unmissable warning rather than silently producing nothing.
- The `CLAUDE.md`/`AGENTS.md` bridges are never installed on a failed render — a bridge must not be left pointing at a rules file that doesn't exist.

A half-rendered rules file with leftover markers or unresolved placeholders is worse than no file at all, since it looks finished to a casual reader.

## Self-containment

The rendered file has to keep making sense long after the setup skill that produced it is gone — a developer may delete the skill from their repo the day after running it, and the committed rules file must not care. To guarantee that, a template's body (everything outside the frontmatter) must never reference:

- a `.claude/` path,
- any `references/` path, or
- the literal skill name `i18n-setup`.

None of these mean anything once the skill is uninstalled, so a template that leaks one is describing its own machinery instead of describing rules for the target repository. If a body genuinely needs to point a reader somewhere for more detail, it should link to public documentation instead of the local skill's own file layout.
