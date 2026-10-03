import Markdown from 'react-markdown';
import styles from './MessageBody.module.css';

export function MessageBody({ text }: { text: string }) {
  return (
    <div className={styles.body}>
      <Markdown>{text}</Markdown>
    </div>
  );
}
