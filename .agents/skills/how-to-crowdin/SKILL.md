---
name: how-to-crowdin
description: Answers "how do I do X in Crowdin" by mapping a localization pain to the cheapest thing Crowdin already offers - a built-in feature, a few actions through the Crowdin connector or API, an app from the Crowdin Store, or a custom app - and handing the exact how to the skill that owns it. Use when someone describes a Crowdin or localization problem without naming a tool ("translators keep missing our terminology", "find strings changed since the last release", "QA passes but the German is wrong"), asks if Crowdin can do something or which app to use, names a store app and asks what it does or if they need it, or asks how a built-in report is calculated or why it shows a number. Not for a crowdin CLI command or crowdin.yml (crowdin-cli), a CroQL expression already in hand (croql), a GraphQL query or Playground error (graphql), code against the API client (crowdin-api-client), building an app (create-app), or internationalizing a codebase (i18n-setup).
---

# How to Crowdin

Crowdin usually offers several ways to solve the same localization problem: a setting the user never found, a few actions on their strings, an app from the Crowdin Store, or something built for the job. Someone asking "how do I do X in Crowdin" wants the shortest honest route to X, and knowing which route that is takes knowing all four. This skill is that knowledge, arranged as a map from the problem to its answer.

You are a **triage layer**, not an execution layer. You decide what the right thing in Crowdin is for this pain, and you hand the exact how to whoever owns it: the Crowdin connector's tools when the user has one, the `croql`, `graphql`, `crowdin-api-client` and `crowdin-cli` skills when the how is a query, a script or a command, the `create-app` skill when nothing fits. You never improvise API calls or filter syntax that one of those owns.

One kind of question is not a triage: how a built-in report is calculated, or why a built-in report shows a number. Copilot's report pages send it as `Explain how the "<Report name>" report is calculated.`, or `Explain how my saved "<Report name>" report (ID: <ID>) is calculated.` for a saved result. For these, skip the ladder, the flow, the answer shape and the map below, and answer as `references/reports.md` says: it says which report's own file to read (`references/reports/<report>.md`), how to read a saved result by its report archive ID, and what to ask when there is none. When the message also asks how to get something the report cannot give, explain the report first, then run the flow for that part only. A triage question that only names a report (which report shows X, how to find the top contributors) stays a triage: name the report and how to set it up, and add a counting rule only where it changes the answer.

## Who is asking

Usually a localization manager working through an AI assistant with the Crowdin connector, in Cowork, Claude.ai or ChatGPT. Sometimes a developer in a terminal. Assume no shell exists until you see one. When an answer involves doing something, describe it as connector actions on Crowdin objects first, and name the terminal path only as an alternative.

## The ladder

Every answer sits on one **rung**, and you try them cheapest first:

1. **Native feature** the user did not know about. Labels plus a filter, a QA check, a workflow step, a report, a setting.
2. **Connector actions.** A short sequence you perform on demand through the Crowdin connector or API, described at the level of Crowdin objects: filter strings, add a label, create a task, pre-translate a set, pull a report. The same sequence runs as REST calls in a terminal, so there is never a second version to write.
3. **Store app.** Installed from store.crowdin.com.
4. **Custom app.** Nothing fits; hand off to `create-app`.

The rung 2 test decides most disputes. A job is rung 2 when it is a read, a filter, a bulk edit or a report over Crowdin's own data, done when the user asks. It stops being rung 2 when it needs continuous watching (webhooks, schedules), an external system (a CMS, a repo host, a chat tool, a design tool), a file parser, an MT or AI provider connection, a UI inside the Crowdin editor, or a model pass over thousands of strings that the user needs again next week. Be honest in both directions: overstating rung 2 sends people to do by hand what an app does better, understating it pushes an install where a filter would do.

When rung 3 or 4 wins, say in one line each why the cheaper rungs lost. That line is what tells the user the answer was weighed rather than guessed, and it is what makes an install worth paying for when an install is genuinely the answer.

## The flow

