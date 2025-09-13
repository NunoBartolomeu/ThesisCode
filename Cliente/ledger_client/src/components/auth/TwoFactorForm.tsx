'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { CodeInput } from '@/components/auth/CodeInput';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';
import { AuthStorageService } from '@/services/storage/AuthStorageService';

interface TwoFactorFormProps {
  onSuccess?: () => void;
  redirectTo?: string;
}

export function TwoFactorForm({ onSuccess, redirectTo = '/files' }: TwoFactorFormProps) {
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const router = useRouter();
  
  const { verify2FA, loading, errors, clearErrors, user } = useAuthViewModel();

  // Check if user has valid session on mount
  useEffect(() => {
    const savedUser = AuthStorageService.getUser();
    if (!savedUser) {
      router.push('/login');
    }
  }, [router]);

  const handleCodeChange = (newCode: string[]) => {
    setCode(newCode);
    // Clear errors when user starts typing
    if (errors.code || errors.general) {
      clearErrors();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitCode();
  };

  const handleCodeComplete = async (fullCode: string) => {
    if (fullCode.length === 6) {
      await submitCode(fullCode);
    }
  };

  const submitCode = async (providedCode?: string) => {
    const fullCode = providedCode || code.join('');
    
    if (fullCode.length !== 6) {
      return;
    }

    const result = await verify2FA(fullCode);
    
    if (result?.success) {
      onSuccess?.();
      router.push(redirectTo);
    } else {
      // Clear the code inputs on error since server invalidates the code
      setCode(['', '', '', '', '', '']);
    }
  };

  // Show invalid access if no user session
  if (!user) {
    return (
      <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
            Invalid Access
          </h2>
          <p className="mt-2" style={{ color: 'var(--text-muted)' }}>
            You don't have an active 2FA session. Please log in first.
          </p>
          <Button 
            onClick={() => router.push('/login')}
            className="mt-4"
          >
            Go to Login
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8">
      <div
        className="max-w-md w-full space-y-8 rounded-2xl shadow-xl p-10 border-2 transition-all duration-200"
        style={{
          backgroundColor: 'var(--bg)',
          background: 'var(--gradient)',
          borderColor: 'var(--border)',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = 'var(--gradient-hover)';
          e.currentTarget.style.borderColor = 'var(--primary)';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.background = 'var(--gradient)';
          e.currentTarget.style.borderColor = 'var(--border)';
        }}
      >
        <div className="text-center">
          <div 
            className="mx-auto h-12 w-12 flex items-center justify-center rounded-full border-2"
            style={{
              backgroundColor: 'var(--primary)',
              borderColor: 'var(--primary)',
            }}
          >
            <svg className="h-6 w-6" fill="white" stroke="white" viewBox="0 0 24 24">
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" 
              />
            </svg>
          </div>
          <h2 className="mt-6 text-3xl font-extrabold" style={{ color: 'var(--text)' }}>
            Two-Factor Authentication
          </h2>
          <p className="mt-2 text-sm" style={{ color: 'var(--text-muted)' }}>
            Enter the 6-digit code from your authenticator app
          </p>
          {user?.email && (
            <p className="mt-1 text-xs" style={{ color: 'var(--text-muted)' }}>
              Code sent to {user.email}
            </p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
          <CodeInput
            value={code}
            onChange={handleCodeChange}
            onComplete={handleCodeComplete}
            disabled={loading}
            length={6}
          />

          {errors.code && (
            <div
              className="rounded-md p-4 border-2"
              style={{
                backgroundColor: 'var(--danger)',
                borderColor: 'var(--danger)',
                color: 'var(--text)',
              }}
            >
              <div className="text-sm font-medium text-center">{errors.code}</div>
            </div>
          )}

          {errors.general && (
            <div
              className="rounded-md p-4 border-2"
              style={{
                backgroundColor: 'var(--danger)',
                borderColor: 'var(--danger)',
                color: 'var(--text)',
              }}
            >
              <div className="text-sm font-medium text-center">{errors.general}</div>
            </div>
          )}

          <Button 
            type="submit" 
            isLoading={loading} 
            disabled={loading || code.join('').length !== 6}
            className="w-full"
          >
            {loading ? 'Verifying...' : 'Verify Code'}
          </Button>

          <div className="text-center">
            <a 
              href="/login" 
              className="text-sm hover:underline transition-colors"
              style={{ color: 'var(--text-muted)' }}
            >
              ← Back to login
            </a>
          </div>
        </form>
      </div>
    </div>
  );
}