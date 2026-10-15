import type { RefusalReason } from '../../domain/models';
import styles from './RefusalNotice.module.css';

const REFUSED_LABEL = 'Refused';
const UNKNOWN_REASON = 'unknown reason';

export function RefusalNotice({ reason, message }: { reason: RefusalReason | null; message: string }) {
  return (
    <aside className={styles.notice}>
      <header className={styles.header}>
        <span className={styles.label}>{REFUSED_LABEL}</span>
        <span className={styles.reason}>{reason ?? UNKNOWN_REASON}</span>
      </header>
      <p className={styles.message}>{message}</p>
    </aside>
  );
}
