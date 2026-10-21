import { useState } from 'react';
import type { QueryMeta, ToolCall } from '../../domain/models';
import { CallFlow } from '../call-flow/CallFlow';
import { ToolCallCard } from './ToolCallCard';
import styles from './ToolCallCard.module.css';

const TRACE_LABEL = 'Agent trace';
const VIEWS = ['list', 'flow'] as const;

type TraceView = (typeof VIEWS)[number];

export function ToolCallList({ calls, meta, latencyMs }: { calls: ToolCall[]; meta: QueryMeta; latencyMs: number }) {
  const [view, setView] = useState<TraceView>('list');

  if (calls.length === 0) {
    return null;
  }

  return (
    <div className={styles.trace}>
      <div className={styles.traceHeader}>
        <p className={styles.traceLabel}>
          {TRACE_LABEL} · {calls.length} {calls.length === 1 ? 'call' : 'calls'}
        </p>
        <div className={styles.viewSwitch} role="group" aria-label="Trace view">
          {VIEWS.map(option => (
            <button
              key={option}
              type="button"
              className={`${styles.viewOption} ${view === option ? styles.viewActive : ''}`}
              aria-pressed={view === option}
              onClick={() => setView(option)}
            >
              {option}
            </button>
          ))}
        </div>
      </div>
      {view === 'flow' ? (
        <CallFlow calls={calls} meta={meta} latencyMs={latencyMs} />
      ) : (
        calls.map((call, index) => <ToolCallCard key={index} step={index + 1} call={call} />)
      )}
    </div>
  );
}
