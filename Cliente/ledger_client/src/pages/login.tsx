import '../app/globals.css';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/router'; // or your routing solution
import { Layout } from '@/components/Layout';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { useAuthViewModel } from '@/viewmodels/AuthViewModel';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { authState, viewModel } = useAuthViewModel();
  const router = useRouter(); // Replace with your routing solution

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isSubmitting) return; // Prevent double submission
    
    setErrors({});
    setIsSubmitting(true);

    try {
      const result = await viewModel.login(email, password);
      if (!result.success) {
        setErrors(result.errors || { general: result.message || 'Login failed' });
      } else if (result.data?.needsVerification) {
        router.push('/two-factor-auth');
      }
    } catch (error) {
      setErrors({ general: 'An unexpected error occurred. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Show loading spinner if initializing auth
  if (authState.isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p style={{ color: 'var(--text-muted)' }}>Loading...</p>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="flex items-center justify-center py-0 px-4 sm:px-6 lg:px-8 pb-40">
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
              Sign in to your account
            </h2>
            <p className="mt-2 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
              Or{' '}
              <a
                href="/register"
                className="font-medium hover:underline transition-colors"
                style={{ color: 'var(--primary)' }}
              >
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
                disabled={isSubmitting}
              />

              <Input
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                error={errors.password}
                disabled={isSubmitting}
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
              isLoading={isSubmitting} 
              disabled={isSubmitting}
              className="w-full"
            >
              {isSubmitting ? 'Signing In...' : 'Sign In'}
            </Button>
          </form>
        </div>
      </div>
    </Layout>
  );
}