import type { Conversation, ConversationKey } from '../../domain/conversation';
import { Button } from '../../ui/Button';
import { ConversationItem } from './ConversationItem';
import styles from './Sidebar.module.css';

const BRAND = 'JDD 2026 Assistant';
const BRAND_DETAIL = 'Spring AI · MCP · Ollama';
const HEADING = 'Conversations';
const NEW_LABEL = 'New';
const EMPTY_LABEL = 'No conversations yet.';

type SidebarProps = {
  conversations: Conversation[];
  activeKey: ConversationKey | null;
  onCreate: () => void;
  onSelect: (key: ConversationKey) => void;
};

export function Sidebar({ conversations, activeKey, onCreate, onSelect }: SidebarProps) {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <span className={styles.brandName}>{BRAND}</span>
        <span className={styles.brandDetail}>{BRAND_DETAIL}</span>
      </div>
      <header className={styles.header}>
        <h1 className={styles.heading}>{HEADING}</h1>
        <Button variant="primary" onClick={onCreate}>
          {NEW_LABEL}
        </Button>
      </header>
      {conversations.length === 0 ? (
        <p className={styles.empty}>{EMPTY_LABEL}</p>
      ) : (
        <ul className={styles.list}>
          {conversations.map(conversation => (
            <ConversationItem
              key={conversation.key}
              conversation={conversation}
              selected={conversation.key === activeKey}
              onSelect={() => onSelect(conversation.key)}
            />
          ))}
        </ul>
      )}
    </aside>
  );
}
