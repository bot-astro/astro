---
name: generate-screenshots
description: Captures screenshots of a running app and uploads them to Crowdin as visual context tagged to the strings they show. Use when someone wants to add screenshots to a Crowdin project, show translators where a string appears in the UI, find which strings still have no screenshot, or update the screenshots after a release - including bare phrasings like "translators can't see where this string is used". Not for `crowdin screenshot` syntax alone (crowdin-cli), textual string context or `ai_context` (context-extraction), or the full internationalize-and-connect journey (i18n-setup, which delegates here).
---

# Generate screenshots

A bare `Save` could be a button, a menu item, or a dialog title, and each reading translates differently. A screenshot is the strongest context Crowdin can attach to a string: Crowdin tags strings to positions on the image, so one screenshot contextualizes every string visible on it.

This skill owns the Crowdin side, the same on every platform: which screens are worth a shot, measured by which strings have none; one folder of PNGs whose file names are the screenshot identities; one upload; a report of what is still uncovered and why. Capturing is the platform's own business, done by a **capture definition** the skill writes into the project - a Playwright spec, Maestro flows, an XCUITest - so the next run is two commands. For every `crowdin` command's full behavior, read the `crowdin-cli` skill's [commands reference](../crowdin-cli/references/commands.md#screenshot); this file states only the flags it passes.

## Preconditions

