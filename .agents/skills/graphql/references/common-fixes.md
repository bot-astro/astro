# Common Crowdin GraphQL Fixes

Use this reference when troubleshooting invalid queries.

## 1) Unknown argument on field

Symptom:
- `Unknown argument "X" on field "Y"...`

Action:
1. Remove unsupported argument.
2. Check field arguments in Playground Explorer.
3. Reintroduce constraints only through supported args/input objects.

Example:
- Invalid: `translations(..., createdAfter: "...", createdBefore: "...")`
- Safer: `translations(languageId: "uk", first: 100, after: $cursor)`

## 2) Missing pagination argument

Symptom:
- Validation error about missing `first` or `last` on connection.

Action:
- Add `first` (or `last`) to every connection level you query.

## 3) Excessive node count

Symptom:
- Query rejected or risky due to >10,000 node requests.

Action:
1. Reduce nested `first` values.
2. Split one large query into multiple narrower calls.
3. Request only needed fields.

## 4) Rate-limit visibility

To inspect query cost in practice:

```graphql
query {
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
