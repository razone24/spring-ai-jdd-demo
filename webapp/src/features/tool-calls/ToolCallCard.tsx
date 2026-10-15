import { useState } from 'react';
import type { ToolCall } from '../../domain/models';
import { JsonView } from '../../ui/JsonView';
import { ToolArguments } from './ToolArguments';
import styles from './ToolCallCard.module.css';

const COLLAPSED_MARK = '▸';
const EXPANDED_MARK = '▾';
const ORIGIN_HINTS = {
  model: 'The model chose to call this tool',
  harness: 'A deterministic harness step, not a model decision',
} as const;
const ARGUMENTS_LABEL = 'Arguments';
const RESULT_LABEL = 'Result';
const ERROR_LABEL = 'Error';
const EMPTY_RESULT = '(empty)';

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function ResultPreview({ text }: { text: string }) {
  const parsed = parseJson(text);
  if (parsed !== undefined && typeof parsed === 'object' && parsed !== null) {
    return <JsonView value={parsed} />;
  }

  return <pre className={styles.result}>{text || EMPTY_RESULT}</pre>;
}

export function ToolCallCard({ step, call }: { step: number; call: ToolCall }) {
  const [expanded, setExpanded] = useState(false);
  const origin = call.origin ?? 'model';

  return (
    <section className={`${styles.card} ${call.error ? styles.failed : ''}`}>
      <button type="button" className={styles.header} onClick={() => setExpanded(!expanded)} aria-expanded={expanded}>
        <span className={styles.chevron}>{expanded ? EXPANDED_MARK : COLLAPSED_MARK}</span>
        <span className={styles.step}>{step}</span>
        <span className={`${styles.origin} ${styles[origin]}`} title={ORIGIN_HINTS[origin]}>
          {origin}
        </span>
        <span className={styles.name}>{call.name}</span>
        {call.server && <span className={styles.server}>{call.server}</span>}
        <span className={styles.duration}>{call.error ? '✕' : `${call.durationMs} ms`}</span>
      </button>
      {expanded && (
        <div className={styles.body}>
          <h4 className={styles.sectionLabel}>{ARGUMENTS_LABEL}</h4>
          <ToolArguments value={call.arguments} />
          {call.error ? (
            <>
              <h4 className={styles.sectionLabel}>{ERROR_LABEL}</h4>
              <pre className={`${styles.result} ${styles.errorText}`}>{call.error}</pre>
            </>
          ) : (
            <>
              <h4 className={styles.sectionLabel}>{RESULT_LABEL}</h4>
              <ResultPreview text={call.resultPreview ?? ''} />
            </>
          )}
        </div>
      )}
    </section>
  );
}
