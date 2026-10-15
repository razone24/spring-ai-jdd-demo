import type { QueryEnvelope } from '../../domain/models';
import { ToolCallList } from '../tool-calls/ToolCallList';
import { MessageBody } from './MessageBody';
import { RefusalNotice } from './RefusalNotice';
import { TurnMeta } from './TurnMeta';

export function AssistantTurn({ envelope, latencyMs }: { envelope: QueryEnvelope; latencyMs: number }) {
  const { meta } = envelope;

  return (
    <div>
      <TurnMeta meta={meta} latencyMs={latencyMs} />
      <ToolCallList calls={envelope.toolCalls} />
      {meta.refused ? (
        <RefusalNotice reason={meta.refusalReason} message={envelope.message} />
      ) : (
        <MessageBody text={envelope.message} />
      )}
    </div>
  );
}
