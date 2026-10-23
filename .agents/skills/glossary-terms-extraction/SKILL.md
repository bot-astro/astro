---
name: glossary-terms-extraction
description: Builds or expands a Crowdin glossary from the project's real content. Use when someone wants a glossary created or topped up, asks which terms to add or what is missing, or wants product and feature names found in their strings, docs or files and marked do-not-translate. Not for adding one named term, filling in term translations from the project's own translations (glossary-term-translations), importing or exporting a glossary file as is, editing existing entries, or the full internationalize-and-connect journey (i18n-setup).
---

# Glossary terms extraction

Translators meet a product's vocabulary one string at a time, with no way to tell which words are the product's own. A glossary is where a term gets decided once - translated one way everywhere, or not translated at all. This skill finds the terms worth deciding in the user's real content and adds them only after the user has reviewed them.

There is no target size. Mine for terms a translator would otherwise get wrong, and stop when the candidates get weaker: when you notice you are reaching for filler, say so and let the user choose between more material and stopping. A short, sharp glossary beats a long, padded one. A padded glossary gets ignored or deleted wholesale, taking its good entries with it, and a wrong entry teaches every translator the wrong answer. When in doubt, leave the term out.

## Pick the path

How terms are read and written depends on what this session has:

- **Crowdin connector tools** - `list_glossaries`, `add_term` and the rest of the Crowdin MCP server, as in Crowdin Copilot or an assistant with the Crowdin connector. Read and write through them, and review in rounds: follow `references/connector.md`.
- **A shell with the Crowdin CLI and a `crowdin.yml`**, or a calling workflow that hands over a source catalog path (`i18n-setup` does). Draft a CSV, review it as a whole, and upload it with `crowdin glossary upload`: follow `references/cli.md`.

With both available, take the connector path unless the user or a calling workflow asked for the file. With neither, draft the reviewed list in the conversation and name both ways to add it.

Everything below holds on both paths.

## Material to mine

Terms come from real content, never from a product name alone:

- **The project's source strings** - what translators will actually see.
- **Reference material the user gives**: a product or docs site, a help center, attached files (PDFs, documents, an existing glossary export), pasted text. Canonical pages name brands and features more cleanly than strings mention them, so ask for such material once when a project is in scope, together with the first thing you need from the user anyway. With no project, it is the only source. Fetch only the pages the user gave; when one turns out to be an index of many pages, say so and ask which to follow.
- **A one-line product description.** It is what makes "brew" a domain term in a coffee-logging app and an ordinary verb anywhere else. Ask for it when nothing in the conversation or repository supplies one.

With no project and no material, ask for at least a URL, a document or a paragraph about what is being localized. A user who only wants to brainstorm from a name gets that, labeled as guesses, with an offer to revisit once real content exists.

Read the existing glossary before mining: a term already in it is not a candidate. When it already holds dozens of entries, say how many and ask whether to keep mining at a higher bar.

## What earns an entry

A term qualifies when a translator, seeing it in one string with no other context, could reasonably get it wrong. In practice that is four kinds:

- **Product and feature names.** Brand, plan, module and other named surfaces of the user's own product - usually the entries that say *do not translate this*. The most valuable kind and the one most often missing.
- **Domain vocabulary the content leans on.** Words with a specific meaning in this product's world, particularly ones that recur.
- **Words that are ambiguous out of context.** English noun/verb pairs are the classic case: on its own, a `Log` label could be a record or an action.
- **Objects that must stay distinct.** When the content separates two similar things, translating both with one word collapses a distinction it depends on.

Recurrence is the strongest signal: a term in two or more strings is a candidate on frequency alone. A term that appears once needs one of the other reasons - a brand name earns its entry the first time it is used.

Leave out ordinary words every translator already handles (`Save`, `Cancel`, `page`, `article`, `level`) unless this project gives one a specific meaning; whole sentences or messages, which are strings to translate rather than terms; and anything you would be guessing at. When a term is worth having but the content supports two meanings, ask which one the product uses. Third-party brands in the content (Stripe, HubSpot, WordPress) are do-not-translate, but translators usually know them: ask once whether to include them rather than filling the list with them.

## Writing an entry

- **Dictionary form.** Nominative singular for nouns, infinitive for verbs, not whatever inflected form the source shows. Heavily inflected languages spread one noun over many surface forms; they are one term, and Crowdin matches the inflections itself.
- **A description that answers a practical question**: what this is in this product, and whether to translate it. "The paid tier; a plan name, keep in English" helps. "A plan is an intention to do something" does not.
- **Translations only from paired material** - the user's own content with its translation: a file and its translated version, a site and its `/de` mirror, a bilingual export. Lift each term's translation from it, and check that every occurrence maps to the same target word. Inflections of one word are fine; a different word or root is a conflict to show the user with both options, because the glossary's choice becomes every future translator's answer. Without paired material, entries are source-language only, including when the user asks to fill translations in: a machine-guessed glossary translation carries the authority of a decision. Offer the source-only entries, and translations later from paired material. In a Crowdin project whose strings are already translated, those translations are the paired material: add the source terms here and, when the Crowdin connector is available, hand their translations to `glossary-term-translations`, which lifts them from the project's final translations.

## The review gate

Nothing reaches the glossary unseen. The user reviews the candidates, and their edits win: a reworded description or a changed do-not-translate choice is the entry. A declined write is a complete outcome, not a failure.
