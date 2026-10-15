import { buildTitle } from '../domain/conversation';
import type { Conversation } from '../domain/conversation';
import { CopyButton } from '../ui/CopyButton';
import styles from './ConversationHeader.module.css';

const UNSENT_LABEL = 'id assigned by the backend on the first turn';
const GRAFANA_URL = import.meta.env.VITE_GRAFANA_URL ?? 'http://localhost:3000/d/jdd-assistant';
const GRAFANA_LABEL = 'Grafana ↗';

export function ConversationHeader({ conversation }: { conversation: Conversation }) {
  const { conversationId } = conversation;

  return (
    <header className={styles.header}>
      <h2 className={styles.title}>{buildTitle(conversation)}</h2>
      <div className={styles.aside}>
        {conversationId ? (
          <span className={styles.id}>
            {conversationId}
            <CopyButton value={conversationId} label="Copy conversation id" />
          </span>
        ) : (
          <span className={styles.unsent}>{UNSENT_LABEL}</span>
        )}
        <a className={styles.link} href={GRAFANA_URL} target="_blank" rel="noreferrer">
          {GRAFANA_LABEL}
        </a>
      </div>
    </header>
  );
}
