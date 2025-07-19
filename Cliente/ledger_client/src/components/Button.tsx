import '../app/globals.css';
import { ReactNode, ButtonHTMLAttributes, AnchorHTMLAttributes } from 'react';

type CommonProps = {
  children: ReactNode;
  isLoading?: boolean;
  className?: string;
};

type ButtonProps =
  | (CommonProps & ButtonHTMLAttributes<HTMLButtonElement> & { as?: 'button' })
  | (CommonProps & AnchorHTMLAttributes<HTMLAnchorElement> & { as: 'a' });

export const Button = ({
  children,
  isLoading = false,
  className = '',
  as = 'button',
  ...props
}: ButtonProps) => {
  const isDisabled = isLoading;

  const baseClass = `font-medium rounded-lg px-6 py-3 text-base border-2 transform transition-all duration-100
    hover:shadow-lg hover:scale-105 focus:outline-none focus:ring-2 focus:ring-offset-2
    ${isDisabled ? 'opacity-50 cursor-not-allowed pointer-events-none' : ''}
    ${className}`;

  const style = {
    background: 'var(--gradient)',
    color: 'var(--text)',
    borderColor: 'var(--border)',
  };

  const onMouseEnter = (e: any) => {
    if (!isDisabled) {
      e.currentTarget.style.background = 'var(--gradient-hover)';
      e.currentTarget.style.borderColor = 'var(--primary)';
    }
  };

  const onMouseLeave = (e: any) => {
    if (!isDisabled) {
      e.currentTarget.style.background = 'var(--gradient)';
      e.currentTarget.style.borderColor = 'var(--border)';
    }
  };

  if (as === 'a') {
    return (
      <a
        className={baseClass}
        style={style}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        {...(props as AnchorHTMLAttributes<HTMLAnchorElement>)}
      >
        {children}
      </a>
    );
  }

  return (
    <button
      className={baseClass}
      style={style}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      disabled={isDisabled}
      {...(props as ButtonHTMLAttributes<HTMLButtonElement>)}
    >
      {isLoading ? (
        <span className="flex items-center">
          <svg
            className="animate-spin -ml-1 mr-2 h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          Loading...
        </span>
      ) : (
        children
      )}
    </button>
  );
};
