# Task usage (Enterprise only)

**What it shows.** Statistics about **tasks**, not translations, in five views with their own filters and exports: **Workload**, **Created vs resolved**, **Task performance**, **Task completion time** and **Task cost**. Not archived.

**Settings that change the result.** The period, group by, language, task type, created by, assignee, task size in words (completion time), statuses (Task cost), projects (workspace). On the page, all languages covers tasks in every language; a project-level export or API report covers only the project's current target languages (or a language coordinator's languages).

**How it counts.**
- Each task counts once in each figure; only Workload grouped by member counts it for each of its assignees. The period selects tasks by **creation date** in every view; only the Resolved line of Created vs resolved selects by the task's resolved date.
- Words are the task's size recorded when it was created or its strings last changed, not the live word count on the task page.
- *Workload:* tasks created in the period, by current status (To Do, In Progress, Done, Closed); Total also includes Pending and For Approval tasks, so it can exceed the four columns, and their words are in neither volume. Active volume is the words of To Do and In Progress tasks, throughput volume of Done and Closed. Grouped by member, a task counts fully for each assignee.
- *Created vs resolved:* tasks created and tasks resolved in the period. The page's graph shows running totals from the start of the period (each point is everything created or resolved up to that day, or month for long periods); the export and the API give per-day counts. The comparison with the previous period shows only in the UI.
- *Task performance:* tasks with a due date. Open overdue: still open and past due when the report is generated. Closed on time or overdue: resolved before or after the due date (exactly at it counts as neither). On-time rate, on the page only: closed on time ÷ (closed on time + closed overdue), rounded to a whole percent.
- *Task completion time:* completion = resolved − created, waiting = started − created, active work = resolved − started, overdue = resolved − due date for tasks resolved after it. Completion time is shown as average, median, 80th percentile and maximum; waiting, active work and overdue time as averages. Each figure uses only the tasks that have the dates it needs.
- Started is set at the first move to In Progress, Done or Closed, and resolved at the first move to Done or Closed; reopening resets neither, but both dates can be set or changed on the task (started and resolved dates) or through the API, and the report uses the stored dates.
- *Task cost:* only tasks with a saved cost, summed per currency. A task gets a saved cost when someone with manager access generates a Translation cost or Cost estimate report for tasks, or automatically through the task's Create Cost Estimate Report and Create Translation Cost Report options. The Actual cost (the task's translation plus proofreading total from the latest Translation cost report for it) wins over the Estimated cost (from the latest Cost estimate for it: the translation part for translation tasks, the proofreading part for proofreading tasks). Active columns cover only To Do and In Progress tasks; words are task sizes.

**Common questions.**
- *Translations, approvals, people translating a string.* Not read, except through a task's saved cost: an Actual cost follows `references/reports/translation-cost.md`, an Estimated cost `references/reports/cost-estimate.md`; read that file when the question is about what a task's cost includes.
- *Created before the period, finished in it:* only in the Resolved line.
- *No cost:* the task is absent from Task cost, not even zero. Deleting a cost report from the archive removes the cost it saved; the task then uses its other saved cost, if any, or drops out of Task cost (an older report's cost does not come back). A deleted task disappears everywhere.

**Project and workspace.** A workspace (or the organization) covers its projects and nested workspaces and can group by project; grouped by member, a person gets one row per project.
