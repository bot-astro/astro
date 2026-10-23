# Showing the round in a host with UI tables and a question tool

Crowdin Copilot is such a host: `ShowUI` renders the tables and `AskUserQuestion` asks the questions.

- **Tables first.** Render the round with the UI tool before any question: in Crowdin Copilot, `ShowUI` with one Card per term, the table's heading as the Card's title and the table as a DataTable. The question form shows only short option labels, so the tables have to be on screen before it opens.
- **Batches.** A question tool call holds at most 4 questions, so at most 3 conflict questions: with more than 3 conflicting terms, ask them in batches of 3, and put Apply as the last question of the call that holds the last batch. A round with up to 3 conflicting terms is a single call.
- **Apply** is a question of its own, never folded into a conflict question: a question holds at most 4 options. The summary of what will be written is the description of "Write as Drafts now". For a new term and for a backfill of an existing concept alike:

  ```json
  {"header": "<Apply, in the user's language>", "question": "<How should I apply the translations of \"error\"?>", "multiSelect": false, "options": [
    {"label": "<Write as Drafts now>", "description": "<1 new term, 3 translations: Ukrainian, German, French>"},
    {"label": "<Skip>", "description": "<Write nothing>"}
  ]}
  ```

  Every `<...>` is text you write in the user's language; the names above say what each option does, not how to label it.
- **Edits** come through the "Other" answer; apply them as given.
