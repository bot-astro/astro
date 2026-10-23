---
name: translate
description: Drafts translations into the project's own resource files from the terminal and uploads them to Crowdin as suggestions for review. Use when a developer asks to translate their app's strings, fill the missing translations for a locale, or translate the catalog into a newly added language. Not for prose documents, not for Crowdin's server-side pre-translation (`crowdin auto-translate`, in crowdin-cli), not for the full internationalize-and-connect journey (i18n-setup, which delegates here).
---

# Translate

A developer adds twelve strings and wants to see them in German today, not after the next translation cycle. This skill is the translator seat for that moment: it drafts translations into the project's own resource files, keeps them consistent with what the project's Crowdin resources already say, checks them mechanically, and uploads them to Crowdin as **unapproved suggestions** for reviewers to decide on.

Its one real advantage over Crowdin's server-side pre-translation is that the agent has the code. A source reference or a translator comment leads to the component a string renders in, and that is where `Order` turns out to be a button rather than a noun. For bulk work that advantage costs more than it earns, and the skill says so (see "Large runs").

Every `crowdin` command below belongs to the `crowdin-cli` skill — syntax, credentials, exit codes, `crowdin.yml`. This file names only the flags it passes.

## Preconditions

- **`crowdin.yml` exists** in the working directory. Without it, stop: say that the skill would be drafting with no glossary, no TM and no review in Crowdin, and offer the `i18n-setup` skill's connect-only path instead.
- **The CLI is installed and authorized.** Exit code `101` means the user runs `crowdin login` in their own terminal; the token value stays out of the conversation.
- **The CLI is current.** `--assigned`, `language list --verbose` and `style-guide` are what this skill leans on; an unknown-option or unknown-command error means an older install; upgrade it the way it was installed (the `crowdin-cli` skill has the line for each method).

## The run

Six steps, in order. Steps 1–3 and 5–6 run on the main thread; step 4 may fan out (see "Large runs").

### 1. Resolve

- **Files.** `crowdin config sources` and `crowdin config translations` print the resolved local source and translation paths per language, with any `languages_mapping` from `crowdin.yml` already applied. The path is the only source of a language's locale.
- **Languages.** The ones the user named, otherwise every project language. Run `crowdin language list --verbose -o json` once and keep two fields per language: `code`, which under the default `--code id` is the Crowdin language id that `-l` takes, and `pluralCategoryNames`, which feeds the plural check in step 5. `code` is the only source of the id; a mapping changes path placeholders, and the id stays what the CLI printed.
- **Gaps.** One rule for every format: an entry present in the source file with no non-empty counterpart in the target file, in that format's own notion of an entry, plus any entry the format marks as needing work (a fuzzy PO entry). A target file that does not exist yet is all gaps. [references/formats.md](references/formats.md) shows the rule in each common format family, the fallback for one it does not list, and the library checks that count gaps for you where they exist (Lingui's `lingui check missing --locale <locale> --mode catalog`).

Done when every language in scope has a target path, an id, its plural categories and a gap count.

### 2. Sync

Two commands, in this order, for the languages in scope:

```bash
crowdin upload sources --cache
crowdin download translations -l <language id> --skip-untranslated-strings
```

