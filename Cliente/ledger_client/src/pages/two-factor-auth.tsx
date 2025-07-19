import '../app/globals.css';
import { useState, useRef, useEffect } from 'react';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/Button';
import { useRouter } from 'next/router'; // or your routing solution
import { useAuthViewModel } from '@/viewmodels/AuthViewModel';

export default function TwoFactorAuthPage() {
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const { authState, viewModel } = useAuthViewModel();
  const router = useRouter(); // Replace with your routing solution

  // Focus first input on mount
  useEffect(() => {
    if (inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, []);

  // Handle navigation based on auth state changes
  useEffect(() => {
    if (authState.isLoading || !authState.user) {
      return
    }
    if (authState.token) {
      router.push('/files');
    } else {
      router.push('/two-factor-auth');
    }
  }, [authState.token, authState.user, authState.isLoading, router]);

  const handleInputChange = (index: number, value: string) => {
    // Only allow digits
    if (value && !/^\d$/.test(value)) return;

    const newCode = [...code];
    newCode[index] = value;
    setCode(newCode);

    // Move to next input if value entered
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
      // Move to previous input on backspace if current is empty
      if (e.key === 'Backspace' && !code[index] && index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
      
      // Move to next input on arrow right
      if (e.key === 'ArrowRight' && index < 5) {
        inputRefs.current[index + 1]?.focus();
      }
      
      // Move to previous input on arrow left
      if (e.key === 'ArrowLeft' && index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
    };

    const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    const fullCode = code.join('');
    
    if (fullCode.length !== 6) {
      setError('Please enter all 6 digits');
      return;
    }
    
    
    const result = await viewModel.verify2FA(fullCode);
    
    if (!result.success) {
      setError(result.message || result.errors?.code || '2FA verification failed');
      // Clear the code on error
      setCode(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } 
  };

  const handleResendCode = async () => {
    setError('');
    // You can implement this in your AuthViewModel if needed
  };

  // Show loading state
  if (authState.isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-lg" style={{ color: 'var(--text)' }}>Loading...</p>
          </div>
        </div>
      </Layout>
    );
  }

  // UPDATED: Show error state if not in 2FA state (instead of checking sessionData)
  if (!authState.user) {
    return (
      <Layout>
        <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
              Invalid Access
            </h2>
            <p className="mt-2" style={{ color: 'var(--text-muted)' }}>
              You don't have an active 2FA session. Please log in first.
            </p>
            <Button 
              onClick={() => window.location.href = '/login'}
              className="mt-4"
            >
              Go to Login
            </Button>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
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
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h2 className="mt-6 text-3xl font-extrabold" style={{ color: 'var(--text)' }}>
              Two-Factor Authentication
            </h2>
            <p className="mt-2 text-sm" style={{ color: 'var(--text-muted)' }}>
              Enter the 6-digit code from your authenticator app
            </p>
            {authState.user.email && (
              <p className="mt-1 text-xs" style={{ color: 'var(--text-muted)' }}>
                Code sent to {authState.user.email}
              </p>
            )}
          </div>

          <form onSubmit={handleSubmit} className="mt-8 space-y-6">
            <div className="flex justify-center space-x-2">
              {code.map((digit, index) => (
                <input
                  key={index}
                  ref={el => {
                    inputRefs.current[index] = el;
                  }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleInputChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  className="w-12 h-12 text-center text-xl font-bold border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-opacity-50 transition-colors"
                  style={{
                    backgroundColor: 'var(--bg-light)',
                    borderColor: 'var(--border)',
                    color: 'var(--text)',
                  }}
                  onFocus={(e) => {
                    e.currentTarget.style.borderColor = 'var(--primary)';
                    e.currentTarget.style.boxShadow = `0 0 0 2px rgba(var(--primary-rgb), 0.2)`;
                  }}
                  onBlur={(e) => {
                    e.currentTarget.style.borderColor = 'var(--border)';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                  placeholder="0"
                />
              ))}
            </div>

            {error && (
              <div
                className="rounded-md p-4 border-2"
                style={{
                  backgroundColor: 'var(--danger)',
                  borderColor: 'var(--danger)',
                  color: 'var(--text)',
                }}
              >
                <div className="text-sm font-medium text-center">{error}</div>
              </div>
            )}

            <Button 
              type="submit" 
              isLoading={authState.isLoading} 
              className="w-full"
            >
              Verify Code
            </Button>

            <div className="text-center">
              <button
                type="button"
                onClick={handleResendCode}
                className="text-sm font-medium hover:underline transition-colors"
                style={{ color: 'var(--primary)' }}
              >
                Didn't receive a code? Resend
              </button>
            </div>

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
    </Layout>
  );
}