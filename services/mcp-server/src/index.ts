import { createHash, randomUUID } from 'node:crypto';
import { createServer, type IncomingMessage, type ServerResponse } from 'node:http';
import { toNodeHandler } from '@modelcontextprotocol/node';
import {
  createMcpHandler,
  getOAuthProtectedResourceMetadataUrl,
  McpServer,
  OAuthError,
  OAuthErrorCode,
  type OAuthMetadata,
  oauthMetadataResponse,
  requireBearerAuth,
} from '@modelcontextprotocol/server';
import { registerTools, tools } from './tools/index.js';
import { exposedTools } from './exposed-tools.js';
import { instructions, registerSkills } from './skills.js';
import { flushTelemetry, introspectionFailures, log, reportError, requestContext, toolCalls, toolDuration, version } from './observability.js';

const port = Number(process.env.MCP_PORT ?? 9002);
const managementPort = Number(process.env.MCP_MANAGEMENT_PORT ?? 9102);
const apiUrl = (process.env.API_CENTRAL_URL ?? 'http://localhost:9001').replace(/\/+$/, '');
// the OAuth issuer, api-central CA__PUBLIC_URL
const issuer = new URL(process.env.API_CENTRAL_PUBLIC_URL ?? apiUrl);
const resource = new URL(process.env.MCP_PUBLIC_URL ?? `http://localhost:${port}/mcp`);
const resourcePath = resource.pathname.replace(/\/+$/, '') || '/';
const metadataUrl = getOAuthProtectedResourceMetadataUrl(resource);

let metadataOptions: Parameters<typeof oauthMetadataResponse>[1] | undefined;
let shuttingDown = false;

