# How the built-in reports are calculated

The rules behind Crowdin's seven built-in reports: Cost estimate, Translation cost, Auto-Translation accuracy, Translator accuracy, Task usage, Time spent and Top members. This file holds the steps for answering, the report table and the rules every report shares; each report has its own file under `references/reports/` with what it shows and counts, which settings change the result, and how the usual "why is this number what it is" questions resolve. Crowdin Store app reports, the Overview page and AI usage reports are not covered: for those, go back to the flow in SKILL.md, and explain a Store app's report from its item file in the store catalog (`references/catalogs.md`).

## Using this file

1. **Identify the report and read its file.** The table below maps the page title, the API name or the symptom to one report and its file. The ID in the message is the report archive ID. The project, if any, comes from the page Copilot was opened on, not from the text; a workspace or organization report names no scope. Read that report's file and the shared rules below; open another report's file only when the question compares two reports or the report's file sends you there. Done when you know the report and have read its file.
2. **Pin the product and the level:** crowdin.com or Crowdin Enterprise, project or workspace (Enterprise only). When the answer differs and you cannot tell, give both. Done when you know both, or will answer for each.
3. **Read the archive when there is an archive ID.** On crowdin.com use `get_report_archive`; on Crowdin Enterprise, whose archives belong to the organization, use `get_organization_report_archive`. It returns the archive's `name`, `scopeType`, `scopeId`, `createdAt` and `scheme`, the settings it was generated with (dates, unit, languages, rates, options; Time spent leaves some out, see its file). For the numbers, export the archive, wait until the export's status says it finished, and only then download it: a download started earlier can return an unfinished export. On crowdin.com that is `export_report_archive`, `check_report_archive_export_status` and `download_report_archive`. On Enterprise, `export_report_archive` does not work (it exports crowdin.com archives only): use the connector's organization export tool if it has one, or else start the export through the API (`POST /api/v2/reports/archives/{archiveId}/exports`), then use `check_organization_report_archive_export_status` and `download_organization_report_archive`. With neither, ask the user to export the saved report from its page (Export) and share the file. An archive is a snapshot that is never recalculated, so generating the report again today can give different numbers. Without the Crowdin connector (or the API in a terminal), say that reading report <ID> needs the connector, explain the rules from the report's file, name the settings whose value decides each rule, and state no setting or number you have not read. Done when you have the archive's settings and, when the question is about its numbers, the downloaded result, or have said what reading it needs.
4. **Without an archive ID, ask before you compute.** Task usage and Top members are never archived, and a report that was not generated or not saved has no ID. How a report is calculated needs no data: answer that from the report's file without asking for any. To explain the user's own numbers, first ask what their page shows: the period, the language and other filters, the workspace or projects when the report is not in a project, and which number puzzles them and its value. Say why you ask: there is no saved result to read back. Skip what their message already says. Then generate the same report through the connector or the API with those settings (API report names are in the table) and explain from that result in step 6. Never generate with defaults and present the result as theirs: without dates an API report covers everything since the project or workspace was created, while the page has its own default period (see The period). Even with the same settings the result can differ from the page: check the report's file for what the API counts differently (Task usage's languages, for one), and data changed since the page was generated can change the numbers too (each report's file says which); say so when they do not match. Done when you have the user's settings and a result generated with them, or the question needs no data.
5. **Open the documentation only for what these files do not cover.** For anything this file or the report's file states (how something is counted, a default, who sees what), follow it: it was checked against the product rule by rule in October 2026, while the documentation describes reports more in outline, so do not repeat a documentation statement that contradicts it. When the question, or a setting in the archive's `scheme`, names something neither file covers, such as a setting, option, column or report, read the report's section in Crowdin's documentation and follow it on that point: it may be newer than these files. Fetch one page, chosen by product and by where the report was opened:
   - crowdin.com, in a project (any report but My Contribution): `https://support.crowdin.com/project-reports/`
   - crowdin.com, My Contribution: `https://support.crowdin.com/contributor-reports/`
   - Enterprise, in a project (any report but My activity): `https://support.crowdin.com/enterprise/project-reports/`
   - Enterprise, outside a project (a workspace or the whole organization; the documentation calls it Organization Reports): `https://support.crowdin.com/enterprise/organization-reports/`
   - Enterprise, My activity: `https://support.crowdin.com/enterprise/contributor-reports/`

   Read the section headed with the report's name (My Contribution and My Activity have their own). A fetch tool ignores the `#` part of a URL, so name the section in the request and ask for its settings, options and columns in the page's own words. Task usage has no section on the crowdin.com pages, and Cost estimate none on the organization page. When step 2 left the product or level open, fetch the page for each. Do not use the documentation text sets from `references/catalogs.md` here: they run to megabytes and keep the crowdin.com and Enterprise pages under the same titles.

   The user's own numbers outrank both: when an archive or a report generated with their settings contradicts a rule in these files, say the files may be out of date and explain from the numbers. Done when nothing in the question falls outside these files, or you have the documentation's words for what does, or know it does not cover it either.
6. **Answer from the report's file**, and from the documentation for what step 5 found there: the rule first, then the user's case or numbers. When a rule depends on a setting, name the setting and its value in the archive or in the settings the user gave. When something is not counted at all, say so plainly. Done when every number or behavior the user asked about is tied to a named rule, and to the setting's value where the rule depends on one, or is named as depending on a setting whose value could not be read, or as something neither these files nor the documentation answers.
7. **Say so rather than guess** when neither these files nor the documentation answers a question. These reports often behave differently from what their column names suggest, so a gap filled from intuition is usually wrong.

| Report | File | Levels | API report name (project / workspace) | Archived |
|---|---|---|---|---|
| Cost estimate | `references/reports/cost-estimate.md` | project | `costs-estimation-pe` / none | yes |
| Translation cost | `references/reports/translation-cost.md` | project, workspace | `translation-costs-pe` / `group-translation-costs-pe` | yes |
| Auto-Translation accuracy | `references/reports/auto-translation-accuracy.md` | project, workspace | `pre-translate-accuracy` / `group-pre-translate-accuracy` | yes |
| Translator accuracy | `references/reports/translator-accuracy.md` | project, workspace | `translator-accuracy` / `group-translator-accuracy` | yes |
| Task usage (Enterprise only) | `references/reports/task-usage.md` | project, workspace | `task-usage` / `group-task-usage` | no |
| Time spent | `references/reports/time-spent.md` | project, workspace | `time-spent` / `group-time-spent` | yes |
| Top members | `references/reports/top-members.md` | project, workspace | `top-members` / `group-top-members` | no |

crowdin.com capitalizes the page names (Cost Estimate, Top Members). Members who are neither managers nor language coordinators see Translation cost as My Contribution (crowdin.com) or My activity (Enterprise), and Cost estimate only by task: it covers all of the task's strings, not only their share. Auto-Translation accuracy archives saved before match scores existed show older metrics (Edit rate, Average Edit Distance), not covered here, under the same API name; `pre-translate-efficiency` is only a deprecated API alias that generates the current report.

## Rules shared by all reports

### Two kinds of data

- **A record of actions.** Translation cost and Top members read their own records, written when someone saved a translation, approved one, or voted (not the project's Activity feed). Deleting the translation, removing the approval or cancelling the vote later removes nothing, so the work still counts.
- **The current state.** Cost estimate, Auto-Translation accuracy and Translator accuracy read strings, translations and approvals as they are at generation time, so a removed approval or deleted translation changes the next run (a Cost estimate generated again with the same settings can reuse a recent result; see its file).
- Task usage reads tasks, and Time spent reads time logged in task comments; neither looks at translations.

### Units

- Words, strings and characters are counted on the **source text**, not the translation; the only exception is Top members' Target column.
- Each Chinese or Japanese character is a word, a URL is one word, punctuation alone is not a word. Tags are left out according to the project's tag detection setting. Characters never include line breaks; "characters" also leaves out spaces and tabs.
- Plural strings: in Translation cost, Translator accuracy and Top members every translated plural form is a separate entry that carries the volume of the string's main source text (the other source forms are not added), so in the strings unit one plural string counts once per form. Cost estimate and Auto-Translation accuracy count a plural string once.

### The period

- The UI sends whole days in the viewer's time zone and defaults to the last 30 days (Cost estimate: since the project was created). Through the API dates are UTC, and without them the period runs from the project's or workspace's creation to now. Both ends are included.
- Each report decides with its own date:

| Report | The date that puts something into the period |
|---|---|
| Cost estimate | the string was added or its source last changed |
| Translation cost | the person's first save of a translation for that string; the approver's first approval |
| Auto-Translation accuracy | the auto-translation was created, or last edited in place |
| Translator accuracy | the person's translation was created, or last edited in place |
| Task usage | the task was created; the Resolved line uses the task's resolved date (set when it is first marked Done or Closed, or entered on the task) |
| Time spent | time was first logged on that task comment |
| Top members | first translation, first approval, first vote; votes received use the voter's first vote on the string, and Winning the approver's first approval of it |

### Rates and currency

- The currency is a label. Nothing is converted.
- A rate is a price per selected unit (per hour in Time spent). A Net Rate Scheme row is a percentage of the full translation rate; 101% (perfect) means a 100% match whose context also matches. A match outside every row is No match, priced at 100%.
- Custom rates are checked in list order and the **first** matching row wins, even when a more specific row follows. A row for a member and language matches; so does a row for a member with no languages, or for a language with no members.
- Rate templates only fill the form; an archive's `scheme` holds what was actually used.

### Match scores (Auto-Translation accuracy and Translator accuracy)

- Both accuracy reports score a translation against the **reference**, the string's current approved translation. Enterprise picks the one with the most approvals, then votes, then the latest approval; crowdin.com the one approved at the furthest workflow step, then the latest approval, then votes.
- A translation that is the reference, or has identical text, is a **100% Match**. Otherwise the match score is the character similarity of the two texts, 100% × (1 − changed characters ÷ length of the longer text), rounded down; case, spaces, punctuation and tags count, and a text over 3000 characters scores 0. The score falls into a Match Score Category, or into **No Match** below all of them.
- **Match Score Categories** default in the web UI to 99-90% on Enterprise, and to 99-90% and 89-80% on crowdin.com. A report generated through the API without `matchScoreCategories` has no bands, so everything below 100% is No Match; the archive's `scheme` shows the categories actually used.

### Access, languages and labels

- Members who are neither managers nor language coordinators see only their own work in Translation cost and Time spent. A language coordinator sees everyone's work in their languages, except in Time spent on crowdin.com.
- At project level, "all languages" means the project's current target languages, or a language coordinator's own languages, so work in a language later removed from the project drops out. Top members and the Task usage page (not its export) ignore both limits.
- At workspace level "all languages" has no such limit, except in Auto-Translation accuracy and Translator accuracy, which cover only languages that are still a target of at least one of the workspace's projects.
- Label filters: "with selected labels" keeps strings that have all of them; "without selected labels" drops only strings that have all of them.

### Workspace level (Enterprise)

A workspace report covers the projects of the workspace and its nested workspaces; the organization level covers all projects. The rules are the same, one person's numbers are summed across projects, and there are no file, branch, label or workflow step filters. Cost estimate has no workspace level.
