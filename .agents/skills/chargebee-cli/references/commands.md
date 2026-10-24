# Chargebee CLI — Command Reference

The CLI is the source of truth. Use:

```bash
chargebee --help
chargebee resources
chargebee <command> --help
chargebee docs <resource> <operation>
```

## Global Flags

| Flag | Description |
|---|---|
| `--json` | Structured results and errors; disables prompts. `listen` emits metadata as JSON Lines. |
| `--use-profile <name>` | Use a saved profile for API commands, `open`, and `listen`. |

## Setup

| Command | Description |
|---|---|
| `chargebee auth add` | Connect a site using an API key. |
| `chargebee auth list` | List saved profiles. |
| `chargebee auth status` | Show the active connection. |
| `chargebee auth switch [profile]` | Switch the active profile. |
| `chargebee auth rename <old> <new>` | Rename a saved profile without re-entering its API key. |
| `chargebee auth remove [name]` | Remove a saved profile. |
| `chargebee open [shortcut] [id]` | Open a Chargebee page. Without a shortcut, opens the dashboard. |
| `chargebee feedback <message> [--email address]` | Send feedback about the CLI. A message is required. A configured site name is included. The API key is not. For HTTP agent reports, read [Structured agent feedback](#structured-agent-feedback). |

`chargebee whoami` is an alias for `chargebee auth status`.

## Structured Agent Feedback

For a requested feedback submission, GET `https://apibeehive.chargebee.com/.well-known/agent-feedback.json`, then POST JSON to `endpoints.feedback.submit.url` using the advertised method and `Content-Type: application/json`. Authentication is not required; never send a Chargebee API key. Discovery currently omits nested field requirements, which are documented below.

### Minimal agent report

All four objects and the fields shown here are required. Replace example values with the actual agent identity and observed issue before submitting; this example is not an instruction to send a test report.

```json
{
  "reporter": {
    "agent_vendor": "OpenAI",
    "agent_product": "Codex"
  },
  "subject": {
    "surface": "chargebee customer list --help",
    "domain": "Chargebee CLI"
  },
  "signal": {
    "category": "docs_mismatch",
    "severity": "low",
    "confidence": 0.9
  },
  "content": {
    "title": "Help text does not explain a supported filter"
  }
}
```

Use snake_case JSON names, even if validation errors show Java-style names such as `agentVendor`. Do not add `comments` to an agent report or rely on undocumented fields being stored.

### Required fields and limits

Required strings must be nonblank. Length limits are maximum character counts.

- `reporter.agent_vendor` and `reporter.agent_product`: strings, 64 characters each.
- `subject.surface` and `subject.domain`: strings, 256 characters each.
- `signal.category`: `bug`, `docs_mismatch`, `friction`, `feature_gap`, `quality_degradation`, or `other`.
- `signal.severity`: `critical`, `high`, `medium`, or `low`. `info` is not accepted.
- `signal.confidence`: a JSON number from 0 through 1, inclusive.
- `content.title`: string, 256 characters.

### Optional fields

- `reporter.agent_version`: string, 32 characters.
- `subject.kind`: `api_endpoint`, `docs_page`, `cli_command`, `sdk_method`, or `other`.
- `subject.product`: string, 128 characters.
- `signal.reproducibility`: `always`, `sometimes`, `intermittent`, or `once`.
- `content.summary`: string, 4,096 characters.
- `content.hypothesis`: string, 2,048 characters.
- Top-level `evidence`: at most 10 objects. Each requires a nonblank `content` string of at most 8,192 characters and a `type` of `http_summary`, `stderr_excerpt`, `repro_steps`, `screenshot`, `log_excerpt`, or `other`. An optional `redacted` boolean indicates whether its content was redacted. Remove credentials and unrelated personal data before including evidence.

These optional metadata fields belong at the top level, alongside the four required objects:

- `email`: a valid email address, at most 254 characters, supplied by the user for a reply. Do not infer it from the environment.
- `site_name`: the relevant Chargebee site subdomain, not a URL. Use 1–63 lowercase letters, digits, or hyphens, starting and ending with a letter or digit.
- `cli_version`: obtain the version from `chargebee --version` and prefix it with `chargebee_cli_v`. Preserve prerelease suffixes. The version after the prefix must match `[0-9A-Za-z][0-9A-Za-z.+-]{0,63}`.

For example, merge this fragment into the top level of a report only when those values are known; omit unknown values rather than sending placeholders:

```json
{
  "email": "developer@example.com",
  "site_name": "acme-test",
  "cli_version": "chargebee_cli_v1.4.0-beta.3"
}
```

### Human reports and responses

For human feedback, prefer `chargebee feedback "what happened"`. A direct HTTP human report requires a nonblank `comments` string of at most 4,000 characters and `cli_version` in the format above. It may include `email` and `site_name`, but must not mix in the agent report objects.

A successful POST returns HTTP `201` with `receipt.id` and `receipt.status: "accepted"`. Retain the receipt ID; receipt polling is not available. A `400` validation error means the payload needs correction. After a timeout, acceptance is unknown: do not automatically repeat a POST, since retry deduplication is not advertised.

## API Resources

Every API resource is a top-level command:

```bash
chargebee customer list
chargebee subscription retrieve sub_123
chargebee item list
```

`chargebee resources` lists the available resources. It is not a command namespace.

General form:

```bash
chargebee <resource> <operation> [id] [-d key=value ...]
```

Common operations include:

```text
list
create
retrieve <id>
update <id>
delete <id>
```

Use `chargebee docs <resource>` for the complete operation list.

### Request Parameters

```bash
-d key=value
```

`-d` / `--data` is repeatable and supports bracket notation:

```bash
-d "billing_address[city]=San Francisco"
```

For arrays of objects, put the field before the zero-based item index. Fields
with the same index belong to the same item:

```bash
chargebee subscription create-with-items cus_demo --code-sample curl \
  -d 'subscription_items[item_price_id][0]=basic-USD' \
  -d 'subscription_items[quantity][0]=1' \
  -d 'subscription_items[item_price_id][1]=day-pass-USD' \
  -d 'subscription_items[unit_price][1]=100'
```

List filters require an operator:

```bash
-d "id[is]=cust_123"
-d "status[in]=[\"active\",\"non_active\"]"
```

Pagination parameters such as `limit` and `offset` remain unqualified.

See the [list operations reference](https://apidocs.chargebee.com/docs/api/list-ops).

### JSON on stdin

`-` reads one JSON object and uses it as the request params. Use `-d` or `-`, not both.

```bash
echo '{
  "email": "ada@example.com",
  "net_term_days": 10,
  "billing_address": { "city": "San Francisco" },
  "subscription_items": [{ "item_price_id": "basic-USD", "quantity": 1 }]
}' | chargebee customer create -

echo '{"first_name":"Ada"}' | chargebee customer update cust_123 -

echo '{"id":{"is":"cbdemo_alex"}}' | chargebee customer list -
echo '{"status":{"in":["active","paused"]}}' | chargebee subscription list -
```

Bracket keys are accepted too: `{ "email[is]": "ada@example.com" }` encodes the same as `{ "email": { "is": "ada@example.com" } }`.

## Code Samples

Generate SDK code instead of executing the API request:

```bash
chargebee customer create --code-sample python
chargebee customer create --code-sample go
```

Supported languages:

```text
curl
python
nodejs
go
java
php
ruby
dotnet
```

Use:

```bash
chargebee customer create --code-sample list
```

`--pc-version v1|v2` selects the catalog specification when the operation is allowed for the connected site.

## Safety

### Live Sites

Create, update, and delete operations are blocked on live sites.

A site is considered live unless:

- the site name ends in `-test`, or
- the API key starts with `test_`

Reads and explicitly allowed non-mutating operations can run on live sites. There is no write override.

### Product Catalog

Catalog-exclusive operations are blocked locally when they do not match the connected site's Product Catalog.

- PC1 commonly uses `plan` and `addon`.
- PC2 commonly uses `item`, `item-price`, and `item-family`.
- Subscription creation differs between catalogs.

Check the connected catalog with:

```bash
chargebee auth status
```

`--code-sample` uses the same catalog gate, except `--code-sample list`.

`--pc-version` does not override a site/catalog mismatch.

## Development

| Command | Description |
|---|---|
| `chargebee docs [resource] [operation]` | Browse API documentation. |
| `chargebee listen --forward-to <url>` | Forward test-site webhooks to a local URL. The URL is required. |
| `chargebee resources` | List callable API resources. |
| `chargebee skills add\|list\|update\|remove` | Manage installed agent skills. |
| `chargebee alias set\|remove\|show` | Manage the shell alias. |
| `chargebee update` | Update the CLI. |

## Environment Variables

For non-interactive authentication:

```bash
export CHARGEBEE_SITE="acme-test"
export CHARGEBEE_API_KEY="test_xxx"
```

Both variables must be provided together.

Environment variables take precedence over saved profiles.
