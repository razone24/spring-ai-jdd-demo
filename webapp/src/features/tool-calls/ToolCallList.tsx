import type { ToolCall } from '../../domain/models';
import { ToolCallCard } from './ToolCallCard';
import styles from './ToolCallCard.module.css';

const TRACE_LABEL = 'Agent trace';

export function ToolCallList({ calls }: { calls: ToolCall[] }) {
  if (calls.length === 0) {
    return null;
  }

  return (
    <div className={styles.trace}>
      <p className={styles.traceLabel}>
        {TRACE_LABEL} · {calls.length} {calls.length === 1 ? 'call' : 'calls'}
      </p>
      {calls.map((call, index) => (
        <ToolCallCard key={index} step={index + 1} call={call} />
      ))}
    </div>
  );
}
