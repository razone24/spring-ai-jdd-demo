import { useEffect, useState } from 'react';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'jdd-theme';
const DARK_QUERY = '(prefers-color-scheme: dark)';

function savedTheme(): Theme | null {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'light' || saved === 'dark' ? saved : null;
  } catch {
    return null;
  }
}

function systemTheme(): Theme {
  return matchMedia(DARK_QUERY).matches ? 'dark' : 'light';
}

/**
 * The colour theme on <html data-theme>. Follows the OS until the user picks one; the pick is saved.
 * index.html applies the same rule before first paint, so the page never flashes the other theme.
 */
export function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => savedTheme() ?? systemTheme());

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    const query = matchMedia(DARK_QUERY);
    const follow = () => {
      if (!savedTheme()) setTheme(systemTheme());
    };
    query.addEventListener('change', follow);
    return () => query.removeEventListener('change', follow);
  }, []);

  function toggle() {
    const next: Theme = theme === 'dark' ? 'light' : 'dark';
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Storage blocked: the choice lasts until reload.
    }
    setTheme(next);
  }

  return { theme, toggle };
}
