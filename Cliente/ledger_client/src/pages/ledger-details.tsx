'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Layout } from '@/components/Layout';
import { LedgerService } from '@/viewmodels/LedgerService';
import { LedgerDTO, PageSummaryDTO, PageEntryDTO } from '@/dto/ledger_dto';

interface LoadingState {
  isLoading: boolean;
  error: string | null;
  data: LedgerDTO | null;
}

export default function LedgerDetailsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const ledgerName = searchParams?.get('ledger');

  const [state, setState] = useState<LoadingState>({
    isLoading: true,
    error: null,
    data: null,
  });

  const ledgerService = new LedgerService();

  useEffect(() => {
    if (!ledgerName) {
      setState({ isLoading: false, error: 'No ledger name provided', data: null });
      return;
    }
    fetchLedgerDetails(ledgerName);
  }, [ledgerName]);

  const fetchLedgerDetails = async (ledgerName: string) => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await ledgerService.getLedger(ledgerName);

      if (response.success && response.data) {
        setState({ isLoading: false, error: null, data: response.data });
      } else {
        setState({ isLoading: false, error: 'Failed to fetch ledger details', data: null });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState({ isLoading: false, error: errorMessage, data: null });
    }
  };

  const handleGoBack = () => router.push('/ledgers');

  const handleTryAgain = () => {
    if (ledgerName) fetchLedgerDetails(ledgerName);
  };

  const getPageIcon = () => {
    return (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--primary)' }}>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    );
  };

  const getVerifiedEntryIcon = () => {
    return (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--success)' }}>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
  };

  const getUnverifiedEntryIcon = () => {
    return (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--warning)' }}>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
      </svg>
    );
  };

  const renderEntrySection = (entries: PageEntryDTO[], title: string, icon: () => JSX.Element, emptyMessage: string) => {
    if (entries.length === 0) return null;

    return (
      <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
        <div className="px-6 py-4 border-b-2" style={{ borderColor: 'var(--border)' }}>
          <h3 className="text-xl font-bold" style={{ color: 'var(--text)' }}>{title} ({entries.length})</h3>
        </div>
        <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
          {entries.map((entry) => (
            <div 
              key={entry.id}
              className="px-6 py-4 transition-all duration-200 cursor-pointer"
              style={{ backgroundColor: 'transparent' }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--gradient-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
              }}
              onClick={() => router.push(`/entry-details?entry=${encodeURIComponent(entry.id)}`)}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  {icon()}
                  <div>
                    <p className="font-medium" style={{ color: 'var(--text)' }}>Entry {entry.id}</p>
                    <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                      {entry.isParticipant ? 'You are a participant' : 'Not a participant'}
                    </p>
                  </div>
                </div>
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-6xl space-y-8">
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            Ledger Details
          </h1>

          {state.isLoading && (
            <div className="p-12 text-center rounded-2xl border-2" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
              <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
              <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                Loading details for: <strong>{ledgerName}</strong>
              </p>
            </div>
          )}

          {state.error && !state.isLoading && (
            <div className="rounded-2xl p-6 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
              <h2 className="text-2xl font-bold mb-2">Error</h2>
              <p>{state.error}</p>
              <div className="mt-4 space-x-4">
                <button onClick={handleTryAgain} className="px-4 py-2 bg-white text-red-600 rounded">
                  Try Again
                </button>
                <button onClick={handleGoBack} className="px-4 py-2 bg-white text-gray-700 rounded">
                  Back
                </button>
              </div>
            </div>
          )}

          {state.data && !state.isLoading && !state.error && (
            <div className="space-y-8">
              <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
                <div className="px-6 py-6 border-b-2" style={{ borderColor: 'var(--border)' }}>
                  <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>{state.data.name}</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6">
                  <div>
                    <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>⚙️ Configuration</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Entries per page: <span style={{ color: 'var(--text)' }}>{state.data.entriesPerPage}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Hash algorithm: <span style={{ color: 'var(--text)' }}>{state.data.hashAlgorithm}</span></p>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>📊 Statistics</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Total pages: <span style={{ color: 'var(--text)' }}>{state.data.pages.length}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Verified entries: <span style={{ color: 'var(--text)' }}>{state.data.verifiedEntries.length}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Unverified entries: <span style={{ color: 'var(--text)' }}>{state.data.unverifiedEntries.length}</span></p>
                  </div>
                </div>

                <div className="px-6 pb-6">
                  <button onClick={handleGoBack} className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition">
                    📚 Back to Ledgers
                  </button>
                </div>
              </div>

              {renderEntrySection(
                state.data.unverifiedEntries,
                "⏳ Unerified Entries",
                getUnverifiedEntryIcon,
                "No unverified entries"
              )}

              {renderEntrySection(
                state.data.verifiedEntries,
                "✅ Verified Entries",
                getVerifiedEntryIcon,
                "No verified entries"
              )}

              <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
                <div className="px-6 py-4 border-b-2" style={{ borderColor: 'var(--border)' }}>
                  <h3 className="text-xl font-bold" style={{ color: 'var(--text)' }}>Pages ({state.data.pages.length})</h3>
                </div>
                
                {state.data.pages.length === 0 ? (
                  <div className="p-8 text-center">
                    <p className="text-lg" style={{ color: 'var(--text-muted)' }}>No pages available</p>
                  </div>
                ) : (
                  <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
                    {state.data.pages.map((page) => (
                      <div 
                        key={page.number}
                        className="px-6 py-4 transition-all duration-200 cursor-pointer"
                        style={{ backgroundColor: 'transparent' }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--gradient-hover)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'transparent';
                        }}
                        onClick={() => router.push(`/page-details?ledger=${encodeURIComponent(state.data!.name)}&page=${page.number}`)}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            {getPageIcon()}
                            <div>
                              <p className="text-lg font-semibold" style={{ color: 'var(--text)' }}>Page {page.number}</p>
                              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                                {page.entryCount} entries • {new Date(page.timestamp * 1000).toLocaleDateString()}
                              </p>
                            </div>
                          </div>
                          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                          </svg>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}