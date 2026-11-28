# MCP server: from experiment to production

Plan to deploy the MCP server (`services/mcp-server`, see `mcp-server.md`) and publish it in the official connector directories, the MCP Registry and the website.

## Decisions

| | Decision |
|---|---|
| D1 Tool generation | `openapi.json` and `src/tools` are committed, CI regenerates them and fails on a diff. Tool descriptions are prompts, changes are reviewed in the PR. |
| D2 Spec export | `springdoc-openapi-gradle-plugin` (`generateOpenApiDocs`) boots api-central in CI with Mongo and Redis as Semaphore services. |
| D3 Hosting | Own Deployment in the `astro` namespace of the cluster, at `mcp.<domain>`. |
| D4 Image | Plain Dockerfile, `docker buildx` push to GHCR from a new Semaphore block. |
| D5 Plugin and skills | Separate public repo `bot-astro/astro-mcp` (plugin marketplace + skills), synced by CI from `services/mcp-server/plugin`. |
| D6 Publishing | Official connector directories (Anthropic, OpenAI), official MCP Registry, "Add to ..." install links on the website. |
| D7 Testing | No staging. Production deploy first without announcing it, manual test with real clients, then publish. |

## Architecture

```
api-central (Kotlin) --springdoc--> openapi.json --forge--> services/mcp-server/src/tools (committed)
                                                                    |
                         mcp-server (Node, stateless, 2 replicas) <-+
                              |  introspection + tool calls, in cluster (http://api-central.astro.svc)
                              v
                         api-central -- Mongo / Redis / Discord

Claude, ChatGPT, Cursor, VS Code, Claude Code --HTTPS + OAuth (api-central is the AS)--> https://mcp.<domain>/mcp
```

- api-central is the only source of truth: endpoints, OAuth, scopes, permissions.
- The MCP server has no state and no secrets of its own, it can be scaled and rolled back freely.
- Deploy order: api-central before the MCP server. The allow-list of exposed tools is explicit, so a newer api-central never breaks an older MCP server.

## Phase 0: code needed before deploying

api-central:

- [ ] Rate limit by user for Bearer requests. `IpRateLimitFilter` uses `remoteAddr`, so every MCP user would share the MCP pod's IP and one bucket. Enable `UserIdRateLimitFilter` for authenticated requests (it is commented out in `SecurityConfig`) and keep the IP limit for anonymous ones.
- [ ] Grant management: `GET /v2/auth/grants` (connected apps of the user) and `DELETE /v2/auth/grants/{id}`, plus RFC 7009 `POST /oauth/revoke`. Directories expect users to be able to disconnect an app.
- [ ] Revoke the tokens of a reused authorization code.
- [ ] Integration test of the OAuth flow (authorize, consent, token, refresh, reuse, introspect). api-central has no test setup yet: add `@SpringBootTest` with Testcontainers for Mongo and Redis, Discord mocked.

MCP server:

- [x] `GET /healthz` (liveness) and readiness that checks api-central's metadata was loaded.
- [x] Retry loading the authorization server metadata at startup instead of crashing when api-central is not up yet.
- [x] Graceful shutdown on `SIGTERM` (stop accepting, finish in-flight requests).
- [x] Forward `X-Request-Id` to api-central and log it.
- [x] Sentry (`@sentry/node`) and OTLP metrics: tool calls by tool and status, latency, introspection failures.
- [x] Short cache (30 s) of introspection results by token hash, to halve the calls to api-central.
- [x] `Dockerfile`: multi-stage, `node:24-alpine`, `npm ci --omit=dev`, `tsc`, runs as non-root, copies `plugin/skills`.
- [x] `plugin/.mcp.json` with the production URL.
- [x] `server.json` for the MCP Registry, remote only (no `packages`):
  ```json
  {
    "$schema": "https://static.modelcontextprotocol.io/schemas/2025-12-11/server.schema.json",
    "name": "space.astro-bot/astro",
    "title": "Astro",
    "description": "Configure the Astro Discord bot: temporary voice channels, voice roles, interfaces, templates",
    "repository": { "url": "https://github.com/bot-astro/astro-mcp", "source": "github" },
    "version": "1.0.0",
    "remotes": [{ "type": "streamable-http", "url": "https://mcp.<domain>/mcp" }]
  }
  ```
  `description` is at most 100 characters. OAuth is not declared in `server.json` (the registry has no field for it), clients discover it from the protected resource metadata. `name` follows the namespace (see Phase 5), `space.astro-bot` is the reverse DNS of `astro-bot.space` as the docs' pattern implies, check it with `mcp-publisher validate`.

Website (`astro-bot.space` repo):

- [ ] Privacy policy and terms sections about AI clients: what data the connector reads and changes, retention of tokens (1 h access, 30 days refresh), how to disconnect.
- [ ] "Connected apps" page in the dashboard on top of the grant endpoints.

## Phase 1: generation pipeline

- [ ] api-central: add `springdoc-openapi-gradle-plugin`, `generateOpenApiDocs` writes `services/mcp-server/openapi.json`. Spring profile `openapi` that disables Discord, Chargebee and BigQuery startup calls.
- [ ] `justfile`: `mcp-generate` = `generateOpenApiDocs` + `npm run generate` + format.
- [ ] Semaphore drift check, on every branch when `services/api-central/`, `shared/` or `services/mcp-server/` change: start Mongo and Redis with `sem-service`, run `just mcp-generate`, `git diff --exit-code services/mcp-server`. A failure means "run `just mcp-generate` and commit".
- [ ] `npm test` already fails if an allow-listed tool disappears from the generated set (`exposedTools` throws).

