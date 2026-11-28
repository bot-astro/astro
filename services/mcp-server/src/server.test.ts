import assert from 'node:assert/strict';
import { type ChildProcess, spawn } from 'node:child_process';
import { createServer, type IncomingMessage } from 'node:http';
import { after, before, test } from 'node:test';

const received: IncomingMessage[] = [];
const stub = createServer((req, res) => {
  received.push(req);
  res.setHeader('content-type', 'application/json');
  if (req.url === '/.well-known/oauth-authorization-server') {
    res.end(JSON.stringify({ issuer: 'http://localhost:1', authorization_endpoint: 'http://localhost:1/a', token_endpoint: 'http://localhost:1/t', response_types_supported: ['code'] }));
  } else if (req.url === '/oauth/introspect') {
    let body = '';
    req.on('data', (chunk) => (body += chunk));
    req.on('end', () => {
      const token = new URLSearchParams(body).get('token');
      const aud = token === 'otheraud' ? 'http://localhost:1/mcp' : mcp;
      const scope = token === 'readonly' ? 'guilds:read' : token === 'noscope' ? '' : 'guilds:read guilds:write';
      res.end(JSON.stringify(token && token !== 'bad' ? { active: true, scope, client_id: 'c', aud, exp: Math.floor(Date.now() / 1000) + 3600 } : { active: false }));
    });
  } else if (!req.headers.authorization?.startsWith('Bearer ')) {
    res.statusCode = 401;
    res.end('{"code":"UNAUTHENTICATED"}');
  } else {
    res.end('{"ok":true}');
  }
});

let server: ChildProcess;
const mcp = 'http://localhost:19002/mcp';

before(async () => {
  await new Promise<void>((resolve) => stub.listen(19001, resolve));
  server = spawn('npx', ['tsx', 'src/index.ts'], {
    env: { ...process.env, MCP_PORT: '19002', MCP_MANAGEMENT_PORT: '19102', API_CENTRAL_URL: 'http://localhost:19001' },
    stdio: 'pipe',
  });
  await waitFor(async () => (await fetch('http://localhost:19102/readyz')).status === 200);
});

async function waitFor(check: () => Promise<boolean>) {
  for (let i = 0; i < 100; i++) {
    if (await check().catch(() => false)) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('timed out');
}

after(() => {
  server.kill();
  stub.close();
});

const rpc = (method: string, params: unknown, token?: string, headers: Record<string, string> = {}) =>
  fetch(mcp, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      accept: 'application/json, text/event-stream',
      ...(token && { authorization: `Bearer ${token}` }),
      ...headers,
    },
    body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params }),
  });

test('no token is a 401 pointing at the resource metadata', async () => {
  const response = await rpc('tools/list', {});
  assert.equal(response.status, 401);
  assert.match(response.headers.get('www-authenticate') ?? '', /resource_metadata=/);
});

test('a token api-central rejects is a 401', async () => {
  assert.equal((await rpc('tools/list', {}, 'bad')).status, 401);
});

const listTools = async (token: string) => {
  const body = await (await rpc('tools/list', {}, token)).text();
  const result = JSON.parse(body.split('\ndata: ')[1]!.split('\n')[0]!).result as { tools: { name: string }[] };
  return { names: result.tools.map((tool) => tool.name), size: body.length };
};

test('a token for another resource is a 401', async () => {
  assert.equal((await rpc('tools/list', {}, 'otheraud')).status, 401);
});

test('a token without the read scope is a 403', async () => {
  const response = await rpc('tools/list', {}, 'noscope');
  assert.equal(response.status, 403);
  assert.match(response.headers.get('www-authenticate') ?? '', /insufficient_scope/);
});

test('a read only token lists only the read tools', async () => {
  const { names } = await listTools('readonly');
  assert.ok(names.includes('guilds_settings_get'));
  assert.ok(!names.includes('guilds_settings_update'));
  assert.ok(!names.includes('guilds_generators_delete'));
});

test('answers the CORS preflight and exposes the headers to browsers', async () => {
  const preflight = await fetch(mcp, { method: 'OPTIONS', headers: { origin: 'https://client.example', 'access-control-request-method': 'POST' } });
  assert.equal(preflight.status, 204);
  assert.equal(preflight.headers.get('access-control-allow-origin'), '*');
  assert.match(preflight.headers.get('access-control-allow-headers') ?? '', /authorization/);
  const unauthorized = await rpc('tools/list', {});
  assert.match(unauthorized.headers.get('access-control-expose-headers') ?? '', /www-authenticate/);
});

