import type { ReactNode } from 'react';
import styles from './Chip.module.css';

export function Chip({ children, title }: { children: ReactNode; title?: string }) {
  return (
    <span className={styles.chip} title={title}>
      {children}
    </span>
  );
}
