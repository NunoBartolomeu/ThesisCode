'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';

interface RegisterFormProps {
  onSuccess?: () => void;
  redirectTo?: string;
}

export function RegisterForm({ onSuccess, redirectTo = '/files' }: RegisterFormProps) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [verifyPassword, setVerifyPassword] = useState('');
  
  const router = useRouter();
  const { register, loading, errors, clearFieldError } = useAuthViewModel();

  const handleInputChange = (field: string, value: string) => {
    if (errors[field]) {
      clearFieldError(field);
    }

    switch (field) {
      case 'fullName':
        setFullName(value);
        break;
      case 'email':
        setEmail(value);
        break;
      case 'password':
        setPassword(value);
        break;
      case 'verifyPassword':
        setVerifyPassword(value);
        break;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const result = await register(fullName, email, password, verifyPassword);
    
    if (result?.success) {
        router.push('/two-factor-auth');
    }
  };

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
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold" style={{ color: 'var(--text)' }}>
            Create your account
          </h2>
          <p className="mt-2 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
            Or{' '}
            <a
              href="/login"
              className="font-medium hover:underline transition-colors"
              style={{ color: 'var(--primary)' }}
            >
              sign in to your existing account
            </a>
          </p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
          <div className="space-y-4">
            <Input
              label="Full Name"
              type="text"
              value={fullName}
              onChange={(e) => handleInputChange('fullName', e.target.value)}
              placeholder="Enter your full name"
              required
              error={errors.fullName}
              disabled={loading}
            />

            <Input
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => handleInputChange('email', e.target.value)}
              placeholder="Enter your email"
              required
              error={errors.email}
              disabled={loading}
            />

            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => handleInputChange('password', e.target.value)}
              placeholder="Create a password"
              required
              error={errors.password}
              disabled={loading}
            />

            <Input
              label="Confirm Password"
              type="password"
              value={verifyPassword}
              onChange={(e) => handleInputChange('verifyPassword', e.target.value)}
              placeholder="Confirm your password"
              required
              error={errors.verifyPassword}
              disabled={loading}
            />
          </div>

          {errors.general && (
            <div
              className="rounded-md p-4 border-2"
              style={{
                backgroundColor: 'var(--danger)',
                borderColor: 'var(--danger)',
                color: 'var(--text)',
              }}
            >
              <div className="text-sm font-medium">{errors.general}</div>
            </div>
          )}

          <Button 
            type="submit" 
            isLoading={loading} 
            className="w-full"
            disabled={loading}
          >
            {loading ? 'Creating Account...' : 'Create Account'}
          </Button>
        </form>
      </div>
    </div>
  );
}