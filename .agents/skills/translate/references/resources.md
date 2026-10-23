# Fetching the project's resources

Reached from `SKILL.md` step 3. Everything lands in `.crowdin/resources/` (gitignored). Files already present are reused; the user asks for a refresh by saying so. The commands below are account- or project-scoped reads; the `crowdin-cli` skill owns their full option sets.

## Glossary — always

```bash
crowdin glossary list --assigned -o json
crowdin glossary download <id> --format csv --to .crowdin/resources/glossary-<id>.csv
```

`--assigned` lists only the glossaries assigned to the project in `crowdin.yml`; download every one it lists. The CSV has one row per concept with per-language term and description columns — small enough to read whole. A term whose target-language column is empty falls under the glossary rule in `SKILL.md`: rendered consistently, recorded by a translator.

## TM language pair — when earned

Only when the target catalog is thin (fewer than roughly fifty existing translations) or the user wants consistency with legacy content:

```bash
crowdin tm list --assigned -o json
crowdin tm download <id> --source-language-id <source id> --target-language-id <target id> --format csv --to .crowdin/resources/tm-<id>-<source id>-<target id>.csv
```

Both language options are required together. The result is one row per unit with a source column and a target column. Each assigned TM yields its own pair file.

### Lookup

Grep the TM for the phrase, or a distinctive part of it, and read the handful of rows that come back:

```bash
grep -i -F -- 'checkout' .crowdin/resources/tm-*-en-de.csv | head -20
```

An exact source match is reused verbatim; a partial match is a style reference.

## Style guide — always, when assigned

```bash
crowdin style-guide list --assigned --verbose -o json
crowdin style-guide download <id>
```

Keep a guide when its `languageIds` is empty (it applies to every language) or, split on commas, contains the target language id as a whole item (`en` is not `en-GB`); skip the rest. `--verbose` is what puts `languageIds` in the output. The download runs without `--to`: that option takes a file path, not a directory, and the guide's file extension is not in the listing, so the CLI names the file after the guide with the original extension and prints the path; move that file into `.crowdin/resources/`. It is the guide's document in whatever format it was uploaded, so read it as it comes. A guide's separate AI-instructions text is not exposed by the CLI (the listing only says whether one exists), so the document is what the agent reads.

## Several of everything

`--assigned` lists several resources when several are assigned. Download every glossary and every covering style guide; each TM yields its own pair file, and the lookup greps across all of them.
