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
import { PageEntryDTO } from '@/models/ledger_dto';

interface LedgerDetailsProps {
  onGoBack?: () => void;
}

export function LedgerDetails({ onGoBack }: LedgerDetailsProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const ledgerName = searchParams?.get('ledger') || '';
  
  const { currentLedger, loading, error, loadLedger, retryLastOperation } = useLedgerViewModel();

  useEffect(() => {
    if (ledgerName) {
      loadLedger(ledgerName);
    }
  }, [ledgerName, loadLedger]);

  const handleGoBack = () => {
    if (onGoBack) {
      onGoBack();
    } else {
      router.push('/ledgers');
    }
  };

  const handleTryAgain = () => {
    retryLastOperation();
    if (ledgerName) {
      loadLedger(ledgerName);
    }
  };

  const onNavigateToEntry = (entryId: string) => {
    router.push(`/entry-details?entry=${encodeURIComponent(entryId)}`);
  };

  const onNavigateToPage = (pageNumber: number) => {
    router.push(`/page-details?ledger=${encodeURIComponent(ledgerName)}&page=${pageNumber}`);
  };

  const renderEntrySection = (entries: PageEntryDTO[], title: string, icon: React.ReactNode) => {
    if (entries.length === 0) return null;

    return (
      <ListingContainer title={title} count={entries.length} headerIcon={icon}>
        <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
          {entries.map((entry) => (
            <ListingItem
              key={entry.id}
              title={`Entry ${entry.id}`}
              subtitle={entry.isParticipant ? 'You are a participant' : 'Not a participant'}
              icon={icon}
              onClick={() => onNavigateToEntry(entry.id)}
            />
          ))}
        </div>
      </ListingContainer>
    );
  };

  if (loading) {
    return <Loading title="Ledger Details" message={`Loading details for: ${ledgerName}`} />;
  }

  if (error) {
    return (
      <PageContainer title="Ledger Details" onGoBack={handleGoBack}>
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

  if (!currentLedger) {
    return (
      <PageContainer title="Ledger Details" onGoBack={handleGoBack}>
        <EmptyState
          title="Ledger not found"
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Ledger Details" onGoBack={handleGoBack}>
      <div className="space-y-8">
        {/* Ledger Info */}
        <InfoCard 
          title={currentLedger.name}
          sections={[
            {
              title: 'Configuration',
              icon: <Icons.Settings />,
              items: [
                { label: 'Entries per page', value: currentLedger.entriesPerPage },
                { label: 'Hash algorithm', value: currentLedger.hashAlgorithm }
              ]
            },
            {
              title: 'Statistics', 
              icon: <Icons.Stats />,
              items: [
                { label: 'Total pages', value: currentLedger.pages.length },
                { label: 'Verified entries', value: currentLedger.verifiedEntries.length },
                { label: 'Unverified entries', value: currentLedger.unverifiedEntries.length }
              ]
            }
          ]}
        />

        {/* Entry Sections */}
        {renderEntrySection(
          currentLedger.unverifiedEntries,
          "Unverified Entries",
          <Icons.Clock />
        )}

        {renderEntrySection(
          currentLedger.verifiedEntries,
          "Verified Entries",
          <Icons.VerifiedEntry />
        )}

        {/* Pages Section */}
        <ListingContainer 
          title="Pages" 
          count={currentLedger.pages.length}
          headerIcon={<Icons.Page />}
        >
          {currentLedger.pages.length === 0 ? (
            <EmptyState
              icon={<Icons.Page style={{ color: 'var(--text-muted)' }} />}
              title="No pages available"
            />
          ) : (
            <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
              {currentLedger.pages.map((page) => (
                <ListingItem
                  key={page.number}
                  title={`Page ${page.number}`}
                  subtitle={`${page.entryCount} entries • ${new Date(page.timestamp * 1000).toLocaleDateString()}`}
                  icon={<Icons.Page />}
                  onClick={() => onNavigateToPage(page.number)}
                />
              ))}
            </div>
          )}
        </ListingContainer>
      </div>
    </PageContainer>
  );
}