# Auto-Translation accuracy

**What it shows.** How often auto-translations were approved unchanged, and how much the others were edited, in sections for AI (per prompt), MT (per engine) and TM (all memories together): Total, 100% Match, {lowest}-99% Match, Avg. Match Score, No Match, Quality Score, and a chart by the auto-translation's date. The page lists at most 10 prompts and 10 MT engines, those with the most strings; the export includes all of them. No people, rates or costs.

**Settings that change the result.**
- The period, language, files and labels (project), projects (workspace), unit, and **Match Score Categories** (defaults, and the API without them: see Match scores in `references/reports.md`).
- **Tasks** replace the period, language, files and labels: the report takes the strings of each selected task in that task's language, with no date limit. Each task is counted separately and the page adds them up, so a string in two selected tasks counts twice. Enterprise lets you select several tasks; crowdin.com one task or all tasks that are not closed.

**How it counts.**
- *Counted:* strings whose current translation is approved and that have an auto-translation created in the period, no later than that approval. An auto-translation is a TM, MT or AI pre-translation, run by hand, automatically, by a workflow step, or added through the API as pre-translated.
- The auto-translation is scored against the reference, the current approved translation, as Match scores in `references/reports.md` describes: the approved translation is the auto-translation itself, or has identical text, for **100% Match**; otherwise its character similarity puts it in a band or in **No Match**.
- One result per string and language (or task). When a string has several auto-translations from the period (for example MT, then AI added alongside it) or several approved plural forms, their match scores are averaged, but the whole string is credited to only one of them: its section (TM, MT engine or AI prompt), its chart date, and whether it counts as 100% Match or edited. Which one is not fixed, so such a string can appear under MT as edited even though the AI translation was approved unchanged.
- Avg. Match Score averages the edited strings within the bands; Quality Score is roughly (unchanged + edited × their score) ÷ Total, No Match counting as zero.

**Common questions.**
- *A string has several translations.* Only auto-translations are evaluated, and several of them on one string are merged into one result as described above. Human translations count only when one is approved, as the reference. Votes only break ties when choosing the reference.
- *The string was translated in different ways.* Accepting an MT or AI suggestion in the Editor, typing, uploading and copying the source are human translations and are not evaluated. Editor use of MT and AI suggestions shows instead in Translation cost, whose MT and AI match rows say per member how close the saved text was to the suggestion.
- *Several people.* Not applicable: results belong to TM, an MT engine or an AI prompt, never to a person.
- *Approvals.* Not approved yet means not counted. A removed approval drops the string from the next run. Pre-translations approved automatically (TM pre-translation with auto-approve, AI Pipeline auto-approve) count as 100% Match without any human check. If the approved translation is replaced, the next run compares with the new one. On Enterprise one approval is enough.
- *Edited in place* (Replace in translations in the Editor, or editing a translation through the API), an auto-translation stays the same auto-translation, dated by the edit, so approving it afterwards gives 100% Match: the report never sees the text as it was auto-translated. Edited after its approval, it drops out.
- *Which date.* The auto-translation's creation date, or its last in-place edit; the approval may come after the period, as long as it is before the report is generated. With tasks selected there is no date limit.
- *Hidden strings* are counted.

**crowdin.com and Enterprise.** Same calculation; the reference choice and default bands differ as above. Enterprise adds the workspace level, which sums each language across projects.
