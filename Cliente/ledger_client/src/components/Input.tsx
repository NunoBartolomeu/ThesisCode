import '../app/globals.css'

import { InputHTMLAttributes, forwardRef, useState } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className = '', type, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);
    const isPasswordInput = type === 'password';
    const inputType = isPasswordInput && showPassword ? 'text' : type;

    const togglePasswordVisibility = () => {
      setShowPassword(!showPassword);
    };

    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-medium mb-2" style={{ color: 'var(--text)' }}>
            {label}
          </label>
        )}
        <div className="relative">
          <input
            ref={ref}
            type={inputType}
            className={`w-full px-4 py-3 ${isPasswordInput ? 'pr-12' : ''} rounded-lg border-2 transition-all duration-200 
              focus:outline-none focus:ring-2 focus:ring-opacity-50 
              ${error ? 'border-red-500 focus:border-red-500' : ''} 
              ${className}`}
            style={{
              backgroundColor: 'var(--bg-light)',
              color: 'var(--text)',
              borderColor: error ? 'var(--danger)' : 'var(--border)'
            }}
            onFocus={(e) => {
              if (!error) {
                e.target.style.borderColor = 'var(--primary)';
              }
            }}
            onBlur={(e) => {
              if (!error) {
                e.target.style.borderColor = 'var(--border)';
              }
            }}
            {...props}
          />
          {isPasswordInput && (
            <button
              type="button"
              onClick={togglePasswordVisibility}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 p-1 rounded-md transition-colors duration-200 hover:opacity-70"
              style={{ color: 'var(--text)' }}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? (
                // Eye slash icon (hide)
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              ) : (
                // Eye icon (show)
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              )}
            </button>
          )}
        </div>
        {error && (
          <p className="mt-2 text-sm font-medium" style={{ color: 'var(--danger)' }}>
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';