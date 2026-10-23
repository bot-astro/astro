# Continuous sync

Reached from `SKILL.md`'s Phase 7 — Continuous sync (CI), for the `write_workflow` and `set_secret_instruction` steps of `plan.md`. The workflow itself comes from the [`github-action`](../../github-action/SKILL.md) skill — its inputs and their defaults, the version to pin, the permissions pull-request creation needs, and every way a run fails once it is on a runner. This file covers what comes before and after that delegation: whether this repository should get a workflow at all, what to tell the delegated skill about the `crowdin.yml` phase 5 already wrote, and what to read back before calling the phase done.

## Gate: is anything already syncing this repository?

Before the decision below, and before any delegation, run:

```bash
git ls-remote origin 'l10n*'
```

A translation automation announces itself by the service branch it pushes to. Crowdin's native GitHub integration and a workflow in the repository both commit translations to a branch in the `l10n*` family and open a pull request from it, so a hit here means something is already doing this job — the native integration, or a workflow set up before this run. Adding a second automation does not give the user two options to choose between; it gives them two automations racing to open the same pull request, with duplicate or conflicting PRs as the visible result.

A hit is a stop. Report the branch or branches found, ask the user which automation they intend to keep, and do not delegate `write_workflow` until they answer. This gate runs regardless of what the decision rule below would have recommended: a service branch that already exists is a fact about the repository, not a preference to weigh against others.

A repository with no remote fails the command outright instead (exit 128), and the default-branch lookup below fails with it. That leaves the gate's question **unanswered, not cleared** — record in `decisions.md` that it never ran, take the workflow's trigger branch from the user instead of the remote, and tell them to re-run the gate once a remote exists. The workflow only ever executes on GitHub, so the remote arrives before the first run does.

## Decision: a workflow in the repository, or the native integration?

The default is a workflow, because every part of it is something this journey can produce and the user can review in a pull request. Recommend Crowdin's **native GitHub integration** instead — and stop without delegating — when any of these holds:

- Sync should be managed by non-developers directly in the Crowdin UI, rather than through a file that lives in the repository and changes by pull request.
- GitHub Actions aren't usable in this repository at all: organization policy, no minutes, or Actions disabled outright.
- The user would rather have Crowdin's own automatic sync than own a workflow file.
- The project is string-based and delivers translations as target-file bundles — a shape the native integration handles more directly than a file-by-file sync.

When one of them does hold, the rest of this phase is a conversation rather than a file write: point the user at their Crowdin project's **Integrations** tab, where the integration's GitHub authorization happens. That authorization is interactive and browser-based, so nothing here can perform it on the user's behalf — which is exactly why a workflow is the default for repositories that would rather keep their automation in version control.

## Delegate the workflow

`write_workflow` hands over to the **`github-action`** skill. Confirm it is loadable first; if it isn't, the fix is one install command (`npx skills add crowdin/skills --skill github-action`, or the whole set with `npx skills add crowdin/skills`), and a run that installed the whole set — or the plugin, in any agent that installs it as one — already has it. Ask the user to install it and wait.

Do not write workflow YAML from memory in its place — not a minimal version, not "the usual shape". A workflow that looks right and fails on its first run is worse than a phase that paused: the failure surfaces after the user has already been told the journey is finished, and it surfaces on GitHub rather than here. Phase 3's dependency gate exists for the same reason, and the reasoning carries over unchanged.

State up front what the workflow has to come out as, so the delegated skill is deciding *how* rather than *whether*: every merge to the default branch pushes new source strings to Crowdin, finished translations come back as a pull request without anyone running a command, and the credentials the run needs are the ones phase 5 already established.

Then pass what that skill cannot know — this journey's own Crowdin decisions:

| Input | Value | Source |
|---|---|---|
| Config on disk | `crowdin.yml` at the repository root, already verified through phase 5's four gates — it is the sync configuration, not a starting point to re-author | phase 5 |
| Project identity | `project_id` is committed in that file, so the workflow needs no project-id secret and no `project_id_env` | phase 5's `crowdin.yml` |
| Credential name | exactly `CROWDIN_PERSONAL_TOKEN`, as a repository secret — the name `api_token_env` points at | phase 5's `crowdin.yml` |
| Default branch | resolve it now with `git ls-remote --symref origin HEAD`, whose first line names the branch `HEAD` points at | the remote, not `detection.json` |

The default branch comes from the remote and not from `detection.json`'s `git.branch`, which is a different thing wearing a similar name: phase 1 captures it with `git branch --show-current`, records it as informational, and rules it out as a basis for any decision. On a full journey it is the *working* branch by the time this phase runs, because phase 2 offers a new branch and that is the usual answer — so reading it here triggers the workflow on a feature branch, and does so silently. The remote is the source of truth, this phase already reaches it for the gate above, and `git ls-remote` needs neither local refs nor `gh` installed.

The keys phase 5 writes (`project_id`, `api_token_env`, `base_path`, `preserve_hierarchy`, the `files` patterns, and `languages_mapping` or `base_url` where they apply) mean the same thing to Crowdin CLI 4 and 5 alike, so whichever CLI the action bundles reads the committed config correctly — the version pin is safely the delegated skill's call.

## Verify what came back

A delegation is finished when its output has been read, so open the workflow file the delegated skill wrote and check four things:

- Its Crowdin step reads the token from `secrets.CROWDIN_PERSONAL_TOKEN`. A different secret name is the one defect that looks correct on disk and fails on the first run, because it silently disagrees with the `api_token_env` in the config.
- No credential appears as a literal anywhere in the file — every credential is a `secrets.*` reference.
- No second copy of the project id. A workflow written from the general recipe usually carries a project-id secret, because a repository without a committed `project_id` needs one; this one has it in `crowdin.yml`, so that line is a duplicate of a value that can now drift, pointing at a secret nobody was asked to create. Drop it.
- The branch the workflow triggers on is the repository's actual default branch.

Correcting a Crowdin-side value in the file — the secret name, the trigger branch, the redundant project id — is in scope for you, and none of the three is worth a re-delegation. Rewriting the workflow is not in scope: a disagreement about its shape goes back to the delegated skill with the violated constraint restated, the same handling a failed post-condition gets in phases 3–4.

## Secret

`set_secret_instruction` is an instruction, not an action: the workflow's token comes from a repository secret only the user can create. Its value is a **personal access token**. When phase 5's token gate resolved one — from the environment or `.env` — the user can reuse it; when phase 5 authorized through `crowdin login`, the token in `~/.crowdin.yml` expires after 30 days and stays on that machine, so the user creates a personal access token now: crowdin.com → **Settings → API** (`https://crowdin.com/settings#api-key`); Crowdin Enterprise → **Account Settings → Access Tokens** in the organization's Crowdin UI. Then ask them to run, in their own terminal:

```bash
gh secret set CROWDIN_PERSONAL_TOKEN
```

`gh` prompts for the value and sends it straight to GitHub, so it never passes through anything this run reads back — the rule phase 5's token gate already holds to. When `gh` is absent or unauthenticated, use the repository settings path instead (the `github-action` skill names it) rather than asking the user to paste the token anywhere an agent would see it.

Treat the step as complete once the user confirms the secret is set. There is nothing to verify from this side, because verifying would mean reading the value back. What proves the whole phase is a workflow run the user triggers and reads — the `github-action` skill's own last step, and the right place to end phase 7.
