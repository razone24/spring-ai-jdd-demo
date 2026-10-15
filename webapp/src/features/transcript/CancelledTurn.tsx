import { Button } from '../../ui/Button';
import styles from './CancelledTurn.module.css';

const CANCELLED_LABEL = 'Cancelled before the model replied.';
const RETRY_LABEL = 'Send again';

export function CancelledTurn({ onRetry }: { onRetry: () => void }) {
  return (
    <div className={styles.cancelled}>
      <span>{CANCELLED_LABEL}</span>
      <Button variant="ghost" onClick={onRetry}>
        {RETRY_LABEL}
      </Button>
    </div>
  );
}
