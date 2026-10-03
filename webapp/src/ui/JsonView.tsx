import styles from './JsonView.module.css';
import type { ReactNode } from 'react';

const TOKEN_PATTERN = /("(?:\\.|[^"\\])*"\s*:)|("(?:\\.|[^"\\])*")|(\b-?\d+(?:\.\d+)?(?:e[+-]?\d+)?\b)|(\btrue\b|\bfalse\b|\bnull\b)/gi;
const INDENT = 2;

const TOKEN_CLASSES = [styles.key, styles.string, styles.number, styles.literal];

function highlight(json: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let cursor = 0;

  for (const match of json.matchAll(TOKEN_PATTERN)) {
    const start = match.index;
    if (start > cursor) {
      nodes.push(json.slice(cursor, start));
    }
    const group = match.slice(1).findIndex(Boolean);
    nodes.push(
      <span key={start} className={TOKEN_CLASSES[group]}>
        {match[0]}
      </span>,
    );
    cursor = start + match[0].length;
  }
  nodes.push(json.slice(cursor));

  return nodes;
}

export function JsonView({ value }: { value: unknown }) {
  return <pre className={styles.json}>{highlight(JSON.stringify(value, null, INDENT) ?? '')}</pre>;
}
