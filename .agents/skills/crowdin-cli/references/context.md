# Strings context commands

`crowdin context` moves strings context between Crowdin and a local JSONL file: `download` exports it, `upload` sends back the `ai_context` values written into the file, `status` reports coverage, `reset` removes AI context. The enrichment loop and how to write the values: the [context-extraction](../../context-extraction/SKILL.md) skill. Config: set B ([commands.md](commands.md#common-options)).

AI context is stored apart from manual context. `upload` and `reset` change only the AI part; what a human wrote stays.

## context download

- `--to <path>` — output file (default `crowdin-context.jsonl`)
- `-f, --file <glob>` — Crowdin file paths (repeatable)
- `--label <label>` — labels (repeatable)
- `-b, --branch <name>` — branch
- `--croql <expr>` — CroQL expression; cannot be combined with the other filters ([croql](../../croql/SKILL.md))
- `--since <YYYY-MM-DD>` — only strings created after this date
- `--status <empty|manual|ai>` — which kind of context a string already has: `empty` (none of any kind), `manual` (source context, no AI context), `ai` (AI context written)

**Leave `--status` off when the goal is "strings that still need AI context".** The option says which kind of context a string has, and which value means "needs AI context" depends on the source format. A format that carries source references into Crowdin (PO `#:` lines, for one) gives every string manual context, so `empty` matches nothing and `manual` matches everything; a format that carries no context inverts that. Without `--status` the download covers both cases in one call and costs nothing extra, because `upload` sends only the lines whose `ai_context` was filled.

## context upload

Accepts only a file produced by `context download`.

- `--from <path>` — input file (default `crowdin-context.jsonl`)
- `--overwrite` — also send lines whose `ai_context` is empty, which removes those strings' AI context
- `--dryrun` — list the strings that would be updated, one per line

## context status

Coverage: how many strings have manual, AI or no context. Same filters as `download` except `--status` and `--to`, plus `--by-file` for a per-file breakdown.

## context reset

Same filters as `status`, plus `--dryrun`, and `--all`, which is required when no filter is given. Run it before re-enriching from scratch.

## JSONL format

One JSON object per line:

| Field | Meaning |
|---|---|
| `id` | String ID in Crowdin |
| `key` | String key |
| `text` | Source text |
| `file` | Crowdin file path |
| `context` | Existing source context |
| `ai_context` | AI context to set |

```json
{"file":"/src/locales/en.po","ai_context":"","context":"#: src/App.tsx:54","id":3125833,"text":"Click on the Vite and React logos to learn more","key":"Click on the Vite and React logos to learn more"}
```
