'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useLedgerViewModel } from '@/viewmodels/useLedgerViewModel';
import { Loading } from '@/components/ui/Loading';
import { PageContainer } from '@/components/ui/PageContainer';
import { ListingContainer } from '@/components/ui/ListingContainer';
import { ListingItem } from '@/components/ui/ListingItem';
import { EmptyState } from '@/components/ui/EmptyState';
import { InfoCard } from '@/components/ui/InfoCard';
import { Icons } from '@/components/ui/Icons';

interface PageDetailsProps {
  onGoBack?: () => void;
}

export function PageDetails({ onGoBack }: PageDetailsProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const ledgerName = searchParams?.get('ledger') || '';
  const pageNumber = parseInt(searchParams?.get('page') || '0');
  
  const { currentPage, loading, error, loadPage, retryLastOperation } = useLedgerViewModel();

  useEffect(() => {
    if (ledgerName && !isNaN(pageNumber)) {
      loadPage(ledgerName, pageNumber);
    }
  }, [ledgerName, pageNumber, loadPage]);

  const handleGoBack = () => {
    if (onGoBack) {
      onGoBack();
    } else {
      router.push(`/ledger-details?ledger=${encodeURIComponent(ledgerName)}`);
    }
  };

  const handleTryAgain = () => {
    retryLastOperation();
    if (ledgerName && !isNaN(pageNumber)) {
      loadPage(ledgerName, pageNumber);
    }
  };

  const onNavigateToEntry = (entryId: string) => {
    router.push(`/entry-details?entry=${encodeURIComponent(entryId)}`);
  };

  if (loading) {
    return <Loading title="Page Details" message={`Loading page ${pageNumber} from ledger: ${ledgerName}`} />;
  }

  if (error) {
    return (
      <PageContainer title="Page Details" onGoBack={handleGoBack}>
        <div className="rounded-2xl p-6 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
          <h2 className="text-2xl font-bold mb-2">Error</h2>
          <p>{error}</p>
          <div className="mt-4 space-x-4">
            <button onClick={handleTryAgain} className="px-4 py-2 bg-white text-red-600 rounded">
              Try Again
            </button>
          </div>
        </div>
      </PageContainer>
    );
  }

  if (!currentPage) {
    return (
      <PageContainer title="Page Details" onGoBack={handleGoBack}>
        <EmptyState
          title="Page not found"
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Page Details" onGoBack={handleGoBack}>
      <div className="space-y-8">
        {/* Page Info */}
        <InfoCard 
          title={`${currentPage.ledgerName} - Page ${currentPage.number}`}
          sections={[
            {
              title: 'Page Info',
              icon: <Icons.Stats />,
              items: [
                { label: 'Page number', value: currentPage.number },
                { label: 'Entry count', value: currentPage.entryCount },
                { label: 'Timestamp', value: new Date(currentPage.timestamp).toLocaleString('en-GB', { 
                    day: '2-digit', 
                    month: '2-digit', 
                    year: 'numeric', 
                    hour: '2-digit', 
                    minute: '2-digit', 
                    second: '2-digit', 
                    hour12: false 
                  }) 
                }
              ]
            },
            {
              title: 'Hashes',
              icon: <Icons.Hash />,
              items: [
                { label: 'Page hash', value: currentPage.hash, type: 'hash' },
                { label: 'Merkle root', value: currentPage.merkleRoot, type: 'hash' },
                ...(currentPage.previousHash ? [{ label: 'Previous hash', value: currentPage.previousHash, type: 'hash' as const }] : [])
              ]
            }
          ]}
        />

        {/* Entries Section */}
        <ListingContainer 
          title="Entries" 
          count={currentPage.entries.length}
          headerIcon={<Icons.Entry />}
        >
          {currentPage.entries.length === 0 ? (
            <EmptyState
              icon={<Icons.Entry style={{ color: 'var(--text-muted)' }} />}
              title="No entries on this page"
            />
          ) : (
            <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
              {currentPage.entries.map((entry) => (
                <ListingItem
                  key={entry.id}
                  title={`Entry ${entry.id}`}
                  subtitle={entry.isParticipant ? 'You are a participant' : 'Not a participant'}
                  icon={<Icons.Entry />}
                  onClick={() => onNavigateToEntry(entry.id)}
                />
              ))}
            </div>
          )}
        </ListingContainer>
      </div>
    </PageContainer>
  );
}