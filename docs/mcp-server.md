# MCP server for api-central

Experiment: generate an MCP server from the api-central OpenAPI spec with [Cloudflare Forge](https://github.com/cloudflare/forge) (`@cloudflare/forge` 0.3.0).

## Status

- The spec is improved for tool generation (see below).
- `forge generate --target mcp` works on the real spec and gives 35 tools.
- A smoke test with a stdio MCP server and a client lists the 35 tools (about 38 KB of schemas) and calls them. Without a session cookie every call fails with `401 UNAUTHENTICATED`, which is expected. An authenticated call was not tested.
- No server is committed. Auth and the choice between 1:1 tools and task-shaped tools are still open.

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
- Update endpoints replace the whole object and every write returns the whole guild settings.
- `discord.users.getSelf` returns the user's email.

## Options

| Option | Pros | Cons |
|---|---|---|
| A. 1:1 tools on the current endpoints | No backend work | A model must send the full `GeneratorSettings` back and can clobber fields. Big outputs. `POST` updates have wrong hints. |
| B. A on a curated subset (`x-forge-skip`, tool overrides in the server) | Cheap, fine for read tools | Semantics don't change |
| C. Task-shaped endpoints for MCP (for example `PATCH` one generator field, slim responses) | Safe small tools, correct hints, forge generates them as they are | New controller layer to maintain |

Recommendation: B for the read tools (guilds, channels, roles, settings, errors), C for writes.

## Open decisions

- **Auth.** api-central only accepts the Discord OAuth session cookie. The MCP server needs a service token plus acting user, a per-user MCP token, or to run the OAuth flow itself.
- Which of A, B and C to build for the write tools.
- Whether to expose the user's email through `discord.users.getSelf`.
