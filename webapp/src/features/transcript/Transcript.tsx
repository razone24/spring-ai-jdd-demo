import { useEffect, useRef } from 'react';
import type { Conversation } from '../../domain/conversation';
import { TurnView } from './TurnView';
import styles from './Transcript.module.css';

const BOTTOM_THRESHOLD_PX = 120;
const EMPTY_TITLE = 'Ask the JDD 2026 assistant';
const EMPTY_HINT = 'A Spring AI agent on a local LLM, reaching its tools over MCP. Try one of these:';
const EXAMPLE_PROMPTS = [
  'When and where is the talk about software architecture in the age of AI?',
  'What is the Wi-Fi password at the venue?',
  'Which AI talks are on day 2?',
  'How much is a student ticket?',
  'Write me a poem about pizza',
];

type TranscriptProps = {
  conversation: Conversation;
  onCancel: (turnKey: string) => void;
  onRetry: (prompt: string) => void;
};

export function Transcript({ conversation, onCancel, onRetry }: TranscriptProps) {
  const scroller = useRef<HTMLDivElement>(null);
  const pinned = useRef(true);

  useEffect(() => {
    const element = scroller.current;
    if (element && pinned.current) {
      element.scrollTop = element.scrollHeight;
    }
  }, [conversation]);

  function trackPinned() {
    const element = scroller.current;
    if (element) {
      pinned.current = element.scrollHeight - element.scrollTop - element.clientHeight < BOTTOM_THRESHOLD_PX;
    }
  }

  if (conversation.turns.length === 0) {
    return (
      <div className={styles.empty}>
        <h2 className={styles.emptyTitle}>{EMPTY_TITLE}</h2>
        <p className={styles.emptyHint}>{EMPTY_HINT}</p>
        <ul className={styles.examples}>
          {EXAMPLE_PROMPTS.map(prompt => (
            <li key={prompt}>
              <button type="button" className={styles.example} onClick={() => onRetry(prompt)}>
                {prompt}
              </button>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div className={styles.scroller} ref={scroller} onScroll={trackPinned}>
      <div className={styles.column}>
        {conversation.turns.map(turn => (
          <TurnView key={turn.key} turn={turn} onCancel={onCancel} onRetry={onRetry} />
        ))}
      </div>
    </div>
  );
}
