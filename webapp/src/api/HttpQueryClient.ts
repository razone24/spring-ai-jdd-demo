import type { QueryFailure } from '../domain/failure';
import { REFUSAL_REASONS } from '../domain/models';
import type { QueryEnvelope, QueryMeta, RefusalReason, ToolCall, ToolOrigin } from '../domain/models';
import { QueryError } from './QueryClient';
import type { QueryClient, QueryRequest } from './QueryClient';

const QUERY_PATH = '/api/query';
const CONTENT_TYPE = 'application/json';
const UNKNOWN_ERROR_MESSAGE = 'The backend returned an error without a message.';
const UNREACHABLE_MESSAGE = 'Could not reach the assistant. Is assistant-api running behind the proxy?';
const GATEWAY_STATUSES = [502, 503, 504];

type WireToolCall = {
  name?: string;
  server?: string;
  origin?: string;
  arguments?: unknown;
  result_preview?: string;
  duration_ms?: number;
  error?: string;
};

type WireMeta = {
  request_id?: string;
  model?: string;
  prompt_tokens?: number;
  completion_tokens?: number;
  cost_usd?: number;
  rounds?: number;
  latency_ms?: number;
  cache_hit?: boolean;
  grounding_retry?: boolean;
  refused?: boolean;
  refusal_reason?: string;
};

type WireEnvelope = {
  message?: string;
  conversation_id?: string;
  query_interpretation?: { tools?: WireToolCall[] };
  meta?: WireMeta;
};

function buildBody({ prompt, conversationId }: QueryRequest): string {
  return JSON.stringify(conversationId ? { prompt, conversation_id: conversationId } : { prompt });
}

function decodeOrigin(origin: string | undefined): ToolOrigin | null {
  return origin === 'model' || origin === 'harness' ? origin : null;
}

function decodeRefusal(reason: string | undefined): RefusalReason | null {
  return REFUSAL_REASONS.find(known => known === reason) ?? null;
}

function decodeToolCall(wire: WireToolCall): ToolCall {
  return {
    name: wire.name ?? 'unknown',
    server: wire.server ?? null,
    origin: decodeOrigin(wire.origin),
    arguments: wire.arguments ?? null,
    resultPreview: wire.result_preview ?? null,
    durationMs: wire.duration_ms ?? 0,
    error: wire.error ?? null,
  };
}

function decodeMeta(wire: WireMeta | undefined): QueryMeta {
  return {
    requestId: wire?.request_id ?? '',
    model: wire?.model ?? null,
    promptTokens: wire?.prompt_tokens ?? 0,
    completionTokens: wire?.completion_tokens ?? 0,
    costUsd: wire?.cost_usd ?? 0,
    rounds: wire?.rounds ?? 0,
    latencyMs: wire?.latency_ms ?? 0,
    cacheHit: wire?.cache_hit ?? false,
    groundingRetry: wire?.grounding_retry ?? false,
    refused: wire?.refused ?? false,
    refusalReason: decodeRefusal(wire?.refusal_reason),
  };
}

function decodeEnvelope(wire: WireEnvelope, fallbackConversationId: string | null): QueryEnvelope {
  return {
    message: wire.message ?? '',
    conversationId: wire.conversation_id ?? fallbackConversationId ?? '',
    toolCalls: (wire.query_interpretation?.tools ?? []).map(decodeToolCall),
    meta: decodeMeta(wire.meta),
  };
}

function describeError(status: number, message: string | undefined): QueryFailure {
  if (message) {
    return { kind: 'http', status, message };
  }

  return GATEWAY_STATUSES.includes(status)
    ? { kind: 'transport', message: UNREACHABLE_MESSAGE }
    : { kind: 'http', status, message: UNKNOWN_ERROR_MESSAGE };
}

async function readWire(response: Response): Promise<WireEnvelope> {
  try {
    return (await response.json()) as WireEnvelope;
  } catch {
    return {};
  }
}

export class HttpQueryClient implements QueryClient {
  async send(request: QueryRequest): Promise<QueryEnvelope> {
    const response = await this.post(request);
    const wire = await readWire(response);

    if (!response.ok) {
      throw new QueryError(describeError(response.status, wire.message));
    }

    return decodeEnvelope(wire, request.conversationId);
  }

  private async post(request: QueryRequest): Promise<Response> {
    try {
      return await fetch(QUERY_PATH, {
        method: 'POST',
        headers: { 'Content-Type': CONTENT_TYPE },
        body: buildBody(request),
        signal: request.signal,
      });
    } catch (cause) {
      if (request.signal.aborted) {
        throw cause;
      }
      throw new QueryError({ kind: 'transport', message: UNREACHABLE_MESSAGE });
    }
  }
}
