# Extending this skill

For maintainers adding a stack or a language family. Nothing here runs during a setup journey — an executing agent has no reason to read this file.

## What a stack is

A stack is a **language family plus a library**, and never finer than that. `js-ts-lingui` covers every framework Lingui supports, because the differences between them — packages, plugin wiring, which compiler transforms the macros — are what the delegated skills exist to resolve. A framework that needs different handling is that ecosystem's problem to route, not a new entry here; what earns a new entry is a new library to connect (a second JavaScript library, say) or a new language family entirely, which is where the non-web platforms come in: Rails, Android, iOS, and anything else whose catalogs Crowdin takes.

## What a new stack adds

- one `stacks[]` manifest entry — naming the ecosystem skills to delegate to, or internal reference files when no ecosystem exists;
- `references/libraries/<lib>/crowdin.md` — the adapter: locale codes, catalog layout, format, post-conditions;
- `references/libraries/<lib>/rules.template.md`, conforming to `references/rules-template-format.md`;
- eval fixtures and goldens under `evals/`;
- for a new language family only: `references/detect/<language>.md` and one row in `SKILL.md`'s language router table.

The manifest entry must pass `evals/verify-manifest.sh` — schema shape, resolvable paths, and the cross-file invariants. A new manifest field lands with its `manifest.schema.json` entry in the same commit.

## What a new stack never edits

Phases 5–7 of `SKILL.md`, `references/connect.md`, `references/continuous-sync.md`, `references/delegate.md`, `references/glossary.md`, `references/plan-format.md`, `references/rules-template-format.md`, `references/verify-catalogs.md`, or the eval harness scripts. If a stack seems to need an edit to a shared file, the shared file is missing an abstraction — fix that first (usually a new `catalogs[].model`), don't inline. And if it seems to need a *framework* field in the manifest, that is the same signal pointing at the delegation boundary instead.