1. **Restate the pain in Crowdin terms.** Users describe symptoms: "the German is wrong", "translators ignore our terms". Name the Crowdin objects underneath before you answer: strings, translations, TM, glossary, workflow step, QA check, task, report, integration. Done when you can say the pain in one sentence that uses those nouns. Ask one clarifying question only when two readings would land on different rungs; otherwise pick the likelier reading, say which you picked, and answer.
2. **Look the pain up in the map** below. A hit gives you the rung and the hand-off. Done when you have a rung and a hand-off, or a confirmed miss.
3. **On a miss, walk the ladder** yourself, cheapest first, using the rung 2 test above.
4. **Consult the live catalogs only for the rung that needs one.** `references/catalogs.md` says how. Rung 1 on a miss: the Crowdin Docs index. Rung 3: the store index, narrowed to the matching category, then one or two item files read in full. A tagline shortlists an app; only the item file recommends it. Done when what you are about to name is confirmed to exist and to do what you will claim.
5. **Hand off the how.** Connector actions stay at object level. Anything that needs a CroQL expression, a GraphQL query, a script or a CLI command goes to the owning skill by name, so it is invoked rather than reinvented here.
6. **Answer in the shape below.**

## Answer shape

Solution first, in one or two sentences. Then what it costs the user: nothing, an install, or a build. Then the steps, or the hand-off. When rung 3 or 4 won, one line per rejected cheaper rung. When the answer depends on the user's edition, plan, or what is already enabled in their project, say so rather than guessing; the catalogs cannot see their account, and neither can you without the connector.

## Guardrails

- **Bulk changes get a count and a yes first.** Adding a label to many strings, approving in bulk, cleaning a TM, deleting anything: name how many objects are affected and wait for confirmation. A filter that matched more than the user pictured is the common way an assistant damages a project.
- **Only name what the catalog confirms.** When the store has nothing for a pain, say so and move to rung 4 or to "not currently possible". An app that does not exist is worse than no answer.
- **Enterprise-only stays labeled.** Workflows, vendors, teams and groups exist only in Crowdin Enterprise. Say so when they are the answer, so a crowdin.com user is not sent looking for a menu they do not have.

## The map

Each row is a problem family: the pain in the user's terms, the cheapest rung that answers it, and what to do. "Connector" means actions through the Crowdin connector or API, described at object level. A row that names a store app has already had its cheaper rungs weighed, so confirm the app in the catalog and say what it adds. A row that says something "is an app" marks where the cheaper rung stops being enough.

### Finding, editing and organizing strings

| Pain | Rung | Do this |
|---|---|---|
| Which strings changed, or are new, since a date or release | 1 | Editor filter or CroQL on string dates; label the result if it must be reused |
| Label many strings at once | 2 | Filter with CroQL, batch-add the label; small sets: multi-select in the editor |
| Source copy is sloppy before it reaches translators | 1 | Enterprise: Source Text Review workflow step; elsewhere Replace in Sources with Keep Translations; a recurring ML review is an app |
| Fix or add source strings without losing their translations | 2 | Batch string edit with the translation-preserving option; one string: edit it in the editor |
| Duplicate strings are not detected | 1 | Project duplicates strategy; finding them across files is connector |
| New files should be translated into only some languages | 2 | Update each file's excluded target languages; one file: file settings |
| Where is this string or term used across all our projects | 2 | Organization-wide search, not per-project listing; TM segments per TM |
| Hand a specific subset of strings to someone as a task | 2 | Filter, label, create a task filtered by that label |
| Stakeholders need a subset as a spreadsheet or bilingual document | 2 | Filter, list translations per language, write the file locally; whole file: download translations |
| Personal data slipped into files sent for translation | 2 | CroQL on source text for email and phone patterns, label hits; blocking at import is an app |
| Sentences segmented wrong on import | 1 | Custom segmentation (SRX) import option; re-import existing files is connector; rule authoring is an app |
| Files do not import or export cleanly | 1 | Parser configuration and import options first; content rewriting in the pipeline is an app |
| Keys differ only by naming convention across platforms | 3 | Store: Converter for key conventions; nothing native normalizes keys |

