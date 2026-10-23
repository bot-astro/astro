---
name: graphql
description: Write valid, schema-aware Crowdin GraphQL queries and debug invalid ones. Use this whenever the user asks for Crowdin GraphQL queries, filters, pagination, sorting, or when they share GraphQL errors from Crowdin Playground (especially unknown argument/field errors) and need a corrected query that actually matches the schema.
---

# Crowdin GraphQL Query Writing

Use this skill to produce valid Crowdin GraphQL queries that match the live schema, avoid unsupported arguments, and include practical pagination/filtering patterns.

## What to Deliver

Return:
1. A working GraphQL query.
2. A short explanation of how it works.
3. If relevant, a corrected version of the user's broken query plus a brief "what was wrong".
4. Optional: variables object when it improves readability.

## Core Workflow

### 1) Confirm intent and scope

Identify:
- Target resource (projects, strings, translations, etc.).
- Required constraints (language, dates, status, user, approval state).
- Output fields user needs.

If the request is ambiguous, choose a conservative default and say so briefly.

### 2) Build from schema-safe primitives

Use this sequence:
1. Pick the root field that actually exposes the target connection.
2. Add required pagination arguments (`first` or `last`) on every connection.
3. Add nested selections (`edges -> node`) and only requested fields.
4. Add filtering/sorting only where the schema supports it.

Do not invent arguments by analogy with REST parameters.

### 3) Validate against common Crowdin GraphQL constraints

Before finalizing:
- Every queried connection has `first` or `last` (1..10000).
- Estimated total requested nodes stay under 10,000.
- Query includes `pageInfo` when pagination is likely needed.
- Query avoids unsupported arguments on a field.

### 4) Add safe pagination pattern

Prefer cursor pagination:
- Forward: `first` + `after`
- Backward: `last` + `before`

If user asks for "from date X to date Y", use supported `filter` input fields (for resources that expose them), not ad-hoc args.

## High-Risk Mistakes to Prevent

### Unknown argument errors

If the user query contains field arguments that are not in the schema, rewrite the query using supported alternatives.

Example anti-pattern from real failures:
- `translations(..., createdAfter: "...", createdBefore: "...")`

Fix strategy:
1. Remove unsupported args.
2. Keep supported args like `languageId`, `first`, `after`.
3. If date constraints are required, move them to a supported `filter` input on fields that provide one (or explain that this field does not expose date filtering directly).

### Missing required pagination arguments

Crowdin GraphQL connections require `first` or `last`. Never omit them.

### Over-fetching

Do not request large nested fan-outs without reason. Keep node count budgeted.

## Query Patterns

### Pattern A: Minimal safe connection query

```graphql
query GetProjects($first: Int!, $after: String) {
  viewer {
    projects(first: $first, after: $after) {
      edges {
        node {
          id
          name
        }
        cursor
      }
      pageInfo {
        hasNextPage
        endCursor
      }
      totalCount
    }
  }
}
```

### Pattern B: Filtering + ordering (where supported)

```graphql
query FilteredProjects {
  viewer {
    projects(
      first: 10
      filter: { createdAt: { gt: "2023-01-01T00:00:00Z" } }
      order: [{ lastActivityAt: asc }]
    ) {
      edges {
        node {
          id
          name
          lastActivityAt
        }
      }
    }
  }
}
```

### Pattern C: Rate limit awareness

```graphql
query MeAndCost {
  viewer {
    username
  }
  rateLimit {
    limit
    cost
    remaining
    resetAt
  }
}
```

## Debugging Template

When the user provides an error, follow this output format:

1. **Cause**: one sentence pointing to schema mismatch.
2. **Fixed query**: full corrected query.
3. **Why this works**: 2-4 bullets tied to schema rules.
4. **If date filtering was requested**: explain whether that specific field supports date filtering; if not, propose nearest valid alternative.

## Tone and Safety

- Prefer correctness over being "clever".
- If the schema support is uncertain, explicitly say "verify in Playground Explorer for this field".
- Never pretend an argument exists.

## Reference

For constraints and examples, align with Crowdin docs:
- GraphQL API overview, pagination, filtering/sorting, and limits.
- https://support.crowdin.com/developer/graphql-api/
