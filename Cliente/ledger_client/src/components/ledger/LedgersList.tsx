'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useLedgerViewModel } from '@/viewmodels/useLedgerViewModel';
import { PageContainer } from '@/components/ui/PageContainer';
import { ListingContainer } from '@/components/ui/ListingContainer';
import { ListingItem } from '@/components/ui/ListingItem';
import { EmptyState } from '@/components/ui/EmptyState';
import { Icons } from '@/components/ui/Icons';

interface LedgersListProps {
  onLedgerClick?: (ledgerName: string) => void;
}

export function LedgersList({ onLedgerClick }: LedgersListProps) {
  const router = useRouter();
  const { ledgers, loading, error, loadLedgers, clearError } = useLedgerViewModel();

  useEffect(() => {
    loadLedgers();
  }, [loadLedgers]);

  const handleLedgerClick = (ledgerName: string) => {
    if (onLedgerClick) {
      onLedgerClick(ledgerName);
    } else {
      router.push(`/ledger-details?ledger=${encodeURIComponent(ledgerName)}`);
    }
  };

  return (
    <PageContainer title="Ledgers">
      {error && (
        <div className="rounded-2xl p-4 mb-4 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
          <p>{error}</p>
          <button 
            onClick={clearError}
            className="mt-2 px-3 py-1 bg-white text-red-600 rounded"
          >
            Dismiss
          </button>
        </div>
      )}

      <ListingContainer
        title="Available Ledgers"
        count={ledgers.length}
        headerIcon={<Icons.Ledger />}
        actions={
          <button
            onClick={loadLedgers}
            disabled={loading}
            className="px-4 py-2 rounded transition-colors duration-200"
            style={{ 
              backgroundColor: 'var(--primary)', 
              color: 'white',
              opacity: loading ? 0.6 : 1 
            }}
          >
            {loading ? 'Loading...' : 'Refresh'}
          </button>
        }
      >
        {loading ? (
          <div className="p-12 text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
            <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
              Loading ledgers...
            </p>
          </div>
        ) : ledgers.length === 0 ? (
          <EmptyState
            icon={<Icons.Ledger style={{ color: 'var(--text-muted)' }} />}
            title="No ledgers available"
            description="No ledgers have been created yet"
          />
        ) : (
          <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
            {ledgers.map((ledgerName) => (
              <ListingItem
                key={ledgerName}
                title={ledgerName}
                icon={<Icons.Ledger className="h-6 w-6" />}
                onClick={() => handleLedgerClick(ledgerName)}
              />
            ))}
          </div>
        )}
      </ListingContainer>
    </PageContainer>
  );
}