# Fanning out a large run

Reached from `SKILL.md` "Large runs", when a subagent tool exists and step 4 fans out: one worker per target language, and for a language with more than about 150 gaps, one worker per chunk of 100 to 150 strings, because one worker drafting a thousand strings is exactly the single pass where later strings get less care than earlier ones. Steps 1–3 and 5–6 stay on the main thread.

## The chunks

The chunks are consecutive slices of one list. The main thread writes the language's gaps once, one record per string (key, source text or plural forms, translator comment, context, source reference) in `.crowdin/translate/gaps-<language id>.jsonl`, in the target file's own order, so the strings of one screen tend to land in one chunk. Chunk `n` is the records from `(n-1)×size+1` to `n×size`. A worker reads its slice and the shared resources from step 3, never the catalog itself.

## Three rules that keep the chunks consistent

- **Conventions sheet first.** The main thread settles step 4's decisions before any worker starts — the register, the rendering of every recurring term the glossary does not cover, and number, date and unit formatting — in `.crowdin/translate/conventions-<language id>.md`, and every worker gets it.
- **One writer per target file.** A chunk worker writes only its own ledger part, `.crowdin/translate/drafts-<language id>-<n>.jsonl`, and returns its table (drafted, skipped, flagged) plus the terms it had to coin. The target file is written once, by the main thread, after the merge.
- **Reconcile before the merge.** The main thread lines up the coined terms across chunks, fixes the ones rendered two ways and adds them to the sheet, concatenates the parts into the language's ledger, deletes the parts and the gap list, writes the drafts into the target file once, and runs steps 5 and 6 once per language.

Done when the language's ledger holds one line per gap in the count and the target file holds every draft — the same criterion as step 4 on a single thread.
