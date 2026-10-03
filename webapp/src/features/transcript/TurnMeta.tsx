import type { QueryMeta } from '../../domain/models';
import { CopyButton } from '../../ui/CopyButton';
import styles from './TurnMeta.module.css';

const ASSISTANT_LABEL = 'Assistant';
const CACHE_HIT_LABEL = '⚡ semantic cache hit';
const CACHE_HIT_HINT = 'Served from the chat-history MCP server — the model was not called';
const GROUNDING_LABEL = '↻ grounding guard';
const GROUNDING_HINT = 'The first draft used no tool, so the harness discarded it and made the model look the facts up';
const ID_PREVIEW_LENGTH = 8;
const MS_PER_SECOND = 1000;
const COST_HINT = 'Cloud-equivalent cost: what these tokens would cost on gpt-4.1-mini';

function formatSeconds(latencyMs: number): string {
  return latencyMs < MS_PER_SECOND ? `${latencyMs} ms` : `${(latencyMs / MS_PER_SECOND).toFixed(2)} s`;
}

function formatCost(costUsd: number): string {
  return `$${costUsd.toFixed(5)}`;
}

export function TurnMeta({ meta, latencyMs }: { meta: QueryMeta; latencyMs: number }) {
  const tokens = meta.promptTokens + meta.completionTokens;
  const items = [
    formatSeconds(latencyMs),
    !meta.cacheHit && meta.model,
    !meta.cacheHit && `${tokens.toLocaleString()} tokens`,
    !meta.cacheHit && `${meta.rounds} ${meta.rounds === 1 ? 'round' : 'rounds'}`,
  ].filter(Boolean) as string[];

  return (
    <div className={styles.meta}>
      <span className={styles.label}>{ASSISTANT_LABEL}</span>
      {items.map(item => (
        <span key={item} className={styles.item}>
          {item}
        </span>
      ))}
      {!meta.cacheHit && (
        <span className={styles.item} title={COST_HINT}>
          {formatCost(meta.costUsd)}
        </span>
      )}
      {meta.cacheHit && (
        <span className={styles.cacheHit} title={CACHE_HIT_HINT}>
          {CACHE_HIT_LABEL}
        </span>
      )}
      {meta.groundingRetry && (
        <span className={styles.grounding} title={GROUNDING_HINT}>
          {GROUNDING_LABEL}
        </span>
      )}
      {meta.requestId && (
        <span className={`${styles.item} ${styles.request}`} title={meta.requestId}>
          req {meta.requestId.slice(0, ID_PREVIEW_LENGTH)}
          <CopyButton value={meta.requestId} label="Copy request id" />
        </span>
      )}
    </div>
  );
}
