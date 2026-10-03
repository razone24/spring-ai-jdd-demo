import { useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { Button } from '../../ui/Button';
import { Spinner } from '../../ui/Spinner';
import styles from './Composer.module.css';

const PLACEHOLDER = 'Ask about JDD 2026 — sessions, speakers, venue, tickets…';
const PENDING_PLACEHOLDER = 'Waiting for the current turn to finish…';
const SEND_LABEL = 'Send';
const HINT = 'Enter to send · Shift+Enter for a new line';
const MAX_HEIGHT_PX = 200;

export function Composer({ pending, onSend }: { pending: boolean; onSend: (prompt: string) => void }) {
  const [draft, setDraft] = useState('');
  const field = useRef<HTMLTextAreaElement>(null);
  const disabled = pending || draft.trim().length === 0;

  function resize() {
    const element = field.current;
    if (element) {
      element.style.height = 'auto';
      element.style.height = `${Math.min(element.scrollHeight, MAX_HEIGHT_PX)}px`;
    }
  }

  function send() {
    if (disabled) {
      return;
    }
    onSend(draft.trim());
    setDraft('');
    requestAnimationFrame(resize);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send();
    }
  }

  return (
    <div className={styles.composer}>
      <div className={styles.field}>
        <textarea
          ref={field}
          className={styles.input}
          rows={1}
          value={draft}
          placeholder={pending ? PENDING_PLACEHOLDER : PLACEHOLDER}
          disabled={pending}
          onChange={event => {
            setDraft(event.target.value);
            resize();
          }}
          onKeyDown={handleKeyDown}
        />
        <Button variant="primary" onClick={send} disabled={disabled}>
          {pending ? <Spinner /> : SEND_LABEL}
        </Button>
      </div>
      <p className={styles.hint}>{HINT}</p>
    </div>
  );
}
