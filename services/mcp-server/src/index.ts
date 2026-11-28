import { randomUUID } from 'node:crypto';
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

const port = Number(process.env.MCP_PORT ?? 9002);
const apiUrl = (process.env.API_CENTRAL_URL ?? 'http://localhost:9001').replace(/\/+$/, '');
// the OAuth issuer, api-central CA__PUBLIC_URL
const issuer = new URL(process.env.API_CENTRAL_PUBLIC_URL ?? apiUrl);
const resource = new URL(process.env.MCP_PUBLIC_URL ?? `http://localhost:${port}/mcp`);
const resourcePath = resource.pathname.replace(/\/+$/, '') || '/';
const metadataUrl = getOAuthProtectedResourceMetadataUrl(resource);

const oauthMetadata = (await (await fetch(`${apiUrl}/.well-known/oauth-authorization-server`)).json()) as OAuthMetadata;

const metadataOptions = {
  oauthMetadata,
  resourceServerUrl: resource,
  authorizationServerUrls: [issuer],
  scopesSupported: ['guilds:read', 'guilds:write'],
  resourceName: 'Astro',
  dangerouslyAllowInsecureIssuerUrl: issuer.protocol === 'http:',
};

const trimSlash = (value: string) => value.replace(/\/+$/, '');

// api-central owns the tokens (opaque, hashed in Redis), so the check is asking it with token introspection (RFC 7662)
const gate = requireBearerAuth({
  resourceMetadataUrl: metadataUrl,
  requiredScopes: ['guilds:read'],
  verifier: {
    async verifyAccessToken(token: string) {
      const response = await fetch(`${apiUrl}/oauth/introspect`, {
        method: 'POST',
        headers: { 'content-type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ token }),
      });
      const info = response.ok
        ? ((await response.json()) as { active: boolean; scope?: string; client_id?: string; aud?: string; exp?: number })
        : { active: false };
      // a token issued for another resource must not work here, even if api-central would accept it
      if (!info.active || trimSlash(info.aud ?? '') !== trimSlash(resource.href)) {
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
  'access-control-allow-headers': 'authorization, content-type, mcp-protocol-version, mcp-session-id, last-event-id',
  'access-control-expose-headers': 'www-authenticate, mcp-session-id',
};

const handler = createMcpHandler(
  (ctx) => {
    const server = new McpServer({ name: 'astro', version: '0.0.1' }, { instructions });
    registerSkills(server);
    const token = ctx.authInfo?.token;
    const canWrite = ctx.authInfo?.scopes.includes('guilds:write') ?? false;
    // api-central rejects the writes of a read only token, so the agent should not see the tools for them
    const available = exposedTools(tools).filter((tool) => canWrite || tool.annotations?.readOnlyHint === true);
    registerTools(server, available, {
      baseUrl: apiUrl,
      fetch: (input, init) => {
        const request = new Request(input, init);
        request.headers.set('authorization', `Bearer ${token}`);
        if (request.method === 'POST' || request.method === 'PATCH') {
          request.headers.set('idempotency-key', randomUUID());
        }
        return fetch(request);
      },
    });
    return server;
  },
  { onerror: (error) => console.error(error) },
);

const handleNode = toNodeHandler(handler, { onerror: (error) => console.error(error) });

const http = createServer(async (req: IncomingMessage, res: ServerResponse) => {
  const url = new URL(req.url ?? '/', resource);
  const request = new Request(url, { method: req.method, headers: req.headers as Record<string, string> });
  const metadata = oauthMetadataResponse(request, metadataOptions);
  const send = async (response: Response) => {
    res.writeHead(response.status, { ...Object.fromEntries(response.headers), ...corsHeaders });
    res.end(Buffer.from(await response.arrayBuffer()));
  };
  if (req.method === 'OPTIONS') {
    return send(new Response(null, { status: 204, headers: { 'access-control-max-age': '600' } }));
  }
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
});

http.listen(port, () => console.log(`Astro MCP server on ${resource.href}, authorization server ${issuer.origin}`));
