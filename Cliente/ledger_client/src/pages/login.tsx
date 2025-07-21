import '../app/globals.css';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { AuthService } from '@/viewmodels/AuthService';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const authService = new AuthService()
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isSubmitting) return; // Prevent double submission
    
    setErrors({});
    setIsSubmitting(true);

    try {
      const result = await authService.login(email, password);
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