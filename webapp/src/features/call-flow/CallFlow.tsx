import { useState } from 'react';
import type { CSSProperties } from 'react';
import type { QueryMeta, ToolCall } from '../../domain/models';
import { ToolArguments } from '../tool-calls/ToolArguments';
import { ResultPreview } from '../tool-calls/ToolCallCard';
import { buildFlow } from './buildFlow';
import type { FlowNode } from './buildFlow';
import styles from './CallFlow.module.css';

const PARALLEL_HINT = 'The model asked for these tools together, in one response';
const OUTCOMES = {
  answer: { icon: '✓', title: 'Answer' },
  cached: { icon: '⚡', title: 'Cached answer' },
  refused: { icon: '⊘', title: 'Refused' },
} as const;
const SELECT_HINT = 'Click a tool to see its arguments and result.';

type NodeView = { icon: string; title: string; subtitle: string; tone: string | undefined; failed?: boolean };

function viewOf(node: FlowNode, latencyMs: number): NodeView {
  switch (node.kind) {
    case 'question':
      return { icon: '?', title: 'Question', subtitle: 'from the user', tone: styles.question };
    case 'llm':
      return {
        icon: '✦',
        title: `LLM · round ${node.round}`,
        subtitle: node.answers ? 'writes the answer' : 'picks tools',
        tone: styles.llm,
      };
    case 'tool':
      return {
        icon: node.call.origin === 'harness' ? '⛨' : '⚙',
        title: node.call.name,
        subtitle: `${node.call.server ?? node.call.origin ?? ''} · ${node.call.error ? 'failed' : `${node.call.durationMs} ms`}`,
        tone: node.call.origin === 'harness' ? styles.harness : styles.model,
        failed: node.call.error !== null,
      };
    case 'answer':
      return { ...OUTCOMES[node.outcome], subtitle: `${(latencyMs / 1000).toFixed(2)} s total`, tone: styles[node.outcome] };
  }
}

function CallDetails({ call }: { call: ToolCall }) {
  return (
    <div className={styles.details}>
      <h4 className={styles.detailsTitle}>{call.name}</h4>
      <h5 className={styles.sectionLabel}>Arguments</h5>
      <ToolArguments value={call.arguments} />
      <h5 className={styles.sectionLabel}>{call.error ? 'Error' : 'Result'}</h5>
      {call.error ? <pre className={styles.error}>{call.error}</pre> : <ResultPreview text={call.resultPreview ?? ''} />}
    </div>
  );
}

export function CallFlow({ calls, meta, latencyMs }: { calls: ToolCall[]; meta: QueryMeta; latencyMs: number }) {
  const [selected, setSelected] = useState<number | null>(null);
  const stages = buildFlow(calls, meta);
  const selectedCall = selected === null ? null : calls[selected];

  return (
    <div className={styles.wrapper}>
      <div className={styles.canvas}>
        <div className={styles.flow}>
          {stages.map((stage, stageIndex) => (
            <div key={stageIndex} className={styles.step} style={{ '--i': stageIndex } as CSSProperties}>
              {stageIndex > 0 && <span className={styles.edge} aria-hidden="true" />}
              <div className={`${styles.stage} ${stage.parallel ? styles.parallel : ''}`}>
                {stage.parallel && (
                  <span className={styles.parallelLabel} title={PARALLEL_HINT}>
                    parallel · {stage.nodes.length}
                  </span>
                )}
                {stage.nodes.map((node, nodeIndex) => {
                  const view = viewOf(node, latencyMs);
                  const toolIndex = node.kind === 'tool' ? node.index : null;
                  const className = [
                    styles.node,
                    view.tone,
                    view.failed ? styles.failed : '',
                    toolIndex !== null && toolIndex === selected ? styles.selected : '',
                  ].join(' ');
                  const content = (
                    <>
                      <span className={styles.icon}>{view.icon}</span>
                      <span className={styles.text} title={`${view.title}\n${view.subtitle}`}>
                        <span className={styles.title}>{view.title}</span>
                        <span className={styles.subtitle}>{view.subtitle}</span>
                      </span>
                    </>
                  );
                  return toolIndex === null ? (
                    <div key={nodeIndex} className={className}>
                      {content}
                    </div>
                  ) : (
                    <button
                      key={nodeIndex}
                      type="button"
                      className={className}
                      aria-pressed={toolIndex === selected}
                      onClick={() => setSelected(toolIndex === selected ? null : toolIndex)}
                    >
                      {content}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
      {selectedCall ? <CallDetails call={selectedCall} /> : <p className={styles.hint}>{SELECT_HINT}</p>}
    </div>
  );
}
