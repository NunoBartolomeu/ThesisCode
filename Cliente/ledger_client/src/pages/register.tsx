import '../app/globals.css';
import { useState, useEffect } from 'react';
import { Layout } from '@/components/Layout';
import { useRouter } from 'next/router'; // or your routing solution
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { useAuthViewModel } from '@/viewmodels/AuthViewModel';

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [verifyPassword, setVerifyPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const router = useRouter(); // Replace with your routing solution

  const { authState, viewModel } = useAuthViewModel();

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
    setErrors({});
    setIsSubmitting(true);

    try {
      const result = await viewModel.register(fullName, email, password, verifyPassword);

      if (!result.success) {
        setErrors(result.errors || { general: result.message || 'Registration failed' });
      }
      // Note: Don't handle redirection here - let the useEffect handle it
      // based on authState changes
    } catch (error) {
      setErrors({ general: 'An unexpected error occurred. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleInputChange = (field: string, value: string) => {
    // Clear field-specific error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
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

  // Show loading state while initializing
  if (authState.isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto mb-4" 
                 style={{ borderColor: 'var(--primary)' }}></div>
            <p style={{ color: 'var(--text-muted)' }}>Loading...</p>
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
                disabled={isSubmitting}
              />

              <Input
                label="Email Address"
                type="email"
                value={email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                placeholder="Enter your email"
                required
                error={errors.email}
                disabled={isSubmitting}
              />

              <Input
                label="Password"
                type="password"
                value={password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                placeholder="Create a password"
                required
                error={errors.password}
                disabled={isSubmitting}
              />

              <Input
                label="Confirm Password"
                type="password"
                value={verifyPassword}
                onChange={(e) => handleInputChange('verifyPassword', e.target.value)}
                placeholder="Confirm your password"
                required
                error={errors.verifyPassword}
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
              className="w-full"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Creating Account...' : 'Create Account'}
            </Button>
          </form>
        </div>
      </div>
    </Layout>
  );
}