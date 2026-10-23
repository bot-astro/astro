# CLI path

Draft the glossary as a CSV, review it with the user as a whole, and upload it with the Crowdin CLI. `SKILL.md` decides which terms qualify and how each is written. For the `glossary` command's full flag set, read the `crowdin-cli` skill's [commands reference](../../crowdin-cli/references/commands.md#glossary); below are only the flags this path passes.

## Inputs

Resolve these before picking any term:

- **The source catalog** - the file `crowdin.yml` uploads, at its resolved `source` pattern, not the source tree. Those are exactly the strings translators will see, already stripped of code. Without a `crowdin.yml`, ask where the source-language strings live rather than scanning code for them.
- **A draft location outside version control** - a scratch directory, or a gitignored path the caller names - so a rejected draft never lands in the repository.
- **The existing glossary.** `crowdin glossary list --assigned` lists the glossaries assigned to the project in `crowdin.yml`, usually exactly one, with term counts. With a non-zero count, `crowdin glossary download <id> --format csv --to <draft location>` and drop every candidate it already holds.

Both commands are authenticated: the token comes from the environment (`CROWDIN_PERSONAL_TOKEN`, or whatever `crowdin.yml`'s `api_token_env` names), and its value is never asked for, echoed or written. A gitignored `.env` at the project root is enough: the CLI reads `.env` from its working directory.

## Draft the CSV

Write the terms to the draft location:

```csv
term_en,description_en
Plantly,The product name. Keep in English everywhere.
Pro Gardener,The paid plan's name. Keep in English.
water,To record that one plant was watered. A verb here; the app never uses the noun.
cutting,A piece of a plant grown into a new one. Not the act of cutting.
```

The header uses the **source locale's Crowdin language id** (`term_en`, `description_en` for an `en` source). That is the language id, not whatever code the project's locale directories use: the two may differ, and the glossary import only speaks Crowdin ids. When no caller passed it, ask for it rather than reading it off a directory name. Translations lifted from paired material go in one more column per language, such as `term_uk`.

Then show the user every term and stop. This is a real gate: the file is about to become an asset their translators treat as authoritative. Ask for an explicit go-ahead, and take edits before uploading rather than after.

## Upload

Uploading without an id creates a *second* glossary rather than merging, so the `glossary list` result from the inputs decides the command:

- **A glossary already exists.** Import into it: `crowdin glossary upload <file> --id <id>`, with the same `--scheme` and `--first-line-contains-header` as below. **`terms: 0` is the empty glossary Crowdin creates with every new project** - nothing to preserve, so import and move on. A **non-zero count is an asset somebody curated**: say plainly that the import merges into it, and let the user decline, which a project with a mature glossary usually should.
- **No glossary exists.** Create one by uploading without `--id`, passing `--language` with the source locale's Crowdin language id:

```bash
crowdin glossary upload glossary.csv \
  --language en \
  --scheme term_en=0,description_en=1 \
  --first-line-contains-header
```

Map any translation column too (`term_uk=2`). The upload prints the new glossary's id: report it, and record it wherever the caller keeps decisions, because a later run passes it to `--id` instead of creating a duplicate.

The import is asynchronous. `glossary upload` prints `Imported in #<id>` while `glossary list` can still report `terms: 0`; the count catches up moments later. Confirm by re-reading the count, so a race is not mistaken for a failed import.

## Failure and re-runs

- **The upload fails.** The terms survive in the draft CSV, so nothing has to be regenerated. Read the error before changing the file: an authentication or permission failure is not a malformed CSV.
- **The user declines the upload.** Leave the CSV in place, say where it is and which command would upload it, and stop.

## Invoked by another workflow

A calling workflow (the `i18n-setup` journey is one) may pass the inputs directly instead of having them asked: the source catalog path, the product description, the draft path, and the source locale's Crowdin language id. Honor them as given. The contract back is small: the draft CSV at the path it named, the review gate still real (the caller's user, not the caller, says go), and one of two outcomes reported - the uploaded glossary's id, or "declined, draft kept at `<path>`". Both are complete outcomes.
