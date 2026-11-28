# Astro for AI agents

Connect an AI assistant to [Astro](https://astro-bot.space), the Discord bot for temporary voice channels and voice roles, and let it read and change Astro's configuration in the servers you manage: generators, interfaces, voice roles, templates, the bot profile and language.

This repository is a [Claude Code](https://claude.com/claude-code) plugin marketplace with one plugin, `astro`, which contains:

- the Astro MCP server (remote, `https://mcp.astro-bot.space/mcp`)
- skills that teach the agent the workflows: [`configure-astro`](skills/configure-astro/SKILL.md) and [`diagnose-astro`](skills/diagnose-astro/SKILL.md)

You sign in with Discord the first time a tool is used. Read only access is enough to inspect a server, write access lets the agent change it. You can disconnect an app at any time from the Astro dashboard.

## Claude Code

```
/plugin marketplace add bot-astro/astro-mcp
/plugin install astro@astro-mcp
```

## Other clients

Add a remote MCP server (Streamable HTTP) with this URL, OAuth is discovered automatically:

```
https://mcp.astro-bot.space/mcp
```

The server also serves the skills as MCP resources and prompts. Agents that support [Agent Skills](https://agentskills.io) can use the `skills/` folder directly.

## Contributing

This repository is synced automatically from [bot-astro/astro](https://github.com/bot-astro/astro) (`services/mcp-server/plugin`), changes made here are overwritten. Open issues and pull requests there.