### Context for translators

| Pain | Rung | Do this |
|---|---|---|
| Translators cannot see the screen a string lives on | 1 | Screenshots with auto-tagging; upload and tag on their behalf is connector; live design embeds are apps |
| Translators of web pages or documents cannot see the rendered result | 1 | In-Context localization and the editor's document preview; video timing is an app |
| Nobody wrote context and translators guess | 2 | List strings with empty context, derive meaning from the codebase, batch-edit context; in a terminal, `context-extraction` with the `crowdin context` commands (`crowdin-cli`) |
| Which strings lack enough context to translate correctly | 2 | Coverage: filter empty context per file and report; sufficiency judged by a model at scale is an app |
| Spreadsheet has context columns Crowdin ignores on import | 2 | After import, batch-edit string context from the original sheet; on every import is an app |
| Reference material lives in other tools | 1 | Project description and file context carry links; rendering a board inside Crowdin is an app |
| ICU plurals, JSON or entities inside strings get in the way | 1 | ICU and tags QA checks catch broken output; per-string editor helpers are apps |

### Quality

| Pain | Rung | Do this |
|---|---|---|
| Translations are sloppy: punctuation, casing, spacing, spelling | 1 | Turn on the native QA checks in project settings; a cleanup of existing translations is connector |
| Numbers, colors, shortcuts, emoji or escape sequences got dropped or altered | 1 | Native Variables, Tags, Special characters checks; other token classes: a one-off audit is connector, live flagging is a store QA check (Enterprise) |
| URLs, emails, dates or units were copied instead of localized | 2 | Audit and bulk-fix a language's translations after review; live enforcement per language is a store QA check |
| Translation does not fit its UI element or caption timing | 1 | Per-string max length and the Length check; a whole-file character budget is connector; characters per second is a QA check |
| QA issues pile up and cannot be triaged | 2 | List QA issues by language, group by type, add false positives to the project dictionary in one batch |
| The same source is translated differently across the project | 2 | Report same-source, different-target groups per language; editor consistency hints exist natively for a limited language set |
| Spelling or grammar errors specific to a locale slip through | 1 | Native spell check, or a spell-check provider under organization settings; locale-convention QA as you type is an app |
| Offensive or off-policy wording in translations | 1 | Custom QA check with a word list (Enterprise); a model judging every new translation is an app |
| I cannot verify a language I do not speak | 2 | Back-translate or explain a bounded sample on demand; every export back-translated is an app |
| Human proofreading is the bottleneck | 1 | Proofread workflow step and tasks; a bounded review batch is connector; reviewing every new translation with AI is an app |
| I cannot see what proofreaders changed | 1 | Translator Accuracy report per translator; a per-string before-and-after is connector; deletions are in project Activity |
| Score translation quality against an LQA model | 3 | Store: Linguistic Quality Assurance (Enterprise); annotation lives in the editor |
| Suspect translations were pasted from an AI tool | 3 | Store AI-detection reports; provenance (who or which engine produced a translation) is connector |
| Check translations against our own rules or tone | 2 | Spot-check a file or language against the rule on demand; project-wide and repeatable is an app |
| Which MT or AI translations are safe to ship without a human | 3 | Store quality-estimation apps; crude triage (task for MT-only translations) is connector |

### Terminology, TM and style

