import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Button.module.css';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  children: ReactNode;
};

export function Button({ variant = 'secondary', className, children, ...rest }: ButtonProps) {
  return (
    <button type="button" className={[styles.button, styles[variant], className].filter(Boolean).join(' ')} {...rest}>
      {children}
    </button>
  );
}
