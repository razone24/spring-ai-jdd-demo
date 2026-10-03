import { useState } from 'react';
import styles from './CopyButton.module.css';

const RESET_DELAY_MS = 1200;
const COPY_LABEL = 'Copy';
const COPIED_LABEL = 'Copied';

export function CopyButton({ value, label }: { value: string; label?: string }) {
  const [copied, setCopied] = useState(false);

  function copy() {
    void navigator.clipboard.writeText(value).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), RESET_DELAY_MS);
    });
  }

  return (
    <button
      type="button"
      className={styles.button}
      onClick={copy}
      title={label ?? COPY_LABEL}
      aria-label={label ?? COPY_LABEL}
    >
      {copied ? COPIED_LABEL : COPY_LABEL}
    </button>
  );
}
