# Translator accuracy

**What it shows.** How close each translator's translations are to the final approved text: per language and translator, the share of 100% Match, of each match band and of No Match. No rates or costs.

**Settings that change the result.** The period, language, members (the translators), files and labels (project), projects (workspace), unit, and Match Score Categories (see Match scores in `references/reports.md`). The chart labels the bands "Edit distance", but they are match score ranges.

**How it counts.**
- *Counted:* strings whose current translation is approved; for each, every translation a person added in the period and no later than the approval, except TM, MT and AI pre-translations. The reference is the current approved translation, chosen as Match scores in `references/reports.md` describes.
- The person's translation is the approved one: 100% Match. Otherwise its character similarity to the approved text (see Match scores) puts it in a band or in No Match.
- *One result per person, string, plural form and language:* their approved version, or else their closest one.

**Common questions.**
- *A string has several translations.* Each author is scored once against the approved text: the author of the approved one gets 100% Match, the others get their similarity. The page shows only per-translator charts; in exports, a language's Total counts the string once per translator.
- *The string was translated in different ways.* Typing, copying the source, and inserting a TM, MT or AI suggestion in the Editor all count as the person's own translation. Uploaded translations count for the uploader, API translations for the token's owner, unless the API call marks them as pre-translated. Pre-translations are no one's work, but an approved one serves as the reference.
- *Several people, or one person several times.* Each person is counted separately, once per string. A translator whose version lost to another translator's still gets a match score against the approved text, so it lands in a band or in No Match even though nobody edited it.
- *Approvals.* The report does not check who approved, so a self-approved translation is a 100% Match. On Enterprise one approval is enough, even when the workflow asks for more. If the approved translation is later replaced, the next run scores everyone against the new one; when the string no longer has an approved translation, it drops out. A translation added after the approval is ignored.
- *Deleted and hidden.* Deleted translations are not counted; hidden strings are.
- *Which date.* The person's translation's creation date. Editing it in place (Replace in translations) moves the date to the edit and keeps the author, so a translation edited after its string was approved drops out.

**crowdin.com and Enterprise.** Same calculation. crowdin.com filters one member at a time; Enterprise allows several. Enterprise adds the workspace level, which merges each translator's results per language across projects.
