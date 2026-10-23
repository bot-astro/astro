# Top members

**What it shows.** Members ranked by activity in the period: Translated, Target, Approved, Voted, "+" and "−" votes received, Winning, Languages, and Joined (crowdin.com) or Given access (Enterprise). No rates or costs; not archived.

**Settings that change the result.** The period, language, unit (words by default), projects (workspace), and sorting, which changes the rank only. A search by name or username narrows the list before ranking, so a member found by it can show as rank 1.

**How it counts.**
- It reads its record of actions, not the project's Activity feed.
- **Translated:** source volume of strings the person translated, once per string, language, plural form and source version, dated by their first save; pre-translations are not counted.
- **Target:** words or characters of the person's latest translation of those strings, even one made after the period.
- **Approved:** source volume of strings the person approved, dated by their first approval, including approvals of pre-translations, auto-approval on upload and pre-translation with auto-approve (credited to whoever ran it).
- **Voted:** the number of strings the person voted on, up or down, once per string and language (each plural form separately), dated by their first vote on it.
- **Votes received:** for each voter, only their latest vote on a string counts, and only when it is on the person's latest translation of that string; it is dated by the voter's first vote on that string. Votes on a pre-translation go to the account that ran it.
- **Winning:** source volume of the person's translations that were approved, once per approver, dated by that approver's first approval of the string. It counts only while the approved translation is still the person's latest translation of the string and the approver's latest approval there; approved pre-translations give no Winning.

**Common questions.**
- *A string has several translations.* Every author gets Translated for it; Approved goes to each approver, and Winning to the author of each approved translation, once per approver. On Enterprise different proofreaders can approve different translations of one string, so several authors can get Winning for it.
- *The string was translated in different ways.* Typing, TM, MT or AI suggestions in the Editor, copying the source, the API and uploads all count as Translated for whoever saved or uploaded. Pre-translations, including translations added through the API as pre-translated, give no one Translated or Winning; approving one still counts as Approved (also for whoever ran a pre-translation with auto-approve), and votes on one go to the account that ran it.
- *Several people, or one person several times.* Each person counts once per string and language; re-translating adds nothing. A person who first translated the string before the period and rewrote it in the period does not get it in that period.
- *Approvals.* Several approvers each get Approved, and the author gets Winning once per approver. Self-approval counts as both Approved and Winning. A removed approval, a deleted translation or a cancelled vote subtracts nothing.
- *Past periods can change:* Target, votes received and Winning follow each person's latest action on the string; Translated, Approved and Voted do not change.
- *Who is listed.* With all languages, every listed member, including those with no activity; with one language, members with no activity in it are hidden.

**crowdin.com and Enterprise.** Same calculation. crowdin.com lists everyone who has ever been a member of the project, including removed ones. Enterprise lists current members plus the project owner, managers, owners of parent workspaces, organization administrators and technical accounts; members removed from the project are not listed, so their work is not shown. Enterprise also adds the workspace level, which sums across projects.
