# Inventory: from uncovered strings to screens

Reached from `SKILL.md`'s step 1. The coverage query lists every string with no screenshot; this file turns that list into the screen plan the user reviews.

## What the query gives

CroQL cannot combine with other filters, so the query stands alone; `--verbose` widens each row to `fileId`, `labelIds` and `context`, and `-o json` is the shape to process. On a gettext-based project `context` is the shortcut: PO uploads carry each entry's `#:` source references into Crowdin, so every uncovered string already names the file and line that renders it. A key-value catalog (JSON, strings, XML) carries no references, so each key is looked up in the code first; the rendering file is wherever the key is used.

## From files to screens

Screens come from the app's own structure: the router on the web (file-based routes, a route config), the navigator registrations or `app/` tree in React Native and Expo, storyboards, `NavigationStack` destinations or deep-link handlers on iOS, the navigation graph on Android. Each screen has an entry file - a route module, a page component, a navigator registration - and a string belongs to every screen whose import walk from its entry file reaches the string's rendering file. Cut out of the walk whatever imports every entry file at once - a generated route manifest, a central router or navigator module - since through that edge every string would look shared by every screen. Strings reached from no entry file are the first residue candidates: components used only in a state the walk cannot see, or code no screen renders.

Then widen each route into its states. The rendering files name them: a dialog or sheet component, a tab set, a form's validation schema, an empty-state branch, a toast call. Each is a screen of its own in the plan, reached from its route by the interaction that shows it.

## The plan

One row per screen: the screen name under the contract's naming rule, how it is reached (a URL or deep link, or the taps from one), and which strings it covers, as counts per rendering file rather than string by string. Order it deep-linkable screens first, then interactive states, then the variants worth a separate trip. Name what is left out and why - strings reachable only through transient toasts, responsive-only layouts, states behind flags - so the review is of the whole picture, and the residue step inherits the list instead of rediscovering it.
