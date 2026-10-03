import styles from './UserTurn.module.css';

export function UserTurn({ prompt }: { prompt: string }) {
  return (
    <div className={styles.row}>
      <p className={styles.bubble}>{prompt}</p>
    </div>
  );
}
