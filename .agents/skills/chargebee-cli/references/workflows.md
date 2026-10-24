# Chargebee CLI — Workflows

Common workflows for interacting with Chargebee through the CLI.

## 1. First-Time Setup

Interactive:

```bash
chargebee auth add
chargebee auth status
```

Non-interactive:

```bash
chargebee auth add \
  --site acme-test \
  --api-key test_xxx

chargebee auth status
```

For CI or temporary environments, use environment variables instead:

```bash
export CHARGEBEE_SITE="acme-test"
export CHARGEBEE_API_KEY="test_xxx"

chargebee customer list
```

## 2. Multiple Sites

Create named profiles:

```bash
chargebee auth add \
  --profile test \
  --site acme-test \
  --api-key test_xxx

chargebee auth add \
  --profile prod \
  --site acme \
  --api-key live_xxx
```

Switch profiles:

```bash
chargebee auth switch prod
```

Use a profile for one invocation:

```bash
chargebee --use-profile test customer list
```

Check available profiles:

```bash
chargebee auth list
```

## 3. Discover an API Operation

Start with the resource:

```bash
chargebee docs customer
```

Then inspect the operation:

```bash
chargebee docs customer create
```

Use the returned parameter definitions to construct the command.

Do not guess parameter names or enum values.

## 4. Query a Resource

List:

```bash
chargebee customer list
```

Filter:

```bash
chargebee invoice list \
  -d "customer_id[is]=cust_123"
```

Retrieve:

```bash
chargebee subscription retrieve sub_123
```

## 5. Create or Modify a Resource

Before writing:

```bash
chargebee auth status
```

Make sure the connected site is a test site.

Then:

```bash
chargebee customer create \
  -d "first_name=Jane" \
  -d "last_name=Doe" \
  -d "email=jane@example.com"
```

Update:

```bash
chargebee customer update cust_123 \
  -d "email=new@example.com"
```

For important changes, retrieve the resource afterward to verify the result.

## 6. Product Catalog Differences

First check the connected site:

```bash
chargebee auth status
```

Typical PC1 resources:

```text
plan
addon
subscription create
```

Typical PC2 resources:

```text
item
item-price
item-family
subscription create-with-items
```

If an operation is blocked by the catalog gate, use the equivalent operation for the connected catalog. Do not bypass the gate.

## 7. Generate SDK Code

Discover the operation:

```bash
chargebee docs customer create
```

Generate code:

```bash
chargebee customer create \
  -d "first_name=Jane" \
  -d "email=jane@example.com" \
  --code-sample python
```

Generate another language:

```bash
chargebee customer create \
  -d "first_name=Jane" \
  -d "email=jane@example.com" \
  --code-sample nodejs
```

Use `--code-sample list` to see supported languages.

Use generated code as the source for production SDK examples rather than constructing SDK calls from memory.

## 8. Local Webhooks

Forward test-site webhook events:

```bash
chargebee listen \
  --forward-to localhost:3000/webhook
```

`listen` is available only for test sites.

There is no event-type filter; events for the site are forwarded to the configured URL.

## 9. AI Agent Setup

Install the Chargebee skill:

```bash
chargebee skills add --agent claude-code
chargebee skills add --agent cursor
chargebee skills add --agent universal
chargebee skills add --global --agent cursor --agent claude-code
```

Use `chargebee skills add --skill chargebee-cli` to select the skill explicitly.
`-s` is the short form, and the flag is repeatable. Omitting it installs `chargebee-cli` by default.

Check or refresh it:

```bash
# Show skills available to install (no installation)
chargebee skills add --list
# Check existing installations
chargebee skills list
chargebee skills update
chargebee skills update --global --agent cursor
chargebee skills remove --global --agent cursor
```

`add`, `update`, and `remove` prompt for scope unless `--project`, `--global`, or `--path` is specified. Use `--yes` to skip prompts. Without prompts, add/remove default to the project; update uses project installations when present, otherwise global installations. Removal requires confirmation or `--yes`. `list` shows project and global installations; `--global` limits any command to the user-wide scope; `--project` limits it to the current project. `--path` selects a project and conflicts with `--global`. Repeat `-a`/`--agent` to select multiple agents. Use `-s`/`--skill` across add/update/remove, or positional skill names with update/remove.

Inside a Git repository, the installed project skill is added to the repository's `.gitignore`.

If a project skill path resolves to a global installation, it is global only: project listing, update, and removal exclude it. The removal picker starts empty and groups agents sharing the same directory into one choice.

## 10. Common Recovery

| Situation | Action |
|---|---|
| Not configured | Run `chargebee auth add` or set both authentication environment variables. |
| Invalid API key | Verify the key belongs to the configured site. |
| Catalog mismatch | Check `chargebee auth status` and use the operation for that catalog. |
| Live-site write blocked | Switch to a test site. |
| Unknown command/resource | Run `chargebee resources` or `chargebee <resource> --help`. |
| Code sample unavailable | Use `chargebee docs <resource> <operation>` for the API reference and construct the request from the documented parameters. |
| Webhook listen blocked | Use a test site. |
| CLI bug or rough edge | Run `chargebee feedback "what happened"`. |
