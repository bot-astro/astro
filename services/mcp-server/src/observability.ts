import { AsyncLocalStorage } from 'node:async_hooks';
import { readFileSync } from 'node:fs';
import { metrics } from '@opentelemetry/api';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-http';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { MeterProvider, PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import * as Sentry from '@sentry/node';

export const version = (JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as { version: string }).version;

export const requestContext = new AsyncLocalStorage<{ requestId: string }>();

export function log(level: 'info' | 'warn' | 'error', message: string, fields: Record<string, unknown> = {}): void {
  const requestId = requestContext.getStore()?.requestId;
  const line = { time: new Date().toISOString(), level, message, ...(requestId && { requestId }), ...fields };
  process.stdout.write(`${JSON.stringify(line)}\n`);
}

export function reportError(error: unknown): void {
  log('error', error instanceof Error ? error.message : String(error), { stack: error instanceof Error ? error.stack : undefined });
  Sentry.captureException(error);
}

if (process.env.SENTRY_DSN) {
  Sentry.init({ dsn: process.env.SENTRY_DSN, release: version, environment: process.env.SENTRY_ENVIRONMENT ?? 'production' });
}

// same convention as api-central: OTEL_ENDPOINT is the OTLP receiver, the path is added here
const meterProvider = process.env.OTEL_ENDPOINT
  ? new MeterProvider({
      resource: resourceFromAttributes({ 'service.name': 'mcp-server', 'service.version': version }),
      readers: [
        new PeriodicExportingMetricReader({
          exporter: new OTLPMetricExporter({ url: `${process.env.OTEL_ENDPOINT.replace(/\/+$/, '')}/v1/metrics` }),
          exportIntervalMillis: 30_000,
        }),
      ],
    })
  : undefined;
if (meterProvider) {
  metrics.setGlobalMeterProvider(meterProvider);
}

const meter = metrics.getMeter('mcp-server', version);
export const toolCalls = meter.createCounter('mcp.tool.calls', { description: 'Tool calls by tool and status' });
export const toolDuration = meter.createHistogram('mcp.tool.duration', {
  description: 'Duration of the api-central call of a tool',
  unit: 's',
  advice: { explicitBucketBoundaries: [0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10] },
});
export const introspectionFailures = meter.createCounter('mcp.introspection.failures', { description: 'Token introspections that failed' });

export async function flushTelemetry(): Promise<void> {
  await Promise.allSettled([meterProvider?.shutdown(), Sentry.close(2000)]);
}
