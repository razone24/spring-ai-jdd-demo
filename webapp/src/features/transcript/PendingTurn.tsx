import { Button } from '../../ui/Button';
import { useElapsedSeconds } from './useElapsedSeconds';
import styles from './PendingTurn.module.css';

const WAITING_LABEL = 'waiting on model';
const CANCEL_LABEL = 'Cancel';

export function PendingTurn({ startedAt, onCancel }: { startedAt: number; onCancel: () => void }) {
  const seconds = useElapsedSeconds(startedAt);

  return (
    <div className={styles.pending}>
      <span className={styles.dots} aria-hidden="true">
        <i />
        <i />
        <i />
      </span>
      <span className={styles.text}>
        {WAITING_LABEL} · {seconds}s
      </span>
      <Button variant="ghost" onClick={onCancel}>
        {CANCEL_LABEL}
      </Button>
    </div>
  );
}
