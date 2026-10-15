import { describeFailure } from '../../domain/failure';
import type { QueryFailure } from '../../domain/failure';
import { Button } from '../../ui/Button';
import styles from './ErrorTurn.module.css';

const RETRY_LABEL = 'Retry';
const ID_NOTE = 'The conversation id was kept — the error response’s id was discarded.';

export function ErrorTurn({ failure, onRetry }: { failure: QueryFailure; onRetry: () => void }) {
  return (
    <aside className={styles.error}>
      <header className={styles.header}>
        <span className={styles.label}>{describeFailure(failure)}</span>
        <Button variant="ghost" onClick={onRetry}>
          {RETRY_LABEL}
        </Button>
      </header>
      <p className={styles.message}>{failure.message}</p>
      {failure.kind === 'http' && <p className={styles.note}>{ID_NOTE}</p>}
    </aside>
  );
}
