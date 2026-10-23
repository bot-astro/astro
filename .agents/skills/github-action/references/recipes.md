# Workflow recipes

Patterns that go beyond a single sync step. Each assumes the secrets and `permissions` block from the [main skill](../SKILL.md) and shows only what changes.

The action's own [`docs/EXAMPLES.md`](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/docs/EXAMPLES.md) is the upstream catalogue and stays current with the action — [more patterns upstream](#more-patterns-upstream) names its sections rather than copying them here. What's written out below is what upstream doesn't cover, or covers without the reasoning that decides whether you want it.

Third-party action pins here were current in August 2026 (`checkout@v7`, `create-github-app-token@v3`, `create-pull-request@v8`). Majors move faster than any document; check the action's releases page before trusting a pin copied from anywhere, upstream examples included.

- [Split upload and download](#split-upload-and-download)
- [No-`crowdin.yml` configuration](#no-crowdinyml-configuration)
- [Crowdin version branch per git branch](#crowdin-version-branch-per-git-branch)
- [GitHub App authentication](#github-app-authentication)
- [Download without pushing](#download-without-pushing)
- [Download a bundle](#download-a-bundle)
- [AI pre-translation](#ai-pre-translation)
- [Crowdin Enterprise](#crowdin-enterprise)
- [More patterns upstream](#more-patterns-upstream)

## Split upload and download

The two directions have different rhythms: sources should reach Crowdin as soon as they change, while translations are worth collecting on a schedule so contributors get one PR a day instead of one per commit. Splitting also keeps the upload workflow free of any GitHub write permission.

`.github/workflows/crowdin-upload.yml`:

```yaml
on:
  push:
    branches: [ main ]
    paths: [ 'src/locales/en/**' ]

jobs:
  upload:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: crowdin/github-action@v3
        with:
          upload_sources: true
          upload_translations: false
          download_translations: false
        env:
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

`.github/workflows/crowdin-download.yml`:

```yaml
on:
  schedule:
    - cron: '0 6 * * *'
  workflow_dispatch:

permissions:
  contents: write
  pull-requests: write

jobs:
  download:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: crowdin/github-action@v3
        with:
          upload_sources: false          # the default is true
          download_translations: true
          localization_branch_name: l10n_crowdin
          pull_request_title: 'New Crowdin translations'
          pull_request_base_branch_name: main
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

## No-`crowdin.yml` configuration

For a project with one source file and one translation pattern, the patterns can live in the workflow. Anything more — several file groups, `ignore`, `dest`, language mapping — needs the config file.

```yaml
- uses: crowdin/github-action@v3
  with:
    upload_sources: true
    source: src/locale/en.json                  # no leading /
    translation: src/locale/%android_code%.json
  env:
    CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
    CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

Pick one or the other: a `crowdin.yml` in the repository conflicts with these inputs.

## Crowdin version branch per git branch

`crowdin_branch_name` mirrors a git branch as a Crowdin version branch, so strings from a feature branch stay separate from `main` until it merges.

```yaml
- uses: crowdin/github-action@v3
  with:
    upload_sources: true
    download_translations: false
    crowdin_branch_name: ${{ github.ref_name }}
```

## GitHub App authentication

The fix for "the translation PR has no CI checks": an App token is scoped, rotates itself, and — unlike the default `GITHUB_TOKEN` — its pull requests trigger workflows. Create an App with **Contents** and **Pull requests** write permissions, install it on the repository, then store the App ID as a variable and the private key as a secret.

```yaml
    steps:
      - uses: actions/checkout@v7
        with:
          persist-credentials: false      # required, see below

      - uses: actions/create-github-app-token@v3
        id: app-token
        with:
          app-id: ${{ vars.CROWDIN_APP_ID }}
          private-key: ${{ secrets.CROWDIN_APP_PRIVATE_KEY }}
          permission-contents: write
          permission-pull-requests: write

      - uses: crowdin/github-action@v3
        with:
          upload_sources: false
          download_translations: true
        env:
          GH_TOKEN: ${{ steps.app-token.outputs.token }}
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

`persist-credentials: false` is not optional: otherwise the default `GITHUB_TOKEN` that `actions/checkout` cached in `.git/config` takes precedence over `GH_TOKEN`, and the PR is created as the Actions bot anyway.

A classic PAT with the `repo` scope works too — pass it as `GITHUB_TOKEN` — but it carries that user's full access and has to be rotated by hand.

## Download without pushing

Turn off the git half when translations need post-processing — reformatting, a lint pass, regenerating a compiled locale bundle — before they are committed. The files land in the workspace and are yours to handle.

```yaml
      - uses: crowdin/github-action@v3
        with:
          upload_sources: false
          download_translations: true
          push_translations: false
          create_pull_request: false
        env:
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}

      - run: npm run i18n:format

      - uses: peter-evans/create-pull-request@v8
        with:
          branch: l10n_crowdin
          title: 'New Crowdin translations'
```

This also side-steps `git add .`: the Crowdin step commits nothing, so a job that builds artifacts cannot leak them into the translation PR.

## Download a bundle

[Bundles](https://support.crowdin.com/bundles/) export strings in a chosen format regardless of the source format — and they are the **only** way to download translations from a string-based project, where `download_translations` does nothing.

```yaml
      - uses: crowdin/github-action@v3
        with:
          upload_sources: false
          download_translations: false
          download_bundle: 1              # numeric bundle ID
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

`crowdin_branch_name` is not applied to a bundle download — scope the bundle in Crowdin instead.

## AI pre-translation

Upload → pre-translate → download, as **three** steps: a `command` step runs nothing but its command, so it cannot sit inside a step that also uploads or downloads. Upstream has the full workflow in [AI.md](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/docs/AI.md), including the provider and prompt setup.

```yaml
      - name: Pre-translate with AI
        uses: crowdin/github-action@v3
        with:
          command: 'auto-translate'       # 'pre-translate' on the legacy v2 line
          command_args: '--method ai --ai-prompt=${{ secrets.PROMPT_ID }}'
        env:
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

AI output is only as good as the context it gets. Adding string context and screenshots before pre-translating raises quality far more than prompt tuning does. String context is written by an agent before the pipeline runs: the [context-extraction](../../context-extraction/SKILL.md) skill.

## Crowdin Enterprise

Enterprise projects live on an organization-specific API host, so the CLI needs it — in `crowdin.yml`:

```yaml
"base_url": "https://{organization}.api.crowdin.com"
```

or as the `base_url` input. The token comes from **Account Settings → Access Tokens** in the organization.

For GitHub Enterprise Server, point the git and API hosts at your instance with `github_base_url` and, when it differs from `api.<host>`, `github_api_base_url`.

## More patterns upstream

Fetch [`docs/EXAMPLES.md`](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/docs/EXAMPLES.md) — the maintainers keep it in step with each release — and read the section named below. Signed commits are in the [README](https://raw.githubusercontent.com/crowdin/github-action/refs/heads/master/README.md) instead.

| Upstream section | Reach for it when |
|---|---|
| Create PR with the new translations | You want the reference workflow verbatim |
| Translations export options configuration | Untranslated or unapproved strings should stay out of the repo |
| Separate PRs for each target language | Each locale has its own reviewers. Keep `max-parallel: 1` — parallel runs collide on Crowdin's build |
| Checking out multiple branches in a single workflow | A matrix of branches, or pushing somewhere other than the triggering branch — needs `skip_ref_checkout: true` |
| Caching source files for faster uploads | Hundreds of source files; caches checksums in `.crowdin/` so only changed files upload |
| Checking the translation progress | A release should block until translations are complete |
| Outputs | Auto-merging the PR. The outputs are only set when *this* run opened it ([outputs](inputs.md#outputs)) |
| Advanced Pull Request configuration | The translation PR should route itself — labels, assignees, reviewers |
| Run test workflows on all commits of a PR | Force-pushes keep invalidating checks; pass a PAT to `actions/checkout` |
| Triggers | Cron, manual, path-filtered, on release. A release or tag trigger needs `pull_request_base_branch_name` set, or the PR base resolves to a tag ref |
| GitHub (Enterprise) configuration *(README)* | Signed commits — key email, GPG account email, and `github_user_email` all have to match |
