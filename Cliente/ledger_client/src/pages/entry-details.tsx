'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Layout } from '@/components/Layout';
import { LedgerService } from '@/viewmodels/LedgerService';
import { EntryDTO } from '@/dto/ledger_dto';

interface LoadingState {
  isLoading: boolean;
  error: string | null;
  data: EntryDTO | null;
}

export default function EntryDetailsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const entryId = searchParams?.get('entry');

  const [state, setState] = useState<LoadingState>({
    isLoading: true,
    error: null,
    data: null,
  });

  const [newKeywords, setNewKeywords] = useState<string>('');
  const [isAddingKeywords, setIsAddingKeywords] = useState(false);

  const ledgerService = new LedgerService();

  useEffect(() => {
    if (!entryId) {
      setState({ isLoading: false, error: 'No entry ID provided', data: null });
      return;
    }
    fetchEntryDetails(entryId);
  }, [entryId]);

  const fetchEntryDetails = async (entryId: string) => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await ledgerService.getEntry(entryId);

      if (response.success && response.data) {
        setState({ isLoading: false, error: null, data: response.data });
      } else {
        setState({ isLoading: false, error: 'Failed to fetch entry details', data: null });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState({ isLoading: false, error: errorMessage, data: null });
    }
  };

  const handleGoBack = () => {
    if (state.data?.pageNumber) {
      router.push(`/page-details?ledger=${encodeURIComponent(state.data.ledgerName)}&page=${state.data.pageNumber}`);
    } else {
      router.push(`/ledger-details?ledger=${encodeURIComponent(state.data?.ledgerName || '')}`);
    }
  };

  const handleTryAgain = () => {
    if (entryId) fetchEntryDetails(entryId);
  };

  const handleAddKeywords = async () => {
    if (!state.data || !newKeywords.trim()) return;

    try {
      setIsAddingKeywords(true);
      const keywords = newKeywords.split(',').map(k => k.trim()).filter(k => k);
      const response = await ledgerService.addKeywords(state.data.id, keywords);

      if (response.success) {
        setNewKeywords('');
        await fetchEntryDetails(state.data.id);
      } else {
        alert('Failed to add keywords');
      }
    } catch (error) {
      alert('Error adding keywords');
    } finally {
      setIsAddingKeywords(false);
    }
  };

  const handleRemoveKeyword = async (keyword: string) => {
    if (!state.data) return;

    try {
      const response = await ledgerService.removeKeyword(state.data.id, keyword);
      if (response.success) {
        await fetchEntryDetails(state.data.id);
      } else {
        alert('Failed to remove keyword');
      }
    } catch (error) {
      alert('Error removing keyword');
    }
  };

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-6xl space-y-8">
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            Entry Details
          </h1>

          {state.isLoading && (
            <div className="p-12 text-center rounded-2xl border-2" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
              <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
              <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                Loading entry: <strong>{entryId}</strong>
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
                  <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>Entry {state.data.id}</h2>
                  <p style={{ color: 'var(--text-muted)' }}>
                    {state.data.ledgerName} • Page {state.data.pageNumber} • {new Date(state.data.timestamp * 1000).toLocaleString()}
                  </p>
                </div>

                <div className="p-6">
                  <div className="mb-6">
                    <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>📄 Content</h3>
                    <div className="p-4 rounded-xl border" style={{ backgroundColor: 'var(--bg-light)', borderColor: 'var(--border)' }}>
                      <p style={{ color: 'var(--text)', whiteSpace: 'pre-wrap' }}>{state.data.content}</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    <div>
                      <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>👥 Participants</h3>
                      <div className="space-y-2">
                        <div>
                          <h4 className="font-medium text-sm" style={{ color: 'var(--text-muted)' }}>Senders:</h4>
                          {state.data.senders.map((sender, idx) => (
                            <p key={idx} className="text-sm" style={{ color: 'var(--text)' }}>
                              {sender.fullName} ({sender.email})
                            </p>
                          ))}
                        </div>
                        <div>
                          <h4 className="font-medium text-sm" style={{ color: 'var(--text-muted)' }}>Recipients:</h4>
                          {state.data.recipients.map((recipient, idx) => (
                            <p key={idx} className="text-sm" style={{ color: 'var(--text)' }}>
                              {recipient.fullName} ({recipient.email})
                            </p>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>🔐 Security</h3>
                      <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Hash: <span style={{ color: 'var(--text)', fontSize: '0.7rem', wordBreak: 'break-all' }}>{state.data.hash}</span></p>
                      <p className="text-sm mt-2" style={{ color: 'var(--text-muted)' }}>Signatures: <span style={{ color: 'var(--text)' }}>{state.data.signatures.length}</span></p>
                    </div>
                  </div>

                  <div className="mb-6">
                    <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>🏷️ Keywords</h3>
                    <div className="flex flex-wrap gap-2 mb-3">
                      {state.data.keywords.map((keyword, idx) => (
                        <span 
                          key={idx}
                          className="px-3 py-1 rounded-full text-sm flex items-center gap-2"
                          style={{ backgroundColor: 'var(--primary)', color: 'white' }}
                        >
                          {keyword}
                          <button 
                            onClick={() => handleRemoveKeyword(keyword)}
                            className="hover:bg-white hover:bg-opacity-20 rounded-full p-1"
                          >
                            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        </span>
                      ))}
                    </div>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={newKeywords}
                        onChange={(e) => setNewKeywords(e.target.value)}
                        placeholder="Add keywords (comma-separated)"
                        className="flex-1 px-3 py-2 rounded border"
                        style={{ borderColor: 'var(--border)', backgroundColor: 'var(--bg)' }}
                      />
                      <button 
                        onClick={handleAddKeywords}
                        disabled={isAddingKeywords || !newKeywords.trim()}
                        className="px-4 py-2 rounded text-white"
                        style={{ 
                          backgroundColor: 'var(--primary)',
                          opacity: (isAddingKeywords || !newKeywords.trim()) ? 0.6 : 1 
                        }}
                      >
                        {isAddingKeywords ? 'Adding...' : 'Add'}
                      </button>
                    </div>
                  </div>

                  {state.data.relatedEntryIds.length > 0 && (
                    <div className="mb-6">
                      <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>🔗 Related Entries</h3>
                      <div className="space-y-2">
                        {state.data.relatedEntryIds.map((relatedId, idx) => (
                          <button
                            key={idx}
                            onClick={() => router.push(`/entry-details?entry=${encodeURIComponent(relatedId)}`)}
                            className="block text-sm hover:underline"
                            style={{ color: 'var(--primary)' }}
                          >
                            Entry {relatedId}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {state.data.signatures.length > 0 && (
                    <div className="mb-6">
                      <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>✍️ Signatures</h3>
                      <div className="space-y-3">
                        {state.data.signatures.map((sig, idx) => (
                          <div key={idx} className="p-3 rounded border" style={{ borderColor: 'var(--border)', backgroundColor: 'var(--bg-light)' }}>
                            <p className="font-medium text-sm" style={{ color: 'var(--text)' }}>{sig.participant}</p>
                            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>Email: {sig.email}</p>
                            <p className="text-xs mt-1" style={{ color: 'var(--text-muted)', wordBreak: 'break-all' }}>
                              Signature: {sig.signature.substring(0, 50)}...
                            </p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <div>
                    <button onClick={handleGoBack} className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition">
                      📄 Back to Page
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}