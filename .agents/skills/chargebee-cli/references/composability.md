# Composability — Piping, Scripting & Agent Patterns

API commands return JSON by default. Use `--json` for compact API results and structured built-in results and errors. Standard tools such as `jq` work with this output.

## Piping with jq

```bash
chargebee customer list \
  | jq -r '.list[].customer.email'
```

```bash
chargebee subscription list -d "status[is]=active" \
  | jq -r '.list[].subscription.id'
```

```bash
chargebee invoice list -d "limit=100" \
  | jq '.list | group_by(.invoice.status) | map({status: .[0].invoice.status, count: length})'
```

## Capture Values

Use command substitution when a later command needs an ID:

```bash
CUSTOMER_ID=$(chargebee customer create \
  -d "email=test@example.com" \
  | jq -r '.customer.id')

chargebee customer retrieve "$CUSTOMER_ID"
```

## Scripting

A common pattern is:

```bash
chargebee subscription list \
  | jq -r '.list[].subscription.id' \
  | while read -r id; do
      chargebee subscription retrieve "$id"
    done
```

For scripts:

- Use `--json`; successful commands emit one JSON value on stdout.
- Failures emit `{ "error": { "code", "message", "exit_code" } }` on stderr; optional `details` contain API error information. Success warnings are a separate JSON object on stderr.
- Check the command exit code.
- Quote values that may contain spaces or shell characters.

## Agent Workflow

For unfamiliar API tasks:

```bash
chargebee auth status --json
chargebee docs <resource> <operation> --json
chargebee <resource> <operation> ...
```

Use `chargebee docs` for discovery instead of guessing parameter names.

For production SDK code:

```bash
chargebee <resource> <operation> \
  --code-sample <language>
```

Do not manually construct Chargebee SDK calls when a code sample is available.

## Long-running listeners

`chargebee listen --forward-to <url> --json` emits one JSON record per stdout line:
`connecting`, `ready`, `forward_result`, `forward_failed`, and `stopped`. Records
include a timestamp. Warning records and terminal errors go to stderr. These are
listener/forwarding metadata, never full webhook or response bodies.

JSON mode never prompts. Supply required arguments, use `--agent` for skill
installation, and pass `--yes` explicitly when confirming removal. JSON formatting
does not bypass write or catalog gates. With `--code-sample`, read the `code` string
and `language` field from the JSON result.
