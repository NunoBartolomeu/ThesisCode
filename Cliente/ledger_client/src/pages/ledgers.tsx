import '../app/globals.css'

import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { Layout } from '@/components/Layout';
import { LedgerService } from '@/viewmodels/LedgerService';

export default function LedgersPage() {
  const [ledgers, setLedgers] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const ledgerService = new LedgerService();

  useEffect(() => {
    loadLedgers();
  }, []);

  const loadLedgers = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await ledgerService.getAvailableLedgers();

      if (response.success && response.data) {
        setLedgers(response.data);
      } else {
        setLedgers([]);
      }

    } catch (error) {
      console.error('Error loading ledgers:', error);
      setError(error instanceof Error ? error.message : 'Failed to load ledgers');
      setLedgers([]);
    } finally {
      setIsLoading(false);
    }
  };

  const getLedgerIconSvg = () => {
    return (
      <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--primary)' }}>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
  };

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-6xl space-y-8">
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            Ledgers
          </h1>
          
          {error && (
            <div className="rounded-2xl p-4 mb-4 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
              <p>{error}</p>
              <button 
                onClick={() => setError(null)} 
                className="mt-2 px-3 py-1 bg-white text-red-600 rounded"
              >
                Dismiss
              </button>
            </div>
          )}

          <div
            className="rounded-2xl shadow-xl border-2 overflow-hidden transition-all duration-200"
            style={{
              backgroundColor: 'var(--bg)',
              background: 'var(--gradient)',
              borderColor: 'var(--border)'
            }}
          >
            <div className="px-8 py-6 border-b-2" style={{ borderColor: 'var(--border)' }}>
              <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
                  Available Ledgers ({ledgers.length})
                </h2>
                <button
                  onClick={loadLedgers}
                  disabled={isLoading}
                  className="px-4 py-2 rounded transition-colors duration-200"
                  style={{ 
                    backgroundColor: 'var(--primary)', 
                    color: 'white',
                    opacity: isLoading ? 0.6 : 1 
                  }}
                >
                  {isLoading ? 'Loading...' : 'Refresh'}
                </button>
              </div>
            </div>
            
            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
                <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                  Loading ledgers...
                </p>
              </div>
            ) : ledgers.length === 0 ? (
              <div className="p-12 text-center">
                <svg className="mx-auto h-16 w-16 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
                <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                  No ledgers available
                </p>
                <p style={{ color: 'var(--text-muted)' }}>
                  No ledgers have been created yet
                </p>
              </div>
            ) : (
              <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
                {ledgers.map((ledgerName) => (
                  <LedgerRow 
                    key={ledgerName} 
                    ledgerName={ledgerName}
                    onLedgerClick={(name) => router.push(`/ledger-details?ledger=${encodeURIComponent(name)}`)}
                    getIcon={getLedgerIconSvg}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

interface LedgerRowProps {
  ledgerName: string;
  onLedgerClick: (ledgerName: string) => void;
  getIcon: () => React.ReactNode;
}

const LedgerRow = ({ ledgerName, onLedgerClick, getIcon }: LedgerRowProps) => {
  return (
    <div 
      className="px-8 py-6 transition-all duration-200 cursor-pointer"
      style={{ backgroundColor: 'transparent' }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = 'var(--gradient-hover)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'transparent';
      }}
      onClick={() => onLedgerClick(ledgerName)}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4 flex-1 min-w-0">
          <div className="flex-shrink-0">
            {getIcon()}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold truncate" style={{ color: 'var(--text)' }}>
              {ledgerName}
            </p>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </div>
  );
};