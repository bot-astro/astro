---
name: feedback
description: Drafts feedback about Crowdin for the Crowdin team - the skills in this plugin, the Crowdin MCP server, the Crowdin CLI, the Crowdin API or the product - and sends it as a GitHub issue in crowdin/skills only when the user explicitly asks. Use when the user asks to report or file something about Crowdin; after a Crowdin skill instruction, CLI command, MCP tool call or API call failed reproducibly and was just fixed or given up on; when the user is clearly frustrated with a Crowdin component; or when a Crowdin capability the user expected does not exist and blocked the task. Feedback about the agent itself belongs to the agent's own feedback mechanism.
---

# Feedback

Turns a problem with a Crowdin component (a skill in this plugin, the Crowdin MCP server, the Crowdin CLI, the Crowdin API, the product) into a short, factual draft on the user's machine and, only when the user asks, into a GitHub issue the Crowdin team reads.

## When to draft

Draft at high-signal moments, with or without being asked:

- The user asks: "report this", "file feedback", "this should be an issue", "tell Crowdin about this", "the skill was wrong about X".
- A Crowdin skill instruction, CLI command, MCP tool call or API call failed reproducibly and was just fixed or given up on.
- The user expressed clear frustration with a Crowdin component.
- A Crowdin capability the user reasonably expected does not exist and blocked the task.

Type each draft as one of `bug` (something behaves differently from what the documentation, the skill or the user reasonably expected), `idea` (a change that would have made the task easier) or `missing_capability` (something Crowdin cannot do that blocked the task).

One draft per distinct problem per session. Before writing, list `~/.crowdin/feedback/`; if a draft there describes the same problem, update it rather than create a second one. The same failure hit twice in different places is still one problem.

Drafting is silent. Finish the current reply as you would have, then add one line at the very end: `Drafted feedback about <short title> to ~/.crowdin/feedback/<file>. Say "send feedback" to review and send it.` That line is the only trace of the draft in the reply; questions about it wait until the user brings it up. When the user asked for the draft explicitly, show the draft in the reply instead of that line.

## Where drafts live

Write drafts to `~/.crowdin/feedback/<yyyy-mm-dd>-<slug>.md`, where the slug is three to six lowercase words joined by hyphens that name the problem. Create the folder on first use. The folder sits outside any repository, so a draft is never committed with the user's code.

Assume no shell exists until you see one. Where there is no filesystem or shell, as in Claude Cowork, ChatGPT or a skill uploaded to Claude.ai, put the same draft inline in the reply and store nothing; the user copies it from there.

## The draft

The file body is exactly what will become the issue body: no YAML frontmatter, since GitHub renders `---` in an issue as a horizontal rule. Labeled lines in this order, one to three lines each, facts only, no narrative:

```markdown
**Type:** bug
**Component:** api
**Agent:** Claude Code 2.1.269
**Plugin version:**

**What happened:** `POST /api/v2/projects/{projectId}/translations/builds` returned 500 with `{"error":{"message":"Internal Server Error"}}` for a project with one branch and eight target languages. Expected a build identifier, or a 4xx that names what is wrong with the request.
**What the user said:** "third time today, I can't ship without the build"
**Repro:** Call the endpoint with an empty JSON body on a file-based project that has one branch; three attempts one minute apart returned the same response.
**Evidence:** 2026-09-14T10:32:07Z, 10:33:11Z and 10:34:20Z (UTC); the request body was `{}`; the same call on a project without branches succeeded.
```

- `Component` is the skill name for a skill (`crowdin-cli`, `how-to-crowdin` and so on), or `mcp-server`, `cli`, `api` or `product`.
- `Agent` is the agent tool and version when the session shows them, otherwise blank. `Plugin version` is the version from the plugin manifest when the host exposes one, otherwise blank.
- `What happened` states observed versus expected, with the exact error text when it is short.
- `What the user said` quotes the user's own words; the quote carries the weight, so it stays as said. When the user said nothing about it, write `User didn't comment; observed by the model.`
- `Repro` gives the minimal steps, or the shape of input, that reproduces it.
- `Evidence` lists identifiers a reader can chase: timestamps, request identifiers from API error bodies, the Crowdin CLI version, paths relative to the working directory or `~`-prefixed. Run `crowdin --version` for it only when the CLI was involved and a shell exists. Omit the line when there is nothing.
- `Cause` appears only for a root cause verified in this session; otherwise omit the line.

Everything comes from the session or the user. A field the session did not supply stays blank; an empty field beats a plausible one.

## Privacy

Apply these while drafting, so the file on disk is already safe:

- No secrets or credentials of any kind: no Crowdin personal tokens or OAuth secrets, no value of `CROWDIN_PERSONAL_TOKEN`, nothing from `crowdin.yml` that looks like a credential, no API keys of other services.
- No Crowdin organization names, project names or project identifiers. Describe the shape instead: "a file-based project with twelve target languages".
- No source strings, translations, glossary terms or file contents from the user's project. Replace them with a placeholder that keeps the shape: "a string with two ICU plural branches", "a 40 KB JSON file with nested keys".
- People by role, never by name, inside quotes too: "[a translator]", "[the project manager]", "[the user]".
- Paths relative to the working directory or `~`-prefixed, never absolute paths that reveal a user name.
- No customer channel or direct-message identifiers and no customer content.
- If the problem looks like a security vulnerability, describe the class of problem only ("a permission check that can be bypassed on one endpoint") and, when sending, prefer the private route below.

## Sending

Send only when the user explicitly asks, and only the draft they name, or the single queued draft when there is one. Show the draft first unless it is already visible in the reply, so what they approve is what goes out. "Send" after seeing the draft is the consent; never send on an earlier or general instruction.

The title is `[<type>] <component>: <short title>`, for example `[bug] api: translation build returns 500 on a project with one branch`. The type lives in the title because labels need triage permission on the repository, which most submitters do not have.

Then, in this order:

| Situation | Do this |
|---|---|
| A shell exists and `gh auth status` succeeds | `gh issue create --repo crowdin/skills --title "<title>" --body-file <draft path>` |
| `gh` is missing or not authenticated, or there is no shell | Give the user a prefilled link: `https://github.com/crowdin/skills/issues/new?title=<url-encoded title>&body=<url-encoded body>`. They open it and press Submit. |
| The encoded link would exceed 8000 bytes, or the user says the content is sensitive | Hand over the draft text and point to `https://github.com/crowdin/skills/issues/new`, or to Crowdin support at `https://crowdin.com/contacts` for anything they do not want public. Send nothing. |

Encode the title and body fully; the one-to-three-line rule keeps ordinary drafts far below the 8000-byte ceiling.

After a successful `gh` send, prepend `Sent: <issue URL>` as the first line of the draft and move the file to `~/.crowdin/feedback/sent/`. After handing over a link, leave the draft where it is; you cannot know whether the user submitted it.

Issues in `crowdin/skills` are public. Say so in one clause when you hand over a link or before running `gh`, unless the user has already been told in this session.
