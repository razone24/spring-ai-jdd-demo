import styles from './TransportBanner.module.css';

export function TransportBanner({ message }: { message: string }) {
  return <div className={styles.banner}>{message}</div>;
}
