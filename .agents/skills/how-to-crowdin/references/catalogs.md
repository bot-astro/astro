# Live catalogs

Two plain-text indexes describe what Crowdin offers today. They are the source of truth for anything this skill's own map does not cover, and for confirming that an app or feature the map names still exists. Nothing from them is copied into this skill: the store changes weekly, and a copy would be the stale one.

## The store

`https://store.crowdin.com/llms.txt` lists every marketplace item, grouped under one `##` heading per category, one line per item: name, a link to the item's own text file, and a one-line tagline.

```
## Manager Productivity

- [Cross-Project Search](https://store.crowdin.com/llms-txt/cross-project-search.txt): Finds strings, translations, files and branches by text across every project in the organization
```

Each item file (`https://store.crowdin.com/llms-txt/<slug>.txt`) opens with the name, tagline, store URL, `Category`, `Type` and `Author`, then the full description. `Type` tells you what you are looking at:

| Type | Meaning for triage |
|---|---|
| `App` | Installable. Rung 3. |
| `System` | A built-in Crowdin feature that happens to be listed in the store, such as a natively supported file format or a Crowdin-authored MT engine. Rung 1. |
| `Guide` | A how-to for using Crowdin with some external product. Usually documents a native integration, so read it as rung 1 unless it points at an App. |
| `Copilot Skill` | A capability of the AI assistant inside Crowdin. Rung 2 territory: it describes something an agent does on demand. |
| `QA Check` | An installable external QA check. Rung 3. |

The index is about 125 KB. Fetch it, then keep only the section under the category heading you need, or the lines whose name or tagline contain the user's keywords. Read one or two item files in full before recommending anything. A tagline is enough to shortlist, never enough to recommend.

Categories, as the headings spell them: AI, CMS, Marketing, eCommerce, Development, Design, File Management & Storage, Customer Service, Machine Translation, TM & Glossaries, File Formats, Translator Productivity, Manager Productivity, Team Chat & Notifications, Task Management, Security & Identity, Gaming, Other.

When the user names an app, go straight to its item file. Slugs match the store URL path: `https://store.crowdin.com/stale-translations-advisor` is `llms-txt/stale-translations-advisor.txt`.

## The docs

`https://support.crowdin.com/llms.txt` is an index of documentation sets. The ones that matter here:

| File | Use it for |
|---|---|
| `https://support.crowdin.com/_llms-txt/crowdin-help.txt` | crowdin.com features: how a native feature works, where it lives in the UI, its limits |
| `https://support.crowdin.com/_llms-txt/crowdin-enterprise.txt` | the same for Crowdin Enterprise, plus Enterprise-only features: workflows, vendors, teams, groups |
| `https://support.crowdin.com/_llms-txt/developer-portal.txt` | apps, MCP server, CroQL, GraphQL, webhooks |
| `https://support.crowdin.com/_llms-txt/api.txt` | the REST API, when a rung 2 answer needs a specific resource confirmed |

These files are large (the Help set is close to 3 MB). Search inside them for the feature name rather than reading them through. Every page in them is a `# Title` heading followed by the page body, and the page's public URL is `https://support.crowdin.com/<slug>/` for crowdin.com, `https://support.crowdin.com/enterprise/<slug>/` for Enterprise, and `https://support.crowdin.com/developer/<slug>/` for the Developer Portal.

## What the catalogs cannot tell you

Neither index knows which edition or plan the user is on, what is already installed in their organization, or whether a given feature is enabled in their project. When the answer depends on one of these, say so and ask, or check through the connector when one is available.
