import type { Turn } from '../../domain/conversation';
import { AssistantTurn } from './AssistantTurn';
import { CancelledTurn } from './CancelledTurn';
import { ErrorTurn } from './ErrorTurn';
import { PendingTurn } from './PendingTurn';
import { UserTurn } from './UserTurn';
import styles from './TurnView.module.css';

type TurnViewProps = {
  turn: Turn;
  onCancel: (turnKey: string) => void;
  onRetry: (prompt: string) => void;
};

function renderState({ turn, onCancel, onRetry }: TurnViewProps) {
  switch (turn.state.kind) {
    case 'pending':
      return <PendingTurn startedAt={turn.state.startedAt} onCancel={() => onCancel(turn.key)} />;
    case 'answered':
      return <AssistantTurn envelope={turn.state.envelope} latencyMs={turn.state.latencyMs} />;
    case 'failed':
      return <ErrorTurn failure={turn.state.failure} onRetry={() => onRetry(turn.prompt)} />;
    case 'cancelled':
      return <CancelledTurn onRetry={() => onRetry(turn.prompt)} />;
  }
}

export function TurnView(props: TurnViewProps) {
  return (
    <article className={styles.turn}>
      <UserTurn prompt={props.turn.prompt} />
      <div className={styles.response}>{renderState(props)}</div>
    </article>
  );
}