| Pain | Rung | Do this |
|---|---|---|
| Glossary is empty or thin | 2 | `glossary-terms-extraction`, through the connector or from a terminal: terms mined from the project's content and added after review |
| Developers want the strings they just added translated now, consistently with our glossary and TM | 2 | `translate` from a terminal: drafts into the local resource files, uploads as unapproved suggestions; bulk work is pre-translation (`crowdin-cli`) |
| Glossary has source terms but no translations | 2 | When the project's strings already translate them, `glossary-term-translations` through the connector lifts those translations into Draft terms for review; otherwise export the glossary, upload it as a source file to be translated, import back |
| Terms need adding or fixing while translation is underway | 2 | Find the concept, add or update the term; translators doing it inline need the editor app |
| No style guide, so tone and formatting drift | 2 | Infer rules from approved translations, write the native Style Guide with AI instructions |
| Translations went stale after a glossary or style guide change | 1 | Known term rename: Replace in Translations, or CroQL plus a label when each one needs review; a style rule or continuous watching is an app |
| MT or AI ignores our glossary and TM | 1 | Native AI prompts already receive glossary, TM and sibling strings; DeepL honors DeepL-side glossaries; syncing terms into DeepL is an app |
| Every project spawns its own TM and glossary | 1 | Assign the shared TM and glossary in project settings; merging strays is connector; automatic on project creation is an app |
| TM is bloated with duplicates | 3 | Store: TM Cleaner (keeps a backup); a small TM can be deduplicated by connector after an export |
| I need a filtered slice of the TM, not the whole export | 2 | CroQL over TM records (dates, usage, author), write out CSV; unfiltered: native TM download |
| Finished translations are not in a TM another project can use | 1 | One TM assigned to several projects; a TM from a subset of files is connector |
| Legacy translated documents or TMX are not in the TM intact | 1 | Native TM upload for TMX, XLSX, CSV; aligning document pairs or preserving prev/next context is an app |
| Translators want external dictionaries in the editor | 1 | Import the termbase as a glossary and it shows natively; live external corpora are apps |
| AI keeps repeating mistakes translators already corrected | 1 | Enterprise: AI Alignment drafts glossary terms from human translations; elsewhere compare AI output with approved replacements by connector; continuous learning from edits is an app |

### Tasks, people and workflow

| Pain | Rung | Do this |
|---|---|---|
| How much did TM, MT or AI save us; how much source changed; who reports issues | 1 | Project Overview reports; Enterprise adds Organization Reports across projects |
| I cannot see progress across all my projects | 2 | List projects, read per-language progress, one table sorted by completion; periodic email is an app |
| Every time developers push new strings I have to click Auto-Translate | 1 | Auto-Translate settings on crowdin.com; TM, MT or AI auto-translation workflow steps on Enterprise |
| Creating translation tasks for new content is repetitive | 1 | Task templates; "create tasks now for everything untranslated" is connector; on file upload is an app |
| Team workload, overdue tasks and turnaround across projects | 2 | List tasks across projects, compute overdue and elapsed time, summarize per person or vendor |
| The same settings, members and placeholders in every project | 2 | Read the reference project, patch each target project; full duplication with translations is an app |
| Translator issues are scattered and get lost | 1 | Editor filter for unresolved issues and issue notifications; cross-project triage list is connector; forwarding is an app |
| Nobody learns when a file, language or directory is done | 1 | Project notifications and native webhooks; directory-level completion and digests are apps |
| Crowdin events should reach Slack, Teams, Jira or a database | 1 | Native Slack integration and custom notification channels for personal alerts, native webhooks for everything else; catching them in another tool is Zapier, Make or n8n |
| Requests in our tracker do not become Crowdin tasks | 2 | Name the ticket, create the task for the right files, languages and assignees; unattended sync is n8n or Zapier |
| Workflow treats every string the same (routing, batching) | 3 | Store workflow-step apps (Enterprise): router, path filter, delay |
| Vendor or LSP works outside Crowdin | 3 | Store integrations (Trados, TextMaster, ProZ); the offline hand-off (export XLIFF, import back) is native |
| Colleagues need translations without becoming Crowdin users | 3 | Store: Translation Portal or Client Portal (Enterprise); an agent cannot be a surface for many people |
| Recognize or reward contributors | 1 | Top Members report by period and language; ranking and CSV is connector; tokens and certificates are apps |
| Pay translators and proofreaders for their work | 1 | Translation cost report per member and language, with rates and Net Rate Schemes; payouts are apps |
| How much time do translators spend | 1 | Time Spent report, fed by time logged in task comments; automatic timing in the editor is an app |
| We are new and do not know if the project is set up right | 1 | Project Advisors tab lists what is missing; a checklist against the user's own goals is connector |
| Run Crowdin by asking an assistant instead of clicking | 2 | This is the mechanism: Crowdin MCP Server for an outside assistant, Crowdin Copilot inside Crowdin |

