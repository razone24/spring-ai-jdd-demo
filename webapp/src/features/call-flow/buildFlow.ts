import type { QueryMeta, ToolCall } from '../../domain/models';

export type FlowNode =
  | { kind: 'question' }
  | { kind: 'llm'; round: number; answers: boolean }
  | { kind: 'tool'; call: ToolCall; index: number }
  | { kind: 'answer'; outcome: 'answer' | 'cached' | 'refused' };

/** One column of the flow. A column with several nodes holds the calls the model asked for together. */
export type FlowStage = { nodes: FlowNode[]; parallel: boolean };

function single(node: FlowNode): FlowStage {
  return { nodes: [node], parallel: false };
}

function outcomeOf(meta: QueryMeta): 'answer' | 'cached' | 'refused' {
  if (meta.refused) {
    return 'refused';
  }
  return meta.cacheHit ? 'cached' : 'answer';
}

/**
 * Lays a turn's trace out left to right: the harness steps before the tool loop (round 0), then each
 * model round followed by the tools it requested, then the harness steps after the loop.
 */
export function buildFlow(calls: ToolCall[], meta: QueryMeta): FlowStage[] {
  const indexed = calls.map((call, index) => ({ call, index }));
  const toolStage = ({ call, index }: { call: ToolCall; index: number }) => single({ kind: 'tool', call, index });

  const stages: FlowStage[] = [single({ kind: 'question' })];
  stages.push(...indexed.filter(({ call }) => call.round === 0).map(toolStage));

  for (let round = 1; round <= meta.rounds; round++) {
    const requested = indexed.filter(({ call }) => call.round === round && call.origin !== 'harness');
    stages.push(single({ kind: 'llm', round, answers: requested.length === 0 }));
    if (requested.length > 0) {
      stages.push({
        nodes: requested.map(({ call, index }) => ({ kind: 'tool', call, index })),
        parallel: requested.length > 1,
      });
    }
  }

  stages.push(...indexed.filter(({ call }) => call.round > 0 && call.origin === 'harness').map(toolStage));
  stages.push(single({ kind: 'answer', outcome: outcomeOf(meta) }));
  return stages;
}
