import { isPending } from '../domain/conversation';
import { Composer } from '../features/composer/Composer';
import { Sidebar } from '../features/conversation-list/Sidebar';
import { Transcript } from '../features/transcript/Transcript';
import { useActiveConversation, useConversations, useOrderedConversations } from '../state/useConversations';
import { ConversationHeader } from './ConversationHeader';
import { TransportBanner } from './TransportBanner';
import styles from './App.module.css';

export function App() {
  const { state, startConversation, openConversation, sendPrompt, abortTurn } = useConversations();
  const conversations = useOrderedConversations();
  const active = useActiveConversation();

  return (
    <div className={styles.layout}>
      <Sidebar
        conversations={conversations}
        activeKey={state.activeKey}
        onCreate={startConversation}
        onSelect={openConversation}
      />
      {active && (
        <main className={styles.main}>
          <ConversationHeader conversation={active} />
          {state.transportFailure && <TransportBanner message={state.transportFailure} />}
          <div className={styles.body}>
            <Transcript
              conversation={active}
              onCancel={abortTurn}
              onRetry={prompt => sendPrompt(active.key, prompt)}
            />
          </div>
          <Composer pending={isPending(active)} onSend={prompt => sendPrompt(active.key, prompt)} />
        </main>
      )}
    </div>
  );
}
