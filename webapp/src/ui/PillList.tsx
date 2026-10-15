import styles from './PillList.module.css';

const VISIBLE_LIMIT = 3;
const EMPTY_MARK = '—';

export function PillList({ values }: { values: string[] }) {
  if (values.length === 0) {
    return <span className={styles.empty}>{EMPTY_MARK}</span>;
  }
  const visible = values.slice(0, VISIBLE_LIMIT);
  const hidden = values.length - visible.length;

  return (
    <span className={styles.list}>
      {visible.map(value => (
        <span key={value} className={styles.pill}>
          {value}
        </span>
      ))}
      {hidden > 0 && (
        <span className={styles.more} title={values.join('\n')}>
          +{hidden}
        </span>
      )}
    </span>
  );
}
