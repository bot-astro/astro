# Connect

Reached from `SKILL.md`'s Phase 5 — Connect, for the `create_project` (optional), `write_crowdin_yml`, `config_lint`, `config_sources`, `config_translations`, `upload_dryrun`, and `upload_sources` steps of `plan.md`: authoring `crowdin.yml`, the token gate, project creation or verification, and what each gate's output looks like when it passes. The five commands run in the fixed order `SKILL.md` states. For any command's full flag set, read the `crowdin-cli` skill — its [own `SKILL.md`](../../crowdin-cli/SKILL.md), its [commands reference](../../crowdin-cli/references/commands.md), its [configuration reference](../../crowdin-cli/references/configuration.md).

## CLI preflight

Run `crowdin --version` before anything else in this phase.

- **`5.x`** — proceed.
- **`4.x`** — recommend upgrading in place: `npm install -g '@crowdin/cli@^5'` replaces the old major with the current one, and v5 is a single self-contained binary, so upgrading also drops the JRE dependency v4 needed. Point at the `crowdin-cli` skill's [v4-to-v5 migration reference](../../crowdin-cli/references/migrating-from-v4.md) for the flag renames a v4 script would otherwise hit (`pre-translate` → `auto-translate`, `--plain` → `--output plain`, and the rest).
- **absent** — install, trying in order: `npm install -g '@crowdin/cli@^5'` first (most target repos already have Node), else Homebrew on macOS, else the standalone binary. Those two commands, and the rest of the install matrix after them, live in the `crowdin-cli` skill's [configuration reference, installation section](../../crowdin-cli/references/configuration.md#installation).

## Token gate

Detection order — stop checking at the first one found:

1. `CROWDIN_PERSONAL_TOKEN` already set in the environment commands run in — it got there from whatever launched the agent, or from CI.
2. A `.env` file in the project root that sets it. Before treating this as a good credential source, confirm `.env` is actually covered by `.gitignore` — a committed `.env` is a leaked secret sitting in the repository, not a valid place to keep a token, no matter what value it holds.
3. `~/.crowdin.yml` (or whatever `--identity` points at) carrying an `api_token` entry — the file `crowdin login` writes.

If none of the three resolve a token, stop and ask the user to run, in their own terminal:

```bash
crowdin login
```