// api-central can start after this server, so keep trying instead of crashing: readiness stays down meanwhile
async function loadMetadata(): Promise<void> {
  for (let attempt = 1; !shuttingDown; attempt++) {
    try {
      const response = await fetch(`${apiUrl}/.well-known/oauth-authorization-server`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      metadataOptions = { ...metadataBase, oauthMetadata: (await response.json()) as OAuthMetadata };
      log('info', 'Authorization server metadata loaded', { issuer: issuer.origin });
      return;
    } catch (error) {
      const delay = Math.min(1000 * 2 ** (attempt - 1), 30_000);
      log('warn', 'Loading the authorization server metadata failed, retrying', { attempt, delayMs: delay, error: String(error) });
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }
}

const metadataBase = {
  resourceServerUrl: resource,
  authorizationServerUrls: [issuer],
  scopesSupported: ['guilds:read', 'guilds:write'],
  resourceName: 'Astro',
  dangerouslyAllowInsecureIssuerUrl: issuer.protocol === 'http:',
};

const trimSlash = (value: string) => value.replace(/\/+$/, '');

interface Introspection {
  active: boolean;
  scope?: string;
  client_id?: string;
  aud?: string;
  exp?: number;
}

// every MCP request is checked, a short cache halves the calls to api-central; a revoked token works at most this long
const introspectionTtlMs = 30_000;
const introspectionCacheSize = 10_000;
const introspectionCache = new Map<string, { info: Introspection; until: number }>();

async function introspect(token: string): Promise<Introspection> {
  const key = createHash('sha256').update(token).digest('hex');
  const cached = introspectionCache.get(key);
  if (cached && cached.until > Date.now()) {
    return cached.info;
  }
  introspectionCache.delete(key);
  let response: Response;
  try {
    response = await fetch(`${apiUrl}/oauth/introspect`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded', 'x-request-id': requestContext.getStore()?.requestId ?? randomUUID() },
      body: new URLSearchParams({ token }),
    });
  } catch (error) {
    introspectionFailures.add(1, { reason: 'unreachable' });
    log('error', 'Token introspection failed', { error: String(error) });
    return { active: false };
  }
  if (!response.ok) {
    introspectionFailures.add(1, { reason: `http_${response.status}` });
    log('error', 'Token introspection failed', { status: response.status });
    return { active: false };
  }
  const info = (await response.json()) as Introspection;
  if (info.active) {
    if (introspectionCache.size >= introspectionCacheSize) {
      // a Map iterates in insertion order, so this drops the oldest entry
      introspectionCache.delete(introspectionCache.keys().next().value!);
    }
    const expiresAt = info.exp ? info.exp * 1000 : Infinity;
    introspectionCache.set(key, { info, until: Math.min(Date.now() + introspectionTtlMs, expiresAt) });
  }
  return info;
}

// api-central owns the tokens (opaque, hashed in Redis), so the check is asking it with token introspection (RFC 7662)
const gate = requireBearerAuth({
  resourceMetadataUrl: metadataUrl,
  requiredScopes: ['guilds:read'],
  verifier: {
    async verifyAccessToken(token: string) {
      const info = await introspect(token);
      // a token issued for another resource must not work here, even if api-central would accept it
      if (!info.active || trimSlash(info.aud ?? '') !== trimSlash(resource.href)) {
        if (info.active) {
          introspectionFailures.add(1, { reason: 'audience' });
        }
        throw new OAuthError(OAuthErrorCode.InvalidToken, 'Invalid or expired token');
      }
      return {
        token,
        clientId: info.client_id ?? 'mcp',
        scopes: (info.scope ?? '').split(' ').filter(Boolean),
        expiresAt: info.exp,
        resource,
      };
    },
  },
});

// browser based clients call the MCP endpoint and its metadata from any origin, the token is in a header and not a cookie
const corsHeaders = {
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET, POST, DELETE, OPTIONS',
  'access-control-allow-headers': 'authorization, content-type, mcp-protocol-version, mcp-session-id, last-event-id, x-request-id',
  'access-control-expose-headers': 'www-authenticate, mcp-session-id, x-request-id',
};

const handler = createMcpHandler(
  (ctx) => {
    const server = new McpServer({ name: 'astro', version }, { instructions });
    registerSkills(server);
    const token = ctx.authInfo?.token;
    const canWrite = ctx.authInfo?.scopes.includes('guilds:write') ?? false;
    // api-central rejects the writes of a read only token, so the agent should not see the tools for them
    const available = exposedTools(tools).filter((tool) => canWrite || tool.annotations?.readOnlyHint === true);
    // one registration per tool so the fetch knows which tool it measures
    for (const tool of available) {
      registerTools(server, [tool], {
        baseUrl: apiUrl,
        fetch: async (input, init) => {
          const request = new Request(input, init);
          request.headers.set('authorization', `Bearer ${token}`);
          request.headers.set('x-request-id', requestContext.getStore()?.requestId ?? randomUUID());
          if (request.method === 'POST' || request.method === 'PATCH') {
            request.headers.set('idempotency-key', randomUUID());
          }
          const start = performance.now();
          try {
            const response = await fetch(request);
            const status = response.ok ? 'ok' : `${Math.floor(response.status / 100)}xx`;
            const errorCode = response.ok
              ? undefined
              : ((await response.clone().json().catch(() => undefined)) as { code?: string } | undefined)?.code;
            toolCalls.add(1, { tool: tool.name, status, ...(errorCode && { error_code: errorCode }) });
            log(response.ok ? 'info' : 'warn', 'Tool call', { tool: tool.name, status: response.status, ...(errorCode && { errorCode }) });
            return response;
          } catch (error) {
            toolCalls.add(1, { tool: tool.name, status: 'error' });
            reportError(error);
            throw error;
          } finally {
            toolDuration.record((performance.now() - start) / 1000, { tool: tool.name });
          }
        },
      });
    }
    return server;
  },
  { onerror: reportError },
);

const handleNode = toNodeHandler(handler, { onerror: reportError });

const http = createServer((req: IncomingMessage, res: ServerResponse) => {
  const header = req.headers['x-request-id'];
  // a client chosen id ends up in logs, so only a sane one is kept
  const requestId = typeof header === 'string' && /^[\w.:-]{1,128}$/.test(header) ? header : randomUUID();
  res.setHeader('x-request-id', requestId);
  requestContext.run({ requestId }, () => handle(req, res).catch((error) => {
    reportError(error);
    if (!res.headersSent) {
      res.writeHead(500).end();
    }
  }));
});

async function handle(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const url = new URL(req.url ?? '/', resource);
  const request = new Request(url, { method: req.method, headers: req.headers as Record<string, string> });
  const send = async (response: Response) => {
    res.writeHead(response.status, { ...Object.fromEntries(response.headers), ...corsHeaders });
    res.end(Buffer.from(await response.arrayBuffer()));
  };
  if (req.method === 'OPTIONS') {
    return send(new Response(null, { status: 204, headers: { 'access-control-max-age': '600' } }));
  }
  if (!metadataOptions) {
    return send(new Response('Starting', { status: 503, headers: { 'retry-after': '5' } }));
  }
  const metadata = oauthMetadataResponse(request, metadataOptions);
  if (metadata) {
    return send(metadata);
  }
  if (url.pathname !== resourcePath) {
    return send(new Response('Not found', { status: 404 }));
  }
  const auth = await gate(request);
  if (auth instanceof Response) {
    return send(auth);
  }
  (req as typeof req & { auth?: unknown }).auth = auth;
  return handleNode(req as never, res as never);
}

const management = createServer((req, res) => {
  if (req.url === '/healthz') {
    res.writeHead(200).end('ok');
  } else if (req.url === '/readyz') {
    const ready = metadataOptions !== undefined && !shuttingDown;
    res.writeHead(ready ? 200 : 503).end(ready ? 'ready' : 'not ready');
  } else {
    res.writeHead(404).end();
  }
});

http.listen(port, () => log('info', 'Astro MCP server started', { version, resource: resource.href, issuer: issuer.origin, managementPort }));
management.listen(managementPort);
void loadMetadata();

// k8s sends SIGTERM and kills after 30 s: stop being ready, let in-flight requests finish, then exit before that
const drainDelayMs = Number(process.env.MCP_SHUTDOWN_DRAIN_MS ?? 5000);
const shutdownTimeoutMs = 25_000;
async function shutdown(signal: string) {
  if (shuttingDown) {
    return;
  }
  shuttingDown = true;
  log('info', 'Shutting down', { signal });
  setTimeout(() => {
    log('warn', 'In-flight requests did not finish in time, exiting');
    process.exit(1);
  }, shutdownTimeoutMs).unref();
  // the pod stays in the Service endpoints for a moment after SIGTERM, keep accepting until the ingress notices
  await new Promise((resolve) => setTimeout(resolve, drainDelayMs));
  await new Promise((resolve) => {
    http.close(resolve);
    http.closeIdleConnections();
  });
  management.close();
  await flushTelemetry();
  process.exit(0);
}
process.on('SIGTERM', () => void shutdown('SIGTERM'));
process.on('SIGINT', () => void shutdown('SIGINT'));
