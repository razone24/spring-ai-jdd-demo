export const REFUSAL_REASONS = ['OUT_OF_SCOPE', 'UNINTELLIGIBLE'] as const;

export type RefusalReason = (typeof REFUSAL_REASONS)[number];

/** Who decided to call the tool: the model in its tool loop, or the deterministic harness around it. */
export type ToolOrigin = 'model' | 'harness';

export type ToolCall = {
  name: string;
  server: string | null;
  origin: ToolOrigin | null;
  arguments: unknown;
  resultPreview: string | null;
  durationMs: number;
  error: string | null;
};

export type QueryMeta = {
  requestId: string;
  model: string | null;
  promptTokens: number;
  completionTokens: number;
  costUsd: number;
  rounds: number;
  latencyMs: number;
  cacheHit: boolean;
  groundingRetry: boolean;
  refused: boolean;
  refusalReason: RefusalReason | null;
};

export type QueryEnvelope = {
  message: string;
  conversationId: string;
  toolCalls: ToolCall[];
  meta: QueryMeta;
};
