# Time spent

**What it shows.** Time members logged on tasks in the period and its cost at your hourly rates.

**Settings that change the result.** The period, tasks, language, task type, members, the Workflow step filter (Enterprise project), projects (workspace), the base hourly rate (0 by default) and custom hourly rates by member and/or language. Group by changes only the layout, except for time from deleted tasks (below); the currency is only a label.

**How it counts.**
- **Nothing is tracked automatically.** The report sums only time typed into **Log time** on a task comment, or sent as time spent with a task comment through the API. Crowdin has no Editor timer of its own and no per-string time.
- Each comment is one entry, credited to the comment's author, with the task's language, project, type and workflow step.
- Cost of a row = seconds × hourly rate ÷ 3600, rounded to 4 decimals; totals add the rounded rows. The UI shows time rounded to minutes; exports show it exactly.

**Common questions.**
- *Several translations, translation methods, approvals.* Not applicable: the report never looks at translations, and automatic work logs no time.
- *Several people on a task, or one person coming back.* Each entry counts for whoever logged it, managers included; a person's entries add up, with no deduplication or cap.
- *Which date.* When time was first logged on that comment. Changing the amount later keeps the entry in its original period; setting it to zero or deleting the comment removes it from every report generated afterwards, past periods included, while archived results keep it.
- *A deleted task* keeps its logged time. Grouped by task, all of it appears in one row labelled REMOVED_TASKS, where per-language rates cannot apply. Selecting All tasks lists existing tasks, so that time drops out.
- *The task page differs:* the time next to an assignee's name is all the time that person ever logged on that task, with no period; the report covers the period, its filters and every author, assigned or not.
- *Reading an archive.* Its saved settings never show the language or task type filter, and when tasks were selected they also leave out the period, members and workflow step, although all of these were applied. A missing value there does not mean the filter was off; say it is unknown.

**crowdin.com and Enterprise.** Same calculation. Managers see everyone's time; on Enterprise, project language coordinators do too, limited to their languages. Other members, including language coordinators on crowdin.com, see only their own time. Enterprise adds the workspace and organization levels and the Workflow step filter.
