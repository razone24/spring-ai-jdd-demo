import { formatValue } from './formatValue';
import { isRecord } from './argumentShape';
import styles from './ToolArguments.module.css';

const NO_ARGUMENTS = 'No arguments';

export function ToolArguments({ value }: { value: unknown }) {
  if (!isRecord(value)) {
    return <div className={styles.scalar}>{formatValue(value)}</div>;
  }
  const entries = Object.entries(value);
  if (entries.length === 0) {
    return <div className={styles.scalar}>{NO_ARGUMENTS}</div>;
  }

  return (
    <dl className={styles.list}>
      {entries.map(([key, argument]) => (
        <div key={key} className={styles.row}>
          <dt className={styles.key}>{key}</dt>
          <dd className={styles.value}>{formatValue(argument)}</dd>
        </div>
      ))}
    </dl>
  );
}
