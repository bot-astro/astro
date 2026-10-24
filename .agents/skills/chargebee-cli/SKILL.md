---
name: chargebee-cli
description: Use the Chargebee CLI to discover Chargebee APIs, query or modify Chargebee resources, forward test-site webhooks, and generate verified SDK code. Use when working with Chargebee CLI commands, Chargebee API operations, billing resources, subscriptions, customers, invoices, products, or integrations where the CLI can help.
---

# Chargebee CLI

Use the Chargebee CLI (`chargebee`) to interact with Chargebee from the terminal.

The CLI is the preferred interface for **API discovery, testing, scripting, debugging, and querying Chargebee data**. For production application code, use `--code-sample` to generate SDK code rather than writing SDK calls from memory.

## Core Workflow

Follow this sequence for unfamiliar Chargebee tasks:

**Discover → Validate → Execute → Verify**

### 1. Discover

Use the CLI's live API documentation instead of guessing resource names, operations, or parameters:

```bash
chargebee docs
chargebee docs <resource>
chargebee docs <resource> <operation>
```

`chargebee docs <resource> <operation>` provides the exact parameter names, types, required fields, and enum values.

Use `chargebee resources` to discover available API resources.

### 2. Validate

Before API operations:

```bash
chargebee auth status
```

Check the connected site's Product Catalog when the operation is catalog-specific.

Use a **test site for writes**. Live sites are read-only.

### 3. Execute

API operations follow:

```bash
chargebee <resource> <operation> [id] [-d key=value ...]
chargebee <resource> <operation> [id] -
```

Use `-d key=value` for request parameters. `-` reads one JSON object from stdin.

For production SDK code:

```bash
chargebee <resource> <operation> --code-sample <language>
```

Always prefer generated code over writing SDK syntax from memory.

### 4. Verify

For important changes, retrieve the resource afterward or inspect the command response to confirm the expected result.

## Critical Rules

### Live-site safety

Write operations are blocked on live sites. Never attempt to bypass this protection.

Use a test site for create, update, and delete operations.

### Product Catalog

Chargebee supports Product Catalog 1.0 and 2.0. Catalog-specific operations are gated against the connected site's catalog.

Do not bypass a catalog mismatch.

`--code-sample` follows the same catalog gate. The write gate does not apply because code generation does not execute an API request.

### Code Samples

Use `--code-sample` whenever the user needs Chargebee SDK code.

The generated output comes from Chargebee's code-sample generator and should be preferred over manually constructed SDK code.

### Authentication

Use `chargebee auth add` for API-key authentication. `chargebee login` only prints setup guidance; OAuth login/logout are not implemented.

Saved profiles are configured with:

```bash
chargebee auth add
```

For non-interactive use:

```bash
export CHARGEBEE_SITE="acme-test"
export CHARGEBEE_API_KEY="test_xxx"
```

Environment variables take precedence over saved profiles.

Never expose, log, or reproduce API keys.

### Composability

Chargebee CLI commands return JSON and can be combined with standard command-line tools such as `jq`.

See [references/composability.md](references/composability.md) for scripting and agent patterns.

## Feedback

Send CLI feedback from the terminal:

```bash
chargebee feedback "what happened"
chargebee feedback "what happened" --email you@example.com
```

A message is required. The configured site name is included when one is available. The API key is not.

Agents that submit a structured report start from the discovery document. GET it, then POST to `endpoints.feedback.submit.url` in the response:

```
https://apibeehive.chargebee.com/.well-known/agent-feedback.json
```

Before constructing a structured report, read [the feedback payload reference](references/commands.md#structured-agent-feedback) for a minimal valid example, required fields, limits, and accepted values. Discovery currently does not describe the complete payload. JSON field names use snake_case, including `reporter.agent_vendor` and `reporter.agent_product`.

Agent reports accept optional top-level `email`, `site_name`, and `cli_version`; omit unknown values and never include an API key. Human HTTP reports require `comments` and `cli_version`. The `chargebee feedback` command supplies the CLI version and available configured site automatically; `--email` supplies the optional contact email.

## References

- [references/commands.md](references/commands.md) — CLI commands, flags, safety behavior, and resource syntax
- [references/workflows.md](references/workflows.md) — common end-to-end workflows
- [references/composability.md](references/composability.md) — piping, scripting, and agent patterns

When the reference files and the CLI disagree, **the CLI is the source of truth**. Use `chargebee --help`, `chargebee resources`, and `chargebee docs` to verify current behavior.

For automation, pass `--json` for structured results and errors across commands. Supply inputs explicitly; this mode never prompts. `listen --json` emits JSON Lines with status and forwarding metadata only, without webhook payloads. See [composability](references/composability.md).