It authorizes in the browser and writes the identity file, and that is all: `login` is the authorization half of `crowdin init` on its own, and this phase authors the config half — `crowdin.yml` — itself, below. The command is the same for both Crowdin editions. It runs in the user's terminal because the flow finishes through a browser callback on the machine running it; when the agent's shell is that same machine, the agent may run it and relay the URL it prints if no browser opens. What the identity file receives, the timeout, and the token's 30-day lifetime are in the `crowdin-cli` skill's [login entry](../../crowdin-cli/references/commands.md#login). That lifetime suits a developer's machine and is why phase 7's CI secret is a separate personal access token — `continuous-sync.md` covers creating it there.

The alternative is a **personal access token**, for a user who wants a token that does not expire or has no browser on this machine. Print the creation URL rather than a form to fill in: crowdin.com → **Settings → API** (`https://crowdin.com/settings#api-key`); Crowdin Enterprise → **Account Settings → Access Tokens** in the organization's Crowdin UI. Name the token's destination in the same message: a `.env` file at the project root, one line, written by the user —

```
CROWDIN_PERSONAL_TOKEN=<the token>
```

Before pointing the user at that file, make sure `.gitignore` covers `.env`, adding the line if it is missing — that edit carries no secret, and it has to exist before the token does. CLI v5 reads `.env` from the directory it runs in natively, so the token is live the moment the file is saved: no export, no restart. That is why `.env` is the instruction to give — the shell each command runs in inherited its environment when the agent started, so a variable exported in the user's terminal mid-session never reaches it. An environment variable delivers a token only when it was set before the agent launched, or by CI. A user who wants one token across every project on the machine can put it in `~/.crowdin.yml` instead; the CLI picks that file up by itself.

Either way this is a real stop, not something to poll around: wait for the user to say they are done, then run the detection above again — it now resolves at one of the three sources, and the verification call below is what closes the gate.

Verify whatever token was resolved with one authenticated call, and pass a placeholder id to get past the config validator:

```bash
crowdin project list -i 1
```

The command itself ignores `-i`, but validation runs ahead of every command and rejects a config whose `project_id` is missing or non-numeric — and at this point in a fresh setup there is no `crowdin.yml` to carry one, which is the whole reason this call is being made before the project exists. Without the flag the failure reads `Required option 'project_id' is missing`, which looks like a config problem and is really the validator refusing to let a token be checked. `-i 1` satisfies it inline, so nothing has to be written to disk. Branch on the exit code — `0` proceeds to the next step, `101` means the token is bad or expired, back to the top of this gate, and `2` is the validator, not authentication. This is also the call the Project step below reads for an existing project of the same name, so it earns its round-trip twice.

**The agent never sees, asks for, echoes, or writes the token value.** The check here is the exit code, never the token text, and nothing in this step puts the value in a prompt, a variable the agent prints, or a file it writes. The one file inspection this step performs — confirming `.env` is gitignored — reads the ignore rule, never the token line itself.

## Project

The project either already exists (the user hands over a numeric `project_id`) or needs to be created.

- **Existing project** — take the numeric id directly from the user; there's nothing to call yet.
- **New project** — first check the `crowdin project list` output the token gate already produced for a name matching this repository, and surface any match: the project may already exist, and a duplicate is a real cost. Then *offer* `crowdin project add <name> --source-language <id> -l <id>` (one `-l` per target), never run it unasked, and state plainly what it is about to create before running it: the project name, and the source/target languages already frozen in `decisions.md`. Both language flags take the **language id** `decisions.md` recorded beside each locale code. Get an explicit confirmation of that exact name-and-language set — this creates a real project, it isn't a preview.
- **Declined, or the token lacks the scope to create a project** — fall back to the user creating the project in the Crowdin UI themselves and pasting back the numeric id. Continue from there exactly as if it had existed all along.

Either path ends with one numeric `project_id` in hand — the only project-side fact `write_crowdin_yml` still needs.

## Write `crowdin.yml`

The recipe for the v1 stack (`lingui-po`, per the `catalogs[]` row carried in `manifest-snapshot.json` and the layout [`references/libraries/lingui/crowdin.md`](libraries/lingui/crowdin.md) already verified on disk) is fixed. The `translation` pattern's language placeholder is always `%locale%`, and a greenfield run needs no `languages_mapping` at all: phase 3 named the locale directories with the targets' Crowdin locale codes, which is exactly what `%locale%` resolves to. A `languages_mapping` block appears only when `decisions.md` froze one — user-chosen codes, or a layout that predates this journey — with one row per target whose spelling differs from its locale code.

```yaml
"project_id": "<id>"
"api_token_env": "CROWDIN_PERSONAL_TOKEN"
"base_path": "."
"preserve_hierarchy": true

files:
  - source: "/src/locales/en/messages.po"
    translation: "/src/locales/%locale%/%original_file_name%"
```

Substitute the real numeric id from the Project step for `<id>`, and add a `languages_mapping` block only when `decisions.md` froze one. One line in this block is a security invariant and stays absolute: `api_token_env` never becomes a literal `api_token`. It stays when the local token came from `crowdin login`: the identity file outranks the config's `*_env` keys, so `~/.crowdin.yml` supplies the token on this machine and the CI secret supplies it in phase 7's workflow — one config, both places. Whatever the scheme, what proves the resolved path is right is gate three printing paths that match the directories already on disk, never the identity of the placeholder by itself. See the `crowdin-cli` skill's [configuration reference, placeholders section](../../crowdin-cli/references/configuration.md#placeholders) for the full placeholder set when reading a pre-existing config that chose a different placeholder — a config this journey authors always uses `%locale%`.

Two additions apply on top of the fixed block, and only when actually needed:

- **Crowdin Enterprise** — add `"base_url": "https://{org}.api.crowdin.com"`, with `{org}` the organization decided in phase 2. A crowdin.com project omits this key entirely; the CLI's own default already points at `https://api.crowdin.com`.
- **A catalog root the row doesn't spell** — substitute the matched `catalogs[]` row's `source`/`translation` patterns with the **actual paths on disk**, not the row's paths verbatim. The row names the shape (e.g. `next-intl-json`'s `/messages/en.json` → `/messages/%locale%.json`) using the most common root; the real one can sit a directory over — a connect-only project that chose its own, or a project whose sources aren't under `src/` at all, so the catalogs phase 3 created aren't either. The config points at where the files actually are, and gate two below is what confirms it.
- **Locale directory names that differ from Crowdin locale codes** — the user chose codes (URL segments like `/uk/`, an in-house convention), or i18n predates this journey: directories spelled `uk`, `en_US`, `pt_br`, or a bare `de` are none of what `%locale%` resolves to for those languages. Each such language gets a `languages_mapping` row under the `locale` key (see the `crowdin-cli` skill's [configuration reference, language-mapping section](../../crowdin-cli/references/configuration.md#language-mapping)), mapping the Crowdin language ID to the spelling on disk. Resolve what `%locale%` yields per language with `crowdin language list --code locale -o toon` — project- and mapping-aware, which is what makes it authoritative over a guess, but it needs the token verified above. Before credentials exist at all — while freezing spellings back in phase 2 — the same command with `--all` (`crowdin language list --all --code locale -o toon`) answers from the full supported-language list instead of the project's, needing no token and no `crowdin.yml`.

## The four gates, in order

`SKILL.md` fixes this order and it is not a suggestion:

```
crowdin config lint  →  crowdin config sources  →  crowdin config translations  →  crowdin upload sources --dryrun  →  real upload
```

Run them in this sequence every time. Do not jump to the dry run because the config file looks correct by eye, and do not run the real upload before the dry run has actually been seen to pass.

1. **`crowdin config lint`** — offline. Catches structural mistakes (a missing `source` or `translation`, a `translation` pattern with no language placeholder, `dest` used without `preserve_hierarchy`, and the rest of the CLI's validation rules) before anything touches the filesystem or the network.
2. **`crowdin config sources`** — lists the local files the `source` pattern actually matches. A project-info fetch precedes the local listing, so the command wants the token and the real project id even though what it prints is local — the gate order above has both in hand by now, and a `Project Not Found` here is a wrong `project_id`, not a pattern problem. The resolved list must be non-empty and must equal the catalog file(s) phase 3/4 (or an existing project, in connect-only) actually put on disk — an empty or short list means the glob is wrong, not that the catalog is missing.
3. **`crowdin config translations`** — resolves the `translation` pattern's placeholders against the project's actual configured target languages, so it needs the token verified above and has to reach the project to answer at all. That project access is exactly what makes it the gate that matters most: it prints the per-language paths the `translation` pattern resolves to for real, and comparing that output against the locale directory layout already on disk catches **a wrong or missing mapping row as a wrong path**, not as a confusing failure after the upload has already run — `%locale%` resolving `uk` to `uk-UA` while the directory on disk is `uk` is exactly the failure this gate exists to catch before it costs an API call. An authentication or project-access failure here is not a config error, though — read the error before touching `crowdin.yml`. Fix the config and re-run this same command only once the printed paths themselves are wrong; don't move on until they match reality.
4. **`crowdin upload sources --dryrun`** — a preview of the upload itself: the tree the real command would push, with nothing written to the project yet.

Only once all four have passed does the real `crowdin upload sources` run, pushing the source file(s) to the project for the first time.

## Optional add-on: auto-translate

`crowdin auto-translate --method tm` runs only when the user opted into it back at Decide (the add-ons recorded in `decisions.md`) — it is off by default, and this step does not re-open that question. Never reach for `--method mt` or `--method ai` unless the project already has a machine translation engine or an AI prompt configured in Crowdin (`crowdin ai-prompt list --action pre_translate --assigned` shows the prompts it may use); running either method against a project with nothing configured for it is a wasted call at best. See the `crowdin-cli` skill's [commands reference, auto-translate section](../../crowdin-cli/references/commands.md#auto-translate) for the full option set (`--scope`, `--label`, `--auto-approve-option`, and the rest) if the user wants finer control than the bare TM call.

## Wrap-up

Close phase 5 with `crowdin status` — a per-language translation and proofreading snapshot, shown right after the first upload and before anything has had a chance to be translated. An empty-looking report here is expected, not a bug: the strings just arrived.

Mention `--fail-if-incomplete` once, as a forward pointer rather than something this phase runs itself: `crowdin status --fail-if-incomplete` exits non-zero when the project isn't fully translated, which is the ready-made release gate a CI job can build on once phase 7 has wired one up (the [`github-action`](../../github-action/SKILL.md) skill covers the step shape it takes there).
