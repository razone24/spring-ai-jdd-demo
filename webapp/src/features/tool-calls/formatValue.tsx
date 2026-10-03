import { PillList } from '../../ui/PillList';
import type { ReactNode } from 'react';

const EMPTY_MARK = '—';
const LIST_SEPARATOR = ', ';

export function formatValue(value: unknown): ReactNode {
  if (value === null || value === undefined) {
    return EMPTY_MARK;
  }
  if (Array.isArray(value)) {
    return <PillList values={value.map(item => String(item))} />;
  }
  if (typeof value === 'object') {
    return Object.entries(value).map(([key, nested]) => `${key}: ${String(nested)}`).join(LIST_SEPARATOR);
  }

  return String(value);
}