test('lists only the exposed tools', async () => {
  const { names, size } = await listTools('good');
  assert.equal(names.length, 22);
  assert.ok(names.includes('guilds_settings_get'));
  assert.ok(!names.some((name) => /^(billing|auth|guilds_premium|guilds_media)_|getSelf/.test(name)));
  console.log(`tools/list is ${size} bytes`);
});

test('a write call forwards the token and an idempotency key', async () => {
  received.length = 0;
  const response = await rpc('tools/call', { name: 'guilds_settings_update', arguments: { path: { guildId: '1' }, body: {} } }, 'good');
  assert.equal(response.status, 200);
  const call = received.find((req) => req.method === 'PATCH');
  assert.equal(call?.url, '/v2/guilds/1/settings');
  assert.equal(call?.headers.authorization, 'Bearer good');
  assert.ok(call?.headers['idempotency-key']);
});

const result = async (response: Response) => JSON.parse((await response.text()).split('\ndata: ')[1]!.split('\n')[0]!).result;

test('the skills are served as instructions, resources and prompts', async () => {
  const init = await result(await rpc('initialize', { protocolVersion: '2025-06-18', capabilities: {}, clientInfo: { name: 't', version: '0' } }, 'good'));
  assert.match(init.instructions, /skill:\/\/astro\/configure-astro\/SKILL\.md/);
  const resources = await result(await rpc('resources/list', {}, 'good'));
  assert.deepEqual(resources.resources.map((r: { name: string }) => r.name).sort(), ['configure-astro', 'diagnose-astro']);
  const read = await result(await rpc('resources/read', { uri: 'skill://astro/configure-astro/SKILL.md' }, 'good'));
  assert.match(read.contents[0].text, /guilds_settings_get/);
  const prompts = await result(await rpc('prompts/list', {}, 'good'));
  assert.equal(prompts.prompts.length, 2);
});

test('caches the introspection of an active token', async () => {
  received.length = 0;
  await rpc('tools/list', {}, 'cached');
  await rpc('tools/list', {}, 'cached');
  assert.equal(received.filter((req) => req.url === '/oauth/introspect').length, 1);
  received.length = 0;
  await rpc('tools/list', {}, 'bad');
  await rpc('tools/list', {}, 'bad');
  assert.equal(received.filter((req) => req.url === '/oauth/introspect').length, 2);
});

test('forwards the request id to api-central and returns it', async () => {
  received.length = 0;
  const headers = { 'x-request-id': 'req-123' };
  const response = await rpc('tools/call', { name: 'guilds_settings_get', arguments: { path: { guildId: '1' } } }, 'requestid', headers);
  assert.equal(response.headers.get('x-request-id'), 'req-123');
  assert.equal(received.find((req) => req.url === '/oauth/introspect')?.headers['x-request-id'], 'req-123');
  assert.equal(received.find((req) => req.url === '/v2/guilds/1/settings')?.headers['x-request-id'], 'req-123');
  const generated = await rpc('tools/list', {}, 'good');
  assert.match(generated.headers.get('x-request-id') ?? '', /^[0-9a-f-]{36}$/);
});

test('is live but not ready until api-central answers, and exits on SIGTERM', async () => {
  const starting = spawn('npx', ['tsx', 'src/index.ts'], {
    env: { ...process.env, MCP_PORT: '19003', MCP_MANAGEMENT_PORT: '19103', MCP_SHUTDOWN_DRAIN_MS: '0', API_CENTRAL_URL: 'http://localhost:19009' },
    stdio: 'pipe',
  });
  await waitFor(async () => (await fetch('http://localhost:19103/healthz')).status === 200);
  assert.equal((await fetch('http://localhost:19103/readyz')).status, 503);
  assert.equal((await fetch('http://localhost:19003/mcp', { method: 'POST' })).status, 503);
  const exited = new Promise<number | null>((resolve) => starting.on('exit', resolve));
  // npx forwards the signal to the node process
  starting.kill('SIGTERM');
  assert.equal(await exited, 0);
});
