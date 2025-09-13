'use client';

import { useRouter } from 'next/navigation';
import { Layout } from '@/components/ui/Layout';
import { Button } from '@/components/ui/Button';

export default function WelcomePage() {
  const router = useRouter();

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-80">
        <div 
          className="w-4/5 max-w-4xl rounded-2xl shadow-xl p-10 border-2 text-center transition-all duration-200" 
          style={{ 
            backgroundColor: 'var(--bg)',
            background: 'var(--gradient)',
            borderColor: 'var(--border)'
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
          <h1 className="text-5xl font-bold mb-4" style={{ color: 'var(--text)' }}>
            Welcome to File Manager
          </h1>
          <h2 className="text-xl font-medium mb-10" style={{ color: 'var(--text-muted)' }}>
            (powered by LedgerKotlin)
          </h2>

          <div className="flex justify-center gap-6 mb-6">
            <div className="text-center">
              <p style={{ color: 'var(--text-muted)' }} className="mb-2">Coming back?</p>
              <Button onClick={() => router.push('/login')}>
                Login
              </Button>
            </div>
            <div className="text-center">
              <p style={{ color: 'var(--text-muted)' }} className="mb-2">New here?</p>
              <Button onClick={() => router.push('/register')}>
                Register
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}