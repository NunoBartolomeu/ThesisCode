'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation'; // next/navigation for App Router
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';

interface LoginFormProps {
  onSuccess?: () => void;
  redirectTo?: string;
}

export function LoginForm({ onSuccess, redirectTo = '/dashboard' }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const router = useRouter();
  
  const { login, loading, errors } = useAuthViewModel();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const result = await login(email, password);
    
    if (result?.success) {
        router.push('/two-factor-auth');
    }
  };

  return (
    <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8 pb-40">
      <div className="max-w-md w-full space-y-8 rounded-2xl shadow-xl p-10 border-2 transition-all duration-200">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold">
            Sign in to your account
          </h2>
          <p className="mt-2 text-center text-sm">
            Or{' '}
            <a href="/register" className="font-medium hover:underline transition-colors">
              create a new account
            </a>
          </p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
          <div className="space-y-4">
            <Input
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              required
              error={errors.email}
              disabled={loading}
            />

            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              error={errors.password}
              disabled={loading}
            />
          </div>

          {errors.general && (
            <div className="rounded-md p-4 border-2 bg-red-50 border-red-200 text-red-800">
              <div className="text-sm font-medium">{errors.general}</div>
            </div>
          )}

          <Button 
            type="submit" 
            isLoading={loading} 
            disabled={loading}
            className="w-full"
          >
            {loading ? 'Signing In...' : 'Sign In'}
          </Button>
        </form>
      </div>
    </div>
  );
}