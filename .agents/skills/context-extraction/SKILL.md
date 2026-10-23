---
name: context-extraction
description: Writes translator context (`ai_context`) for Crowdin strings - downloads them with `crowdin context`, fills the JSONL file, uploads it back. Use when enriching a Crowdin project's strings context, when a Crowdin JSONL context file needs its ai_context filled, or when translators guess at ambiguous strings for lack of context.
---

# Context Extraction

## The loop

Commands, filters and the file format: the crowdin-cli skill's [context reference](../crowdin-cli/references/context.md). Read it before the download; its `--status` filter does not mean "needs context". With a JSONL file already in hand, start at step 3.

1. **Coverage** (optional): `crowdin context status`. Done when you can tell the user how many strings have manual, AI and no context.
2. **Download**: `crowdin context download`, with no `--status`. Done when `crowdin-context.jsonl` exists and holds one line per string in scope.
3. **Fill** `ai_context` following the rest of this skill. Done when every line that needs context (see "Strings that need context") has a value, and every changed line passes the validity checklist.
4. **Preview**: `crowdin context upload --dryrun`. Done when it lists exactly the strings you filled.
5. **Upload**: `crowdin context upload`, then `crowdin context status`. Done when the upload lists the same strings the dry run did; report how many, and the new coverage, to the user.

## Information sources (priority order)

For each string, use these JSONL fields in order - stop when you have enough to write a confident `ai_context`:

1. `text` - the source string itself; always the primary signal
2. `key` - often encodes structure (e.g. `button.save`, `modal.title.delete_user`, `error.network.timeout`)
3. `context` - may contain a free-text description, a translator comment, a source file reference, or be empty; treat as supplementary and don't assume any specific format
4. Source files - only if `context` contains a parseable file path + line number and the string is still ambiguous; read ±10–15 lines around the reference to identify UI element type, surrounding component, and props

## Strings that need context

Prioritize these - skip strings that are already clear from `text` + `key` alone:

- Ambiguous short words - single verbs, nouns, or adjectives that change meaning depending on UI placement
- Color / status names - words that name a color, state, or category; could be a label, badge, or filter
- ICU / message format strings - strings with plural forms or named parameters whose meaning depends on what's being counted or substituted
- Strings with inline tags - text containing markup tags where the role of the tagged portion isn't clear from the string alone
- Short phrases with unclear scope - brief imperative or standalone phrases that could belong to multiple UI contexts

## Writing good `ai_context` values

- 1–3 sentences, written for a translator - not a developer
- State the UI element type (button, label, tab, tooltip, modal title, dropdown option, etc.) and where it appears
- For plurals: what entity is counted, what `#` is replaced with
- For inline tags: what the tagged portion renders as (link, code, bold text, etc.)
- For color/status names: whether it's a selectable option, badge, filter label, etc.
- Avoid file names and variable names unless they clarify meaning

### Examples

| String | Key | Good `ai_context` |
|--------|-----|-------------------|
| `"Red"` | `color.red` | `"Color option label in a color picker. Refers to the color red as a selectable choice."` |
| `"Blue"` | *(none)* | `"Color name used as a selectable option or status label. Clarify based on surrounding UI."` |
| `"{count, plural, one {# month} other {# months}}"` | `duration.months` | `"Displays a duration in months. '#' is replaced by the numeric count."` |
| `"Edit <0>src/App.tsx</0> and save to test HMR"` | *(none)* | `"Instructional UI message. The tagged portion is rendered as an inline code element highlighting a filename."` |
| `"New"` | `button.new` | `"Label for a button that creates a new item. The exact entity depends on the current page context."` |

## Orchestrated inputs (optional)

When another workflow invokes this skill with extra context, use it:

- App domain - a one-line description of what the product is. Prefix or fold it into placement descriptions where it disambiguates ("Button label in a coffee-brew logging app…").
- Source checkout - when the JSONL `context` field carries a `#: path:line` reference (PO uploads add these automatically) and the repo is available, read the surrounding code instead of guessing: identify the component type, the props, and what any placeholder is replaced with.

Neither input changes the safety rules below - still edit only `ai_context`.

## JSONL file safety rules

**Only ever edit the `ai_context` field value.** Never touch `id`, `key`, `text`, `file`, or `context`.

### Editing procedure

Single-string (1–5 lines): Use StrReplace scoped to the exact line. Target the `"ai_context":""` substring (or the full current value if already set):

```
old: "ai_context":""
new: "ai_context":"Your context here."
```

Batch (many strings): When dozens or hundreds of lines need context, work in batches instead of one string at a time.

- Read a portion of the file (or the whole file if manageable), identify lines that need `ai_context`, fill context for a batch of those lines (parse line as JSON, set `ai_context`, serialize back to a single line), then write or apply that batch of changes. Repeat for the next batch until done.
- Prefer writing (or applying edits) in batches of lines rather than a single global read-then-write or hundreds of single-line replacements. Batch size is up to the implementer (e.g. tens or a hundred lines per batch).
- When batching, still apply the validity checklist below to a sample of changed lines before saving.

### Validity checklist (verify before saving)

- [ ] The line is still a single valid JSON object
- [ ] `ai_context` value is a quoted string
- [ ] No unescaped double quotes inside the value - use `\"`
- [ ] No literal newlines inside the value - keep it on one line
- [ ] No fields added, removed, or reordered

### Character escaping

- `"` → `\"`
- `\` → `\\`
- Newline → avoid; use a space instead
