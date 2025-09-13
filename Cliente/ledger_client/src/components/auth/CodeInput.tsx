'use client';

import { useState, useRef, useEffect } from 'react';

interface CodeInputProps {
  length?: number;
  value: string[];
  onChange: (code: string[]) => void;
  onComplete?: (code: string) => void;
  disabled?: boolean;
  autoFocus?: boolean;
  placeholder?: string;
}

export function CodeInput({ 
  length = 6, 
  value, 
  onChange, 
  onComplete, 
  disabled = false,
  autoFocus = true,
  placeholder = "0"
}: CodeInputProps) {
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  
  useEffect(() => {
    if (autoFocus && inputRefs.current[0] && !disabled) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus, disabled]);

  const handleInputChange = (index: number, inputValue: string) => {
    // Only allow digits
    if (inputValue && !/^\d$/.test(inputValue)) return;

    const newCode = [...value];
    newCode[index] = inputValue;
    onChange(newCode);

    // Move to next input if value entered
    if (inputValue && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }

    // Check if code is complete
    if (onComplete && newCode.every(digit => digit !== '') && newCode.length === length) {
      onComplete(newCode.join(''));
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    // Move to previous input on backspace if current is empty
    if (e.key === 'Backspace' && !value[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
    
    // Move to next input on arrow right
    if (e.key === 'ArrowRight' && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
    
    // Move to previous input on arrow left
    if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
    
    // Submit form on Enter if on last digit and all digits are filled
    if (e.key === 'Enter' && index === length - 1) {
      const fullCode = value.join('');
      if (fullCode.length === length && onComplete) {
        onComplete(fullCode);
      }
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text');
    const digits = pastedData.replace(/\D/g, '').slice(0, length);
    
    if (digits.length > 0) {
      const newCode = [...value];
      for (let i = 0; i < Math.min(digits.length, length); i++) {
        newCode[i] = digits[i];
      }
      onChange(newCode);
      
      // Focus the next empty input or the last input if all filled
      const nextEmptyIndex = newCode.findIndex(digit => digit === '');
      const focusIndex = nextEmptyIndex === -1 ? length - 1 : nextEmptyIndex;
      inputRefs.current[focusIndex]?.focus();
      
      // Check if code is complete
      if (onComplete && newCode.every(digit => digit !== '') && newCode.length === length) {
        onComplete(newCode.join(''));
      }
    }
  };

  return (
    <div className="flex justify-center space-x-2">
      {Array.from({ length }).map((_, index) => (
        <input
          key={index}
          ref={el => {
            inputRefs.current[index] = el;
          }}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={value[index] || ''}
          onChange={(e) => handleInputChange(index, e.target.value)}
          onKeyDown={(e) => handleKeyDown(index, e)}
          onPaste={handlePaste}
          disabled={disabled}
          className="w-12 h-12 text-center text-xl font-bold border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-opacity-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          style={{
            backgroundColor: 'var(--bg-light)',
            borderColor: 'var(--border)',
            color: 'var(--text)',
          }}
          onFocus={(e) => {
            if (!disabled) {
              e.currentTarget.style.borderColor = 'var(--primary)';
              e.currentTarget.style.boxShadow = `0 0 0 2px rgba(var(--primary-rgb), 0.2)`;
            }
          }}
          onBlur={(e) => {
            e.currentTarget.style.borderColor = 'var(--border)';
            e.currentTarget.style.boxShadow = 'none';
          }}
          placeholder={placeholder}
        />
      ))}
    </div>
  );
}