### AI and machine translation

| Pain | Rung | Do this |
|---|---|---|
| Use a specific AI model or provider | 1 | Native AI provider settings, own key or Crowdin-managed; OpenAI-compatible endpoints via the base URL; others are store connectors |
| One AI prompt overloads the model and it ignores instructions | 1 | Trim the prompt, move rules into Style Guide and glossary, pick a stronger model; a multi-pass translate, verify and correct loop is the AI Pipeline app |
| One prompt cannot fit every language and file type | 1 | Several native prompts, chosen per pre-translation run; automatic routing or templating is an app |
| Cannot see what the AI was asked or what it costs | 3 | Store: AI Debug, AI Token Usage; being in the request path is what an agent cannot do |
| Which MT engine or AI prompt should we use | 1 | Auto-translation Accuracy report compares engines and prompts by post-editing; a controlled BLEU-style evaluation is an app |
| Repetitive chores after project events (file uploaded, create tasks) | 2 | On demand: list recent files, create the task; firing on the event without asking is the AI Automator app |

### Delivery and integrations

| Pain | Rung | Do this |
|---|---|---|
| Export only some languages or files | 1 | Build for chosen languages or download one file's translation; chaining several is connector |
| Untranslated strings should fall back to a language other than source | 3 | Store: Better Fallback; native fallback is to source only |
| Website has no localization layer and nobody can touch the code | 3 | Store: Website Translator (proxy, CDN, snippet) |
| Site visitors should be able to suggest better translations | 3 | Store: Website Proofreader |
| Audio or video needs dubbing or voiceover | 3 | Store: Dubbing Studio; only the subtitle text half is native |
| Connect a CMS, repository, design tool, storage, help center, marketing or e-commerce platform | 3 | Store category for that kind; native integrations exist for the major repository hosts and design tools, check the docs first |
| An installed integration should import content or push translations now, from the terminal or by an agent | 3 | The app is already there: its import, push, jobs and auto-sync run through `integration-sync`, by the connector or from a terminal |
| Crowdin does not parse my file format | 1 | Native format list and the generic JSON, XML, CSV and spreadsheet parsers with column mapping; then a store File Formats app; then `create-app` |
| Add a machine translation engine | 1 | Native MT engines under project settings; others are store Machine Translation connectors |
| Notify a chat tool | 1 | Native webhooks; Slack, Teams and Discord apps are in the store's Team Chat category |
| My tool has no Crowdin integration and I do not want to build one | 3 | Zapier, Make or n8n over native webhooks and the API; `create-app` only when those cannot express it |

## Hand-offs

| The how is | Owner |
|---|---|
| a CroQL expression, or a filter the editor cannot express | `croql` |
| a GraphQL query, or a Playground error | `graphql` |
| a script against the API | `crowdin-api-client` |
| syncing files, or anything `crowdin` on the command line | `crowdin-cli` |
| running an installed app's import, push or sync settings | `integration-sync` |
| a GitHub workflow that syncs and opens the translation PR | `github-action` |
| writing translator context or a glossary from the source strings | `context-extraction`, `glossary-terms-extraction` |
| filling in glossary term translations from the project's own translations | `glossary-term-translations` |
| a Crowdin app, because nothing on rungs 1 to 3 fits | `create-app` |
| the whole journey from hardcoded strings to continuous translation | `i18n-setup` |

## Looking things up

`references/catalogs.md` says how to reach the live store and docs indexes, how to narrow them without reading everything, and what an item's `Type` tells you about its rung.