- **A connected project**: `crowdin.yml` with a project id and `api_token_env`, sources already uploaded - screenshots tag strings that exist in Crowdin. Without a `crowdin.yml`, the `i18n-setup` skill's connect-only path comes first.
- **The Crowdin CLI**, authorized. Exit code `101` means the user runs `crowdin login` in their own terminal; the token value stays out of the conversation.
- **A shell.** The engines and the CLI run in one. A connector-only assistant points the developer at this skill in a terminal.
- **A runnable app** and an engine from the table below, or [publish-only mode](#publish-only-mode) without one.

## The journey

**1. Inventory.** Measure before planning:

```bash
crowdin string list --croql 'count of screenshots = 0' --verbose -o toon
```

Group the uncovered strings into screens by the method in [references/inventory.md](references/inventory.md), then show the plan - screen name, how it is reached, which strings it covers - and **stop for the user's review**: they know which screens matter, which strings are dead, and what the app looks like when it is healthy. When the engine still has to be installed, the plan and the install are one question, so one yes covers both. Done when the user has agreed the plan; nothing is written or captured before that.

**2. Session setup.** The user starts the app, or the engine starts it, and the user signs in once, interactively, into a session the engine reuses; the engine's reference says how. Seeded data is the user's to provide: ask for it, since a screen faked with invented records is context that lies. Done when one scouting look shows a signed-in screen in the source language.

**3. Capture definition.** One test or flow per screen in the plan, named after the screen, ending in a screenshot into the batch folder under [the contract's](#the-contract-one-folder-stable-names) name; the engine's reference has the shape. It is project code: show it, and commit it when the user says so - a calling workflow's approved plan counts as that word. Done when every screen in the plan has its test or flow.

**4. Capture.** Empty the batch folder, run the definition, list the folder. Then look at a sample of the images - every dialog, menu and scrolled section, and a few plain pages - for **spoiled captures**: a passing test can still shoot content under a sticky header, a dialog mid-animation, a dev-tools overlay, a hover state; the engine's reference names the guard for each. A screen that failed is fixed in the definition or dropped from the plan with a note. Done when every planned screen has a PNG in the folder and the sample is clean.

**5. Upload.** Run `crowdin screenshot list` first, since the upload's echo does not say created versus updated and the report needs both. Then:

```bash
crowdin screenshot upload .crowdin/screenshots --auto-tag
```

When every string in the project lives in one Crowdin file, add `-f` with that file's path as `crowdin file list` prints it: it scopes the matching and sharpens it, and it applies to every image in the folder, so it is for single-file projects only. With `--auto-tag` the images go one at a time, at a few seconds each; dozens take minutes, so run it in the background. Done when every file in the folder has echoed a screenshot id.

**6. Residue.** Re-run the coverage query and sort every remaining string into exactly one group: placeholder and plural strings, which auto-tag never matches even when they are on screen; strings that are never pixels - alt text, `aria-label`s, page titles; strings in reachable states not yet shot - a tab, a select's options, a form's other error messages, empty states, toasts - which become new screens in the plan when they are worth the trip; and strings unreachable in the app - error paths, emails and push notifications, feature flags, text composed at runtime. Done when no string is outside a group; an honest residue is the terminating condition, zero uncovered strings is not.

**7. Report.** Coverage before and after, screenshots created and updated, screens whose upload echoed zero tags, the residue groups with their counts, and the capture definition's path with the commands from its reference that re-run it.

## The contract: one folder, stable names

Every engine ends in `.crowdin/screenshots/`, a flat folder of PNGs inside the gitignored `.crowdin/` workspace (create it and add `.crowdin/` to `.gitignore` if `i18n-setup` has not already; show the one-line diff first when `.gitignore` has content). The images' durable home is Crowdin.

- **The file name is the identity.** `crowdin screenshot upload` upserts on the basename: `settings.png` replaces the project's `settings.png` in place, keeping its id, so a re-run never duplicates. Names are kebab-case screen names - `dashboard`, `settings-profile` - unique and stable, because renaming a screen orphans the old screenshot instead of replacing it.
- **Platform and state are suffixes.** The same screen on two platforms is `settings-ios.png` and `settings-android.png`, since a directory upload aborts on two files with one basename, subfolders included. A variant is `settings-empty.png`.
- **The folder is the batch.** Empty it before a run, so the upload touches exactly the screens captured this run: an `--auto-tag` upload re-derives a screenshot's tags from the image and discards the ones it had, including tags placed by hand in Crowdin, so only a re-shot screen gets re-tagged.
- **One framing.** The **source language**, since translators need the source text in view and auto-tag matches against source strings; one viewport or one device per platform; light theme unless the app has none. Re-shot images then replace old ones seamlessly.

## Screens, not strings

The unit of work is a **screen**: one capture-worthy UI state - a route, a modal, a drawer, each tab of a tab set, a select or menu open, a form after an empty submit and after a wrong one, an empty or error variant. Strings group under the screen that shows them; shared chrome is covered by whichever screen is shot first. One screenshot per screen: an app with a thousand strings collapses to a few dozen screens, and one image contextualizes everything visible on it.

Deep-linkable screens come first - a URL or deep link is a one-line capture - and a screen only a tap sequence reaches gets a flow. The agent's own browser, simulator or desktop is for **scouting** a screen before writing its flow: what renders, how it is reached. A capture is a file a command wrote.

## The engine

Pick by what the project already has, in this order, and read only that reference:

| Project | Engine | Capture definition | Reference |
|---|---|---|---|
| Native iOS with an existing UI test target | XCUITest | a screenshot test in that target | [references/ios-xcuitest.md](references/ios-xcuitest.md) |
| Expo or React Native already running screenmap | screenmap | its output folder, renamed into the contract | [references/expo-screenmap.md](references/expo-screenmap.md) |
| Any mobile app - iOS, Android, React Native, Flutter | Maestro | YAML flows under `.maestro/crowdin/` | [references/mobile-maestro.md](references/mobile-maestro.md) |
| Web | Playwright | a spec file, one test per screen | [references/web-playwright.md](references/web-playwright.md) |
| A desktop app, a few one-off states, or no test code wanted - when the agent can drive the desktop | Computer use | none; the report carries the screen list | [references/computer-use.md](references/computer-use.md) |

Maestro is the mobile default because one YAML dialect covers every mobile stack and a flow reads as the list of taps it is. A project that already drives its UI with something else - Detox, Espresso, Flutter integration tests - keeps it: any engine that writes named PNGs into the folder qualifies. Computer use is the one engine that leaves no definition behind, so it is offered after the others with that cost named. Installing an engine changes the project's dev dependencies: say what gets installed and get a yes, the same yes as the plan's.

## Re-runs

A re-run is a **top-up**: the inventory diffs the app against the capture definition - new routes, new uncovered strings, screens whose UI changed - and the run captures only those, by running only their tests or flows. The definition plus Crowdin's own state are the entire memory. A full re-shoot is a mode the user asks for after a redesign. Screenshots in Crowdin stay: replacement goes through the same-named upload, and a screenshot with no successor is deleted only on the user's explicit yes, after listing what exists, because deletion removes context translators may be relying on.

## Tags inform, never gate

The upload is the success condition. Auto-tag is OCR against source strings, and it misses interpolated text (`Hello {name}`), plural forms, icons-with-labels, and truncated or low-contrast text; a screenshot with zero tags still shows the translator the screen. Tag counts go into the report as observations, with `-f` scoping as the one bounded improvement to offer, and the run moves on.

## Publish-only mode

When no engine can run here - no Node for Playwright, no simulator on this machine, no desktop the agent can drive, a CI box with no display - say so and fall back: the inventory still produces the plan, the user captures the screens themselves and drops the images into `.crowdin/screenshots/` under the plan's names, and the skill uploads and reports. Every image uploaded is one a command or the user wrote; the skill never generates, mocks or edits an image of the app, because a fabricated screenshot teaches every translator a UI that does not exist.

## Failure modes

| Situation | Do this |
|---|---|
| App won't start, or the session is signed out | Stop and ask. Nothing uploads from a broken session. |
| Upload warns "auto tag is currently in progress" | The image landed; tags were skipped. Note it in the report; the next run's upload re-tags. |
| Upload warns that several screenshots share the name | Duplicates predate this process; the CLI updates the oldest. Surface the warning and offer cleanup. |
