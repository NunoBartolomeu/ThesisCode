'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Layout } from '@/components/Layout';
import { LedgerService } from '@/viewmodels/LedgerService';
import { PageDTO } from '@/dto/ledger_dto';

interface LoadingState {
  isLoading: boolean;
  error: string | null;
  data: PageDTO | null;
}

export default function PageDetailsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const ledgerName = searchParams?.get('ledger');
  const pageNumber = searchParams?.get('page');

  const [state, setState] = useState<LoadingState>({
    isLoading: true,
    error: null,
    data: null,
  });

  const ledgerService = new LedgerService();

  useEffect(() => {
    if (!ledgerName || !pageNumber) {
      setState({ isLoading: false, error: 'Missing ledger name or page number', data: null });
      return;
    }
    fetchPageDetails(ledgerName, parseInt(pageNumber));
  }, [ledgerName, pageNumber]);

  const fetchPageDetails = async (ledgerName: string, pageNumber: number) => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await ledgerService.getPage(ledgerName, pageNumber);

      if (response.success && response.data) {
        setState({ isLoading: false, error: null, data: response.data });
        console.log('Raw timestamp:', response.data.timestamp);
      } else {
        setState({ isLoading: false, error: 'Failed to fetch page details', data: null });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState({ isLoading: false, error: errorMessage, data: null });
    }
  };

  const handleGoBack = () => router.push(`/ledger-details?ledger=${encodeURIComponent(ledgerName || '')}`);

  const handleTryAgain = () => {
    if (ledgerName && pageNumber) fetchPageDetails(ledgerName, parseInt(pageNumber));
  };

  const getEntryIcon = () => {
    return (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--success)' }}>
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
    );
  };

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-6xl space-y-8">
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            Page Details
          </h1>

          {state.isLoading && (
            <div className="p-12 text-center rounded-2xl border-2" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
              <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
              <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                Loading page {pageNumber} from ledger: <strong>{ledgerName}</strong>
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
                  <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
                    {state.data.ledgerName} - Page {state.data.number}
                  </h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6">
                  <div>
                    <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>📊 Page Info</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Page number: <span style={{ color: 'var(--text)' }}>{state.data.number}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Entry count: <span style={{ color: 'var(--text)' }}>{state.data.entryCount}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Timestamp: <span style={{ color: 'var(--text)' }}>{new Date(state.data.timestamp).toLocaleString()}</span></p>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>🔗 Hashes</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Page hash: <span style={{ color: 'var(--text)', fontSize: '0.8rem', wordBreak: 'break-all' }}>{state.data.hash}</span></p>
                    <p style={{ color: 'var(--text-muted)' }}>Merkle root: <span style={{ color: 'var(--text)', fontSize: '0.8rem', wordBreak: 'break-all' }}>{state.data.merkleRoot}</span></p>
                    {state.data.previousHash && (
                      <p style={{ color: 'var(--text-muted)' }}>Previous hash: <span style={{ color: 'var(--text)', fontSize: '0.8rem', wordBreak: 'break-all' }}>{state.data.previousHash}</span></p>
                    )}
                  </div>
                </div>

                <div className="px-6 pb-6">
                  <button onClick={handleGoBack} className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition">
                    📚 Back to Ledger
                  </button>
                </div>
              </div>

              <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
                <div className="px-6 py-4 border-b-2" style={{ borderColor: 'var(--border)' }}>
                  <h3 className="text-xl font-bold" style={{ color: 'var(--text)' }}>Entries ({state.data.entries.length})</h3>
                </div>
                
                {state.data.entries.length === 0 ? (
                  <div className="p-8 text-center">
                    <p className="text-lg" style={{ color: 'var(--text-muted)' }}>No entries on this page</p>
                  </div>
                ) : (
                  <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
                    {state.data.entries.map((entry) => (
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
                            {getEntryIcon()}
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
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}