## Phase 2: infrastructure (`infra` repo)

- [ ] `mcp-server` Deployment in `astro`: 2 replicas, `cpu: 50m`, `memory: 192Mi` request and limit, `NODE_OPTIONS=--max-old-space-size=128`, probes on the management port, `topologySpreadConstraints` across the two cpx52 nodes. The cluster has the room (about 8 GiB of free memory requests per worker), check the real memory after launch.
- [ ] Service, Ingress at `mcp.<domain>` with cert-manager, no rate limiting at the ingress (api-central does it per user).
- [ ] Infisical (`prod`), synced by the operator:
  - mcp-server: `MCP_PORT`, `MCP_PUBLIC_URL=https://mcp.<domain>/mcp`, `API_CENTRAL_URL=http://api-central.astro.svc:<port>`, `API_CENTRAL_PUBLIC_URL`, `SENTRY_DSN`
  - api-central: `CA__PUBLIC_URL`, `CA__MCP_RESOURCE_URL=https://mcp.<domain>/mcp`
- [ ] Cloudflare DNS record for `mcp.<domain>`.
- [ ] ArgoCD Application for the new workload.
- [ ] Optional: NetworkPolicy so only the ingress and mcp-server reach api-central's API port.

## Phase 3: CI/CD (Semaphore)

- [x] Block "Build, test and push mcp-server", `change_in(['/services/mcp-server/', '/.semaphore/'])` on `master`: `npm ci`, `tsc --noEmit`, `npm test`, `docker buildx build --push ghcr.io/bot-astro/mcp-server:<sha>`.
- [x] `.semaphore/deploy/mcp-server` promotion that bumps the tag in `infra`, manual like the other production promotions.
- [x] Promotion "Publish MCP" (manual, after the deploy):
  - publish `server.json` to the MCP Registry: `mcp-publisher login dns --domain astro-bot.space --private-key $MCP_REGISTRY_KEY` then `mcp-publisher publish`. The Ed25519 private key is a Semaphore secret (from Infisical). A version can be published only once and never changed, so the step is skipped when the version in `server.json` is already in the registry, and CI fails if `server.json` and `package.json` versions differ
  - sync `services/mcp-server/plugin` to `bot-astro/astro-mcp` and tag it with the same version

## Phase 4: launch without announcing, manual test

The production URL works but is not listed anywhere yet.

- [ ] Deploy api-central (OAuth changes) then the MCP server.
- [ ] Add it as a custom connector in Claude, ChatGPT (developer mode), Claude Code, Cursor and VS Code. For each: Discord login, consent with write and with read only, tools list, a read, a write with `dryRun`, a real write, a delete with confirmation.
- [ ] Refresh after an hour, reuse of a refresh token revokes the grant, dashboard logout keeps the MCP connection, revoking from "Connected apps" cuts it.
- [ ] Run the scenarios the skills cover ("create a generator in category X", "why are channels not created") and fix tool descriptions and skills where the agent fails. Each fix is a normal PR (descriptions live in the api-central annotations, regenerate).
- [ ] Check memory and CPU of the pods, adjust the requests.

## Phase 5: publish

- [ ] **MCP Registry** (in preview, check the docs again before publishing): domain namespace `space.astro-bot/*` proved with DNS. Generate an Ed25519 key pair, add the `v=MCPv1; k=ed25519; p=<public key>` TXT record at the apex of `astro-bot.space` in Cloudflare (not under a subdomain), store the private key in Infisical. Then run the "Publish MCP" promotion. Why not `io.github.bot-astro`: GitHub OIDC login only works from GitHub Actions (we use Semaphore), and the interactive `login github` cannot publish to an org namespace yet. A domain namespace also shows users the server is really ours, the registry does not check who owns the remote URL.
- [ ] **Anthropic Connectors Directory** and **OpenAI app directory**: check the current requirements before submitting, expect at least OAuth, tool annotations (read only, destructive), a privacy policy, a support contact, a description with examples, and a test account. Prepare a Discord test account that manages a test server with Astro and premium, for the reviewers.
- [ ] **Website** (`astro-bot.space`): an "AI assistants" page with the URL to paste, install links that each client supports (Cursor deeplink, VS Code `vscode:mcp/install` link, the Claude and ChatGPT directory pages once listed), the Claude Code command (`/plugin marketplace add bot-astro/astro-mcp`), and the skills for other agents. Check each link format in the client's docs when building the page.
- [ ] Announce in the support server and the changelog.

## Phase 6: operate

- Grafana dashboard: tool calls by tool and status, latency, error codes (`PREMIUM_REQUIRED`, 4xx, 5xx), active grants.
- Sentry alerts on 5xx of the MCP server and on introspection failures.
- Optional: tool call events in BigQuery to see which tools agents use.
- Rollback: revert the tag in `infra`, like the other services.

## Versioning

- MCP server version = `MAJOR.MINOR.PATCH` in `package.json`, used by `McpServer`, `server.json` and the plugin tag. Registry versions are immutable: a fix to the listing alone (description, title) needs a new version too.
- A removed or renamed tool, or a tool argument that becomes required, is a breaking change for agents of users. Deprecate it in the tool description for one release, then bump MAJOR.
- New tools or optional arguments bump MINOR, description fixes PATCH.

## Order of work

1. Phase 0 api-central (rate limit, grants, tests), Phase 0 MCP server, Phase 1, in parallel.
2. Phase 2 and 3.
3. Phase 4, fixes, Phase 4 again until the scenarios pass.
4. Phase 5, then the website page last so it never links to something not yet listed.
