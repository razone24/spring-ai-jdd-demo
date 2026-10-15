import { useEffect, useState } from 'react';

const TICK_MS = 1000;
const MS_PER_SECOND = 1000;

export function useElapsedSeconds(startedAt: number): number {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), TICK_MS);

    return () => clearInterval(timer);
  }, []);

  return Math.max(0, Math.floor((now - startedAt) / MS_PER_SECOND));
}
