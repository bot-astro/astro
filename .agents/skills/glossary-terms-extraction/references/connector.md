# Connector path

Read and write the glossary through the Crowdin connector's tools. `SKILL.md` decides which terms qualify and how each is written; this file is how they get there.

## 1. Take stock

When the environment says which glossary the user is looking at (Crowdin Copilot passes it as `entity: { type: 'glossary', id }`), read that one with `get_glossary` and `list_terms`. Otherwise call `list_glossaries` - with a project in scope, the glossaries attached to it; without one, the organization's - and read their terms with `list_terms`. Tell the user what you found in a sentence or two.

## 2. Settle three things before mining

Skip whatever the user's message already answered and ask only what is missing, as a short multiple-choice question when the environment offers one. A wrong glossary or a missed source is hard to undo once dozens of terms have landed.

**Where terms land**, in priority order:

1. What the user said - "add to Acme Glossary", "create a new glossary called X". It overrides everything below, including the glossary they are viewing.
2. The glossary they are viewing, when they said nothing about placement. Use it and say so in one line ("Adding to *Acme Glossary*, the one you're viewing").
3. Otherwise, ask: an existing glossary (the project's attached ones first, then organization glossaries that look relevant), or a new one with `add_glossary` (its name and source language).

A new glossary for a project in scope is attached with `edit_project` (`defaultGlossaryId`).

**What to mine.** Ask for the reference material `SKILL.md` describes.

**Pacing.** Ask, recommending rounds:

- *Rounds* - the default. About ten candidates at a time: the user edits, you apply, repeat.
- *Autonomous fill* - only when the user asks for it ("just go", "don't check every batch"). Mine everything, apply what meets the bar in one pass, report at the end. Even then, stop for a translation conflict in paired material and for a write failure that repeats across terms.

## 3. Mine

External material first: terminology is densest there. Then the project's strings with `list_strings`, starting from the most active files (`list_files`). A term found in both is a strong candidate. With external material only, hold a higher bar, since nothing confirms the term appears where it will be translated. Mining is done when further strings stop adding new candidates.

## 4. Propose a round

One markdown table of about ten rows, sorted by category, then term:

```
| # | Category | Term | Definition | DNT |
|---|---|---|---|---|
| 1 | Brand | Plantly | The product name | yes |
| 2 | Feature | Care plan | The watering and feeding schedule set for one plant |  |
| 3 | Technical | Cutting | A piece of a plant grown into a new one |  |
```

Brand and Feature split the product and feature names, Technical is domain vocabulary, and Other covers ambiguous words and objects that must stay distinct. Add a translation column when paired material supplied translations. The table is the round itself, so it goes out once the setup questions are answered, at its real size. A larger first round is fine when the user asks for one.

Questions that came up while mining - which pages of an index to follow, whether to include third-party brands - go above the table. End with one instruction, in words: "Reply with the rows to drop (by number), any wording to change, or terms to add - I'll apply the rest as is."

## 5. Apply

Crowdin separates the *concept* - the idea, shared across languages - from its *terms*, one per language. For each approved candidate:

1. `add_term(glossaryId, languageId: <source>, text: <dictionary form>, partOfSpeech, type, status: "preferred")`. The response carries the `conceptId`. Leave `description` empty unless there is a note specific to this language.
2. `update_concept(glossaryId, conceptId, definition: <the approved definition>, subject?)` for every term. The concept's definition is what translators see, and a term without one is barely useful. For a do-not-translate entry, also set `translatable: false` here, and put "Do not translate" in the term's `note`.
3. `add_term(glossaryId, languageId: <target>, text: <translation>, conceptId, partOfSpeech)` for each translation lifted from paired material. The `conceptId` is what links it to the source term.

Brand names take `partOfSpeech: "proper noun"`, with the space.

When a call fails, name the term and the reason, continue with the rest, and change something before any retry. The same failure on several terms in a row (permission denied, for example) is structural: stop the round and tell the user.

Report in one line: how many terms were added, to which glossary, and which failed and why.

## 6. Continue

After each apply, ask how to go on: another round of the same size, a larger one, "the rest in one go" (one table, one confirmation), or stop. The round size changes only when the user changes it. When candidates get weaker, say so instead of proposing another round.

## Edge cases

- **The user switches glossary midway.** Treat it as a placement reset: confirm the new target, and agree whether the terms already added move with it. A silent split between two glossaries is found weeks later.
- **An attached CSV or TBX glossary export.** It is material like any other: its entries go through the same review table, with their translations and definitions lifted. `import_glossary` would merge it unreviewed.
- **A URL fails or returns nothing usable.** Say which one and why, and continue with the other sources.
