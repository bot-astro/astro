# MCP server for api-central

Experiment: generate an MCP server from the api-central OpenAPI spec with [Cloudflare Forge](https://github.com/cloudflare/forge) (`@cloudflare/forge` 0.3.0).

## Status

- The spec is improved for tool generation (see below).
- `forge generate --target mcp` works on the real spec and gives 35 tools.
- A smoke test with a stdio MCP server and a client lists the 35 tools (about 38 KB of schemas) and calls them. Without a session cookie every call fails with `401 UNAUTHENTICATED`, which is expected. An authenticated call was not tested.
- api-central is an OAuth authorization server for MCP clients with CIMD (see "Authentication").
- The API follows the usual conventions for machines that retry and act on their own (see "API conventions"): request ids, one error shape, idempotency keys, partial updates, dry run, read and write scopes. They were tested against a local api-central with seeded data, the spec was not regenerated with Forge since.
- The MCP server is in `services/mcp-server` (TypeScript, streamable HTTP, stateless). It exposes 22 tools so an agent can read a server's configuration and change it (see "The server"). It is tested against a stub api-central (`npm test`), not against a real login: a real token needs a Discord login through the OAuth flow.

## The server

`services/mcp-server`, `npm run dev` (needs api-central running, Node 24):

- Resource server only. `GET /.well-known/oauth-protected-resource/mcp` points at api-central as the authorization server, `/mcp` answers `401` with `WWW-Authenticate: Bearer resource_metadata="..."` without a valid token. A token is valid if `POST /oauth/introspect` on api-central says it is active, its audience is this server and it has `guilds:read`. It is forwarded as is to every tool call, so api-central keeps enforcing scopes and guild permissions. With a read only token the server lists only the tools with `readOnlyHint`. CORS is open (`*`, no cookies) on `/mcp` and the metadata for browser clients.
- Config: `MCP_PORT` (9002), `MCP_PUBLIC_URL` (`http://localhost:9002/mcp`), `API_CENTRAL_URL` (`http://localhost:9001`), `API_CENTRAL_PUBLIC_URL` (issuer, same as `CA__PUBLIC_URL` of api-central).
- Every `POST` and `PATCH` gets a fresh `Idempotency-Key`.
- Skills: `services/mcp-server/plugin/skills/*/SKILL.md` are Agent Skills (the open `SKILL.md` format that Claude Code, Codex, Cursor, Gemini CLI and others read), written once and used three ways: the server sends them as `instructions` on `initialize` and serves them as MCP resources (`skill://astro/<name>/SKILL.md`) and prompts, so any MCP client gets them; the `plugin` folder is a Claude Code plugin (`.claude-plugin/plugin.json`, `.mcp.json`, `skills/`); and the folders can be copied to the skills directory of any agent that supports the format. Skills: `configure-astro` (workflow and rules for changes) and `diagnose-astro` (find why Astro misbehaves). `plugin/.mcp.json` points at localhost, set the production URL before publishing it.
- Tools (`src/exposed-tools.ts`, an allow-list over the generated ones): list guilds, channels, roles and channel permissions; get the settings; update settings and bot profile (`PATCH`, `dryRun` available); create, update, delete generators, interfaces (also move), voice role connections and templates; set the updates channel; list guild errors.
- Not exposed: billing, premium, media, the setup wizard, `auth.me`, `discord.users.getSelf` (it returns the email, so that open decision is closed), clearing the VC cache and the error log.
- No ad-hoc endpoints were needed: the `PATCH` endpoints are partial updates with `dryRun`, so the model does not resend whole objects. Writes return the whole guild settings, no slim responses (decided).
- `src/tools` is generated, do not edit. Regenerate with `curl localhost:9001/docs/openapi.json -o openapi.json && npm run generate`.

## Reproduce

```sh
# infra (Docker) and the service, port 9001 comes from Infisical
docker compose up -d mongo redis
infisical run --env=dev -- ./gradlew :services:api-central:bootRun

curl localhost:9001/docs/openapi.json -o openapi.json
npx @cloudflare/forge@0.3.0 generate openapi.json --target mcp --out tools
```

Forge generates only the tools as data (`tools/index.ts` exports `tools` and `registerTools`). The server around them is ours:

```ts
import { McpServer } from '@modelcontextprotocol/server';
import { StdioServerTransport } from '@modelcontextprotocol/server/stdio';
import { registerTools, tools } from './tools/index.js';

const server = new McpServer({ name: 'astro', version: '0.0.1' });
registerTools(server, tools, {
  baseUrl: 'http://localhost:9001',
  fetch: (input, init) => {
    const request = new Request(input, init);
    request.headers.set('cookie', `session=${process.env.ASTRO_SESSION}`);
    return fetch(request);
  },
});
await server.connect(new StdioServerTransport());
```

## What changed in the spec

Springdoc builds the spec from the code. It reads types, nullability, enums and the swagger annotations. It does **not** read KDoc, so descriptions need `@Operation` / `@Schema` (kept to the places where the name alone is not enough).

- `OpenApiConfiguration`:
  - API description (auth, errors, premium, id format), tag descriptions, `AErrorCode` meanings.
  - Fixed the security scheme, which was registered as `sessionCookie` but referenced as `session`. It is now global.
  - `OperationCustomizer`: describes the path parameters (`guildId`, `generatorId`...) and adds the 400/401/403/404/429/500 `AErrorResponse` responses to every operation, so controllers don't repeat them.
  - `OpenApiCustomizer`: marks non-null properties as `required`. Springdoc does not, so tools would see every response field as optional.
- Controllers: forge-style `operationId` (`guilds.generators.update`, forge splits at the last dot into group and method), descriptions, `x-forge-require-confirmation` on destructive operations, `x-forge-skip` on login, logout, media upload and the webhooks (webhooks are under a `Webhooks` tag).
- The full replace `POST` updates are `*.replace` and `x-forge-skip`, the tools see the `PATCH` ones (`*.update`).
- Property names are snake_case, like the JSON the API really reads and writes (the global mapper is snake_case, springdoc documented camelCase before, so generated tools would have sent fields the API ignores). `snakeCaseModelResolver` fixes it.
- The wrong "Get bot user" summary of `/v2/discord/users/@me` is fixed, it returns the logged in user.
- The explicit 404 `@ApiResponses` on the Discord channels and roles endpoints were removed: they replaced the default 200 response, so the spec had no success schema for them.

## Forge findings

- No Arazzo, workflow or overlay support in the repo (grepped). The MCP target is one tool per operation. Levers: `operationId`, `x-forge-*` extensions, and mapping over the generated `tools` before registering.
- Tool name is `group_method` in camelCase (`guilds_generators_update`). The arguments mirror the HTTP request: `{path, query, body}`.
- Annotations come from the HTTP method. Our "update" endpoints are `POST`, so they get `idempotentHint: false`. `x-forge-require-confirmation` sets `destructiveHint` and adds a "Confirm with the user" line.
- A tool's `outputSchema` is the success body, and clients validate it.

## Findings in the spec that remain

- The shared Mongo entities (`GuildSettingsEntity`, `GeneratorSettings` with about 45 fields, `InterfaceData`, `TemplateData`) are request and response bodies, with no descriptions (`shared:core` has no swagger dependency).
- `GuildSettingsEntity` also exposes `javaLocale` (a computed property) and an `id` that is an `ObjectId` object.
- Every write returns the whole guild settings.
- `updatesChannel.update` (`PUT`) and `interfaces.move` have no `PATCH` or dry run, they are not partial updates.
- `discord.users.getSelf` returns the user's email.

## Authentication

api-central is the OAuth authorization server for MCP clients (hand rolled, `OAuthServerController`), Discord is only the login step:

1. The client sends the user to `GET /oauth/authorize` with `client_id` = the HTTPS URL of its metadata document (CIMD). api-central fetches it (`McpClientMetadataService`: https only, public hosts only, no redirects, 5 s, 10 KB), requires the `redirect_uri` to be listed in it and PKCE `S256`.
2. If the user has no session cookie, it goes through the normal Discord login (`/v2/auth/discord`, the OAuth state carries the `mcp_request_id` in its own Redis key). The Discord callback then redirects to `/oauth/consent` instead of the dashboard.
3. The consent page shows the client name and host, Allow or Deny redirects to the client's `redirect_uri` with `code` (or `error`), `state` and `iss`.
4. `POST /oauth/token` exchanges the code (PKCE checked, one use, 1 minute) for an opaque access token (1 hour) and a refresh token (30 days, rotated on every use). Only SHA-256 hashes are stored in Redis (`MCP_*` keys in `McpOAuthService`).
5. `AuthFilter` accepts `Authorization: Bearer`, it resolves the user and uses the stored Discord token like it does for the cookie. Permissions per guild are the same as in the dashboard.

Hardening:

- **Audience (RFC 8707)**: `resource` on authorize and token must be `CA__MCP_RESOURCE_URL` (`invalid_target` otherwise, a missing one defaults to it). It is stored in the code and the tokens, and `/oauth/introspect` returns it as `aud`, the MCP server rejects any other.
- **Scopes**: introspection returns `scope`, the MCP server hides the write tools from read only tokens. The consent page lets the user grant read only when the client asked for write, the granted scopes are in the token response.
- **Refresh token reuse**: every authorization is a grant (`MG:` key, 30 days, renewed on rotation) and all its tokens check it. A rotated refresh token is remembered (`MRU:`), using it again deletes the grant, so the leaked and the legitimate tokens stop working (access tokens within seconds, they check the grant on every request). A client that retries a refresh after a timeout also hits this and has to log in again.
- **Code reuse (RFC 6749 4.1.2)**: an exchanged code is remembered for 10 minutes (`MACU:` → grant id), using it again deletes the grant.
- **Revocation (RFC 7009)**: `POST /oauth/revoke` (in the metadata as `revocation_endpoint`) always answers 200. A refresh token deletes its grant, an access token only itself.
- **Connected apps**: `GET /v2/auth/grants` lists the active grants of the user (`id`, `client_id`, `client_name`, `scopes`, `created_at`, `last_used_at` from `MGL:`, updated on every Bearer request), `DELETE /v2/auth/grants/{id}` deletes one (204, 404 if not the user's). Only with the dashboard session, a Bearer token gets 403. Marked `x-forge-skip`, so they never become tools.
- **Rate limit**: authenticated requests (cookie or Bearer) share one bucket per user (`UserIdRateLimitFilter`), anonymous ones are limited by IP, so MCP users don't share the bucket of the MCP pod's IP.
- **Logout**: the dashboard logout keeps the Discord token while the user has active grants (`MUG:` set), so MCP tokens survive it.
- **CORS**: `/oauth/token` and the metadata allow any origin without credentials.
- **CIMD fetch**: OkHttp with a DNS lookup that rejects non public addresses, so the connection goes only to the checked addresses.

Tests: `OAuthServerFlowTest` (`@SpringBootTest`, Testcontainers Mongo and Redis, Discord and the CIMD fetch mocked) covers authorize, consent, token, refresh, refresh and code reuse, introspection, revocation and the grants endpoints. Run with `./gradlew :services:api-central:test`, it needs Docker.

Not done yet:

- Set `CA__PUBLIC_URL` (issuer) and `CA__MCP_RESOURCE_URL` in api-central and the matching variables of the MCP server in each environment, and deploy it (no Dockerfile or manifests yet).

## API conventions

Modelled on what Stripe and similar APIs do, because an LLM client retries, times out and edits without seeing the whole object.

| Feature | How it works |
|---|---|
| Request id | `RequestIdFilter` reuses the `X-Request-Id` that ingress-nginx adds to every request (replaced if it is not 8 to 128 safe characters), or generates `req_...`. It is in the MDC (log pattern `[%X{requestId}]`), the `X-Request-Id` response header and `request_id` of errors. Exposed through CORS. |
| One error shape | `AErrorResponse`: `code`, `message`, `param` (field path like `rename_conditions.x` or query parameter), `request_id`. The rate limit filter and the security handlers go through `ControllersExceptionHandler` too. The OAuth `/oauth/token` keeps the RFC 6749 shape. |
| Retry-After | On 429 (our limiter and Discord's) and on `409 IDEMPOTENCY_IN_PROGRESS`, from `AException.retryAfterSeconds`. |
| Idempotency | `Idempotency-Key` header on `POST` and `PATCH` (`IdempotencyFilter`). Per user, 24 h in Redis (`IDK:` keys). Only 2xx responses are stored and replayed (`Idempotent-Replayed: true`), a failed request releases the key. Same key with another request: 422 `IDEMPOTENCY_KEY_REUSED`. Concurrent: 409. Not on multipart. The MCP server should send a fresh key per tool call. |
| Partial updates | `PATCH` on the update endpoints of generators, interfaces, connections, templates, guild settings and the bot profile (`MergePatchHelper`, JSON Merge Patch). Nested objects merge, lists are replaced, null resets to the default. Unknown fields are a 400. The body schemas are generated (`GeneratorSettingsPatch`...) with nothing required. |
| Dry run | `?dryRun=true` on those same `PATCH` endpoints (`@SupportsDryRun`): everything runs, nothing is saved or sent to Discord, the would-be guild settings are returned. Any other endpoint answers 400 (`DryRunInterceptor`). It is a query parameter, not a header, so it is a normal tool argument. |
| Scopes | `guilds:read` for `GET`/`HEAD`, `guilds:write` for everything else and it includes read. Enforced once in `SecurityConfig`, no scope requested means read only, the dashboard cookie has both. A missing scope is 403 `INSUFFICIENT_SCOPE` with `WWW-Authenticate: Bearer error="insufficient_scope"`. |

Not done on purpose: pagination (lists are small), `expand`, prefixed ids, date based versions.

## Options

| Option | Pros | Cons |
|---|---|---|
| A. 1:1 tools on the current endpoints | No backend work | A model must send the full `GeneratorSettings` back and can clobber fields. Big outputs. `POST` updates have wrong hints. |
| B. A on a curated subset (`x-forge-skip`, tool overrides in the server) | Cheap, fine for read tools | Semantics don't change |
| C. Task-shaped endpoints for MCP (for example slim responses) | Safe small tools, correct hints, forge generates them as they are | New controller layer to maintain. The `PATCH` part of it is done, writes still return the whole guild settings |

Recommendation: B for the read tools (guilds, channels, roles, settings, errors), C for writes.

## Open decisions

- Writes keep returning the whole guild settings, no slim responses (decided).
- Test with a real MCP client (OAuth login through Discord) and tune the tool descriptions from what the agent gets wrong.
