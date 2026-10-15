import { buildTitle, isPending } from '../../domain/conversation';
import type { Conversation } from '../../domain/conversation';
import { CopyButton } from '../../ui/CopyButton';
import { Spinner } from '../../ui/Spinner';
import styles from './ConversationItem.module.css';

const UNSENT_LABEL = 'not yet sent';
const ID_PREVIEW_LENGTH = 8;
const SEPARATOR = '·';

function describeTurns(count: number): string {
  return count === 1 ? '1 turn' : `${count} turns`;
}

type ConversationItemProps = {
  conversation: Conversation;
  selected: boolean;
  onSelect: () => void;
};

export function ConversationItem({ conversation, selected, onSelect }: ConversationItemProps) {
  const { conversationId } = conversation;

  return (
    <li>
      <button
        type="button"
        className={selected ? [styles.item, styles.selected].join(' ') : styles.item}
        onClick={onSelect}
      >
        <span className={styles.title}>
          {buildTitle(conversation)}
          {isPending(conversation) && <Spinner />}
        </span>
        <span className={styles.meta}>
          {conversationId ? (
            <>
              <span className={styles.id} title={conversationId}>
                {conversationId.slice(0, ID_PREVIEW_LENGTH)}
              </span>
              <CopyButton value={conversationId} label="Copy conversation id" />
            </>
          ) : (
            <span className={styles.unsent}>{UNSENT_LABEL}</span>
          )}
          <span>{SEPARATOR}</span>
          <span>{describeTurns(conversation.turns.length)}</span>
        </span>
      </button>
    </li>
  );
}
