---
name: astro-skill-guidelines
description: Rules for writing and maintaining the Astro project's own skills (where they live, format, what to include and what to leave out). Use when creating, editing, splitting or reviewing a skill in this repo.
---

# Writing Astro skills

A skill is a short briefing that gives an agent the context it can't get cheaply from the code. If the code or a doc already says it, the skill points there instead of repeating it.

## Where skills live

- The real files go in `.agents/skills/<name>/SKILL.md`.
- `.claude/skills/<name>` is a **symlink** to `../../.agents/skills/<name>`. Every skill needs one.
- Third-party skills (Crowdin, Infisical, Sentry...) are vendor-managed and tracked in the lock file. Don't edit them; project skills use the `astro-` prefix.

## Format

- Frontmatter has only `name` (same as the folder) and `description`.
- The description says **what** the skill covers and **when** to load it. It is the only thing the agent sees before deciding to load, so make the trigger concrete.
- Keep `SKILL.md` short, ideally under ~100 lines. Move bulky material into `references/*.md` next to it and link to it from `SKILL.md` with a line saying when to read it.
- Use plain prose, short bullets and small tables. No filler, no history, no "how we got here".

## Content rules

Include:
- Concepts, responsibilities, relationships, and the "why" behind non-obvious decisions.
- The name of the tool or system to use for a job, and what it is good for.
- Pointers to the doc, repo or folder that holds the detail.

Leave out, because it goes stale or is one grep away:
- Class, function, file or line references. Module and repo names are fine.
- Versions, counts, prices, limits, IDs, hostnames that change, thresholds.
- Step-by-step procedures that belong in `docs/`, a runbook or a script.
- Secrets, tokens and credentials of any kind.
- Anything that restates `CLAUDE.md`.

Write for the general case. If a sentence stops being true after a routine change, rewrite it at a higher level or delete it.

## Scope

- One skill, one job. Split when a skill covers two unrelated triggers.
- A skill that describes something should be broad; a skill that tells how to do something should be narrow and actionable.
- Prefer linking skills to each other over duplicating content.

## Maintenance

- When a fact in a skill turns out wrong, fix it in the same change that discovered it.
- When adding a new service, tool or product area, update the matching skill (product, tech overview, services and tools).
- Review new skills by asking: could this go stale from a normal code change? If yes, abstract it.