Before the upload, a library that can prove its source catalog is current gets asked, about the source catalog only: `lingui check sync --locale <source locale>` for Lingui 6.8 or later, and a failure means `lingui extract` first, so the strings that reach Crowdin are the ones in the code. [references/formats.md](references/formats.md#library-checks) has why the check is asked about the source locale only, and the fallback for an older Lingui. Upload first, so every string about to be translated exists in Crowdin — translations uploaded for a string Crowdin has never seen are ignored. Download second, so the local files reflect what reviewers have already decided. `--skip-untranslated-strings` makes untranslated entries come back absent instead of filled with source text; without it a key-value gap becomes invisible. When the project syncs a Crowdin branch, both commands take the same `-b`; the user or the CI workflow says whether it does, and when neither does, ask.

**Gate:** no target file holds unsaved translation work before the download, because the download replaces the file whole. Work means a non-empty translation value that differs from git, or one git has and the file no longer does: run `git diff -- <target files>` and look for a removed or changed value (`-msgstr "…"` with text in it, a changed JSON or XML value). Whitespace and blank-line differences, header and metadata stamps (`PO-Revision-Date`, `X-Generator`, `X-Crowdin-*`) and new entries with empty values are not work, so the rewrite `lingui extract` gives every locale's catalog passes this gate on its own; say in one line that it was seen and carry on. Real work is a stop-and-ask. For a [multilingual file](references/formats.md#multilingual-files) the target is the source file itself, so the gate applies to it.

Then recount the gaps and show a table per language. Strings a human translated since the last pull are no longer gaps. Server-side TM matching (`crowdin auto-translate --method tm`) is a different tool, owned by the crowdin-cli skill.

### 3. Fetch resources

Only what the run needs, into `.crowdin/resources/` (gitignored — add `.crowdin/` to `.gitignore` if it is not there, showing the one-line diff first). Files already present are reused; the user asks for a refresh by saying so. [references/resources.md](references/resources.md) has the commands and the lookup pattern.

| Resource | When | Why |
|---|---|---|
| The target catalog's own translations | Always | Same product, same language, same voice — the strongest consistency signal, and free. |
| Glossary | Always | Small; a term decided once, everywhere. |
| TM language pair | When the target catalog is thin (fewer than roughly fifty existing translations), or the user wants consistency with legacy content | Grepped for a phrase, a few rows at a time. |
| Style guide | Always, when one is assigned to the project and covers the language | Tone, formality, formatting conventions. |

### 4. Draft

Per string, the sources of truth apply in this order, and an earlier one wins:

1. **Glossary term.** A term in the string binds its translation; a do-not-translate term stays in the source language verbatim. A term with no translation in the target language falls under the glossary rule below.
2. **Existing translation** of the same source string elsewhere in the target catalog, or of a near-identical one. Match it.
3. **TM hit.** An exact source match is reused as is; a partial match is a style reference.
4. **Context.** The translator comment, the `ai_context` if the project wrote one, and the source reference — which the agent follows into the code when the string is ambiguous.
5. **Style guide**, for tone, formality and formatting conventions.

Before the first draft in a language, settle what those sources leave open, once, and apply it everywhere: the register (`du` or `Sie`), one rendering for each recurring term the glossary does not cover, and number, date and unit formatting. A literal amount, date or number in a string is written the way the target locale writes it (`€48,000 – €60,000` is `48.000 € – 60.000 €` for Spain); only an identifier stays as it is (a postal code, a version, a unit tag such as `1 TB`). A run with a placeholder style guide, an empty TM and a glossary without a column for the language still has to render "workspace" somehow; choosing one rendering and holding to it is translating a word consistently, which the glossary rule allows. The choices go into the report as decisions for the reviewers, and into the conventions sheet when the run fans out.

Every draft is recorded first as one line in the language's ledger, `.crowdin/translate/drafts-<language id>.jsonl` ([format](references/formats.md#the-drafts-ledger)), then written into the target file in place, mirroring the source file's structure: same entry order, quoting, indentation, headers and comments. A new target file mirrors the source file's structure entirely. The source catalog stays untouched (in a multilingual file, its source-language entries). A ledger already present for the language from a run that stopped before upload is the resume point: verify and upload from it, or start over on the user's word.

If the project's own scripts run a catalog compile (a `compile` script in `package.json`, a command the rules file `i18n-setup` rendered names), run it after the write so the running app shows the drafts; the library's own skill names the command. A project whose bundler plugin compiles the catalogs at build time has no such step, and there the compile happens once, as the validator in step 5.

Done when every gap in the count has exactly one ledger line, a draft or a skip with a reason, and the target file holds every draft.

### 5. Verify

Three checks, all mechanical, before anything is uploaded:

1. **Placeholders and plurals**, over the ledger, with the `scripts/check-drafts.sh` that ships beside this file:

   ```bash
   scripts/check-drafts.sh .crowdin/translate/drafts-uk.jsonl --categories one,few,many,other
   ```

   `--categories` is the language's `pluralCategoryNames` from step 1. Exit `0` is clean; `1` prints one line per mismatch; `2` means `jq` is missing or the ledger is not valid JSONL — then review the placeholders by hand from the ledger and report the check as not run.
2. **Format validity** of every touched target file, with the validator the family's row in [references/formats.md](references/formats.md#the-families) names: the library's own compile first (`lingui compile` for Lingui, whether or not the project runs it as a build step; delete its output files afterwards when the project does not), else the format tool. A validator that dies on a large file (`msgfmt -c` killed in a sandbox) counts as unavailable: fall through to the next. When none is available, read the file back and report the check as not run. Where the library also counts gaps (`lingui check missing --locale <locale> --mode catalog` for Lingui), its exit `0` after the write is the proof that none was left behind.
3. **PO plural header.** For PO, the target file's `Plural-Forms` header must agree with the language's categories; a disagreement is reported, and the header stays as it is.

Then a table per language — drafted, skipped (with the reason), flagged — and flagged entries are fixed or reverted to gaps. They never ship.

### 6. Upload

```bash
crowdin upload translations -l <language id>
```

Once per touched language, with the same `-b` as the sync. Never `--auto-approve-imported` (drafts are suggestions). `--import-eq-suggestions` only when the language's ledger holds drafts deliberately identical to their source (a unit such as `1 TB`, an address, a product name in a template): without it those never reach Crowdin, Crowdin keeps showing them as untranslated, and the next run drafts them again. The flag is safe here because the download skipped untranslated strings, so nothing else in the file equals its source. The report lists them as kept in the source language. Show the per-language counts and get a go first; in an `i18n-setup` run the approved plan is the go. Close with `crowdin status` for the languages touched.

Say two things plainly in the report: the suggestions appear under the account that owns the token, because the CLI has no way to mark a suggestion as machine-drafted; and reviewers will find them as ordinary unapproved suggestions. Offer, once, a proofread task per language — `crowdin task add <title> --type proofread --language <id> --file <path>` — as the explicit handoff to a human. It is an offer, not a default.

## The rules

These are hard, because they are where agent translation actually breaks production.

- **Placeholders are byte-identical.** `{count}`, `{0}`, `%s`, `%1$d`, `{{name}}`, `<0>…</0>`, `#` in a plural form: same spelling, same count, same case. Reordering to fit the target grammar is fine; renaming is not. In a plural, a target form carries exactly the placeholders of the source form with the same name, so a `one` form keeps its number (`# Element`, not `Ein Element`) and a `one` form whose source spells the number out (`One other device is signed in.`) is not held to a `#` it never had; a form the source does not have (Ukrainian's `few` and `many`) carries every placeholder all source forms share and nothing the source never uses, and takes its `#` from the source `other` form, whose numbers it carves out. That is what the check enforces, and it accepts Lingui's numbering of one expression differently per branch (`{1}` in `one`, `{2}` in `other`) when the target mirrors it. The one flag that is expected and resolved by hand: printf reordering (`%s of %s` → `%2$s von %1$s`), the gettext-sanctioned way to reorder, which the check reports as a rename.
- **Plurals use the target language's categories** — the ones `language list --verbose` printed, never the source language's. `one`/`other` becomes four forms in Ukrainian and collapses to `other` in Japanese.
- **Tags, entities, escape sequences and edge whitespace survive.** A trailing space or newline in the source is in the target too.
- **`max_length` is respected** when the format or the string carries one.
- **Do-not-translate terms stay in the source language:** brand and product names per the glossary, URLs, identifiers, and anything the project's rules file lists as non-translatable.
- **Never write the source catalog. Never approve. Never overwrite an existing non-fuzzy translation** unless the user names the string.
- **A glossary term with no target translation is rendered consistently, and recording it is the translators' work.** Render the word the same way everywhere, in line with the catalog and the TM, and list the rendering as a decision in the report; the glossary itself gains a translation only from a translator.
- **No fuzzy marking of drafts.** Review state lives in Crowdin; a fuzzy PO entry reads as untranslated to the app, which defeats drafting locally.
- **Ambiguity is a skip with a reason, never a guess.** A reviewer can answer a question; nobody can spot a confident wrong translation among two hundred right ones. A skip whose cause is in the code (one msgid for two meanings, a string with no reference) names the fix in the report, such as a `msgctxt` or context for the ambiguous string, so the developer sees it without opening the ledger.

## Large runs

The count table always appears. When the total number of translations exceeds 200, add two sentences before waiting for the go: cost scales with the count, and later strings get less care than earlier ones in a single pass; `crowdin auto-translate --method ai` is the cheaper route for bulk work, and the crowdin-cli skill owns it. Then proceed on a yes. Never refuse on size.

Languages are independent, and so are chunks of one language. In an environment with a subagent tool, step 4 may fan out after step 3: one worker per target language, and for a language with more than about 150 gaps, one worker per chunk of 100 to 150 strings. [references/large-runs.md](references/large-runs.md) has the chunking, the conventions sheet and the merge. Without subagent support the same work runs serially with an identical end state.

## Invoked by another workflow?

The `i18n-setup` journey delegates its `draft_translations` step here. A caller passes the target languages, the resolved catalog paths, and the go from its approved plan; this skill asks nothing twice, runs steps 1–6, and reports the per-language table back. The rules above hold unchanged — the caller's plan never waives the counts shown before upload or the "never approve" rule.

## Related skills

- **`crowdin-cli`** — every command this skill runs.
- **`glossary-terms-extraction`** — produces the glossary this skill consumes.
- **`i18n-setup`** — the full journey; its phase 6 delegates here when the user asked for translations.
