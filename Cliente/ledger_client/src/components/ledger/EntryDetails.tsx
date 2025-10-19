'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useLedgerViewModel } from '@/viewmodels/useLedgerViewModel';
import { usePKIViewModel } from '@/viewmodels/usePKIViewModel';
import { Loading } from '@/components/ui/Loading';
import { PageContainer } from '@/components/ui/PageContainer';
import { ListingContainer } from '@/components/ui/ListingContainer';
import { ListingItem } from '@/components/ui/ListingItem';
import { EmptyState } from '@/components/ui/EmptyState';
import { InfoCard } from '@/components/ui/InfoCard';
import { Icons } from '@/components/ui/Icons';

interface EntryDetailsProps {
  onGoBack?: () => void;
}

export function EntryDetails({ onGoBack }: EntryDetailsProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const entryId = searchParams?.get('entry') || '';
  
  const { 
    currentEntry, 
    loading, 
    error, 
    keywordOperationInProgress,
    userMessage,
    loadEntry, 
    handleAddKeywords, 
    handleRemoveKeyword,
    clearUserMessage,
    retryLastOperation,
    signEntry 
  } = useLedgerViewModel();

  const {
    keyPair,
    signData,
    hasKeyPair,
    getKeyPairAlgorithm
  } = usePKIViewModel();

  const [newKeywords, setNewKeywords] = useState<string>('');
  const [signingInProgress, setSigningInProgress] = useState(false);

  // Add this after loading entry data
  useEffect(() => {
    if (currentEntry) {
      console.log('Current entry data:', currentEntry);
      console.log('Signatures:', currentEntry.signatures);
      console.log('Signatures type:', typeof currentEntry.signatures);
      console.log('Signatures length:', currentEntry.signatures?.length);
    }
  }, [currentEntry]);

  useEffect(() => {
    if (entryId) {
      loadEntry(entryId);
    }
  }, [entryId, loadEntry]);

  const handleGoBack = () => {
    if (onGoBack) {
      onGoBack();
    } else if (currentEntry?.pageNumber) {
      router.push(`/page-details?ledger=${encodeURIComponent(currentEntry.ledgerName)}&page=${currentEntry.pageNumber}`);
    } else {
      router.push(`/ledger-details?ledger=${encodeURIComponent(currentEntry?.ledgerName || '')}`);
    }
  };

  const handleTryAgain = () => {
    retryLastOperation();
    if (entryId) {
      loadEntry(entryId);
    }
  };

  const onAddKeywords = async () => {
    if (!currentEntry) return;
    
    const result = await handleAddKeywords(currentEntry.id, newKeywords);
    if (result.success) {
      setNewKeywords('');
    }
  };

  const onRemoveKeyword = (keyword: string) => {
    if (!currentEntry) return;
    handleRemoveKeyword(currentEntry.id, keyword);
  };

  const onNavigateToRelatedEntry = (relatedId: string) => {
    router.push(`/entry-details?entry=${encodeURIComponent(relatedId)}`);
  };

  const hasUserSigned = (): boolean => {
    if (!currentEntry || !keyPair) return true;
    return currentEntry.signatures.some(sig => sig.publicKey === keyPair.publicKey);
  };

  const handleSignEntry = async () => {
    if (!currentEntry || !keyPair || !hasKeyPair()) {
      console.error('Cannot sign: missing entry or key pair');
      return;
    }

    setSigningInProgress(true);
    try {
      // Sign the entry hash
      const signatureHex = await signData(currentEntry.hash);
      
      // Call the service to add the signature to the entry

      const result = await signEntry(
        currentEntry.id,
        signatureHex,
        keyPair.publicKey,
        getKeyPairAlgorithm()!!
      );

      if (!result.success) {
        throw new Error(result.message || 'Failed to sign entry');
      }
      
    } catch (err) {
      console.error('Error signing entry:', err);
      // Handle error - show error message to user
    } finally {
      setSigningInProgress(false);
    }
  };

  if (loading) {
    return <Loading title="Entry Details" message={`Loading entry: ${entryId}`} />;
  }

  if (error) {
    return (
      <PageContainer title="Entry Details" onGoBack={handleGoBack}>
        <div className="rounded-2xl p-6 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
          <h2 className="text-2xl font-bold mb-2">Error</h2>
          <p>{error}</p>
          <div className="mt-4">
            <button onClick={handleTryAgain} className="px-4 py-2 bg-white text-red-600 rounded">
              Try Again
            </button>
          </div>
        </div>
      </PageContainer>
    );
  }

  if (!currentEntry) {
    return (
      <PageContainer title="Entry Details" onGoBack={handleGoBack}>
        <EmptyState
          title="Entry not found"
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Entry Details" onGoBack={handleGoBack}>
      {/* User Message Display */}
      {userMessage && (
        <div 
          className="rounded-lg p-4 mb-4"
          style={{ 
            backgroundColor: userMessage.type === 'success' ? 'var(--success)' : 'var(--danger)', 
            color: 'white' 
          }}
        >
          <div className="flex justify-between items-center">
            <p>{userMessage.text}</p>
            <button onClick={clearUserMessage} className="ml-4 text-white hover:bg-white hover:bg-opacity-20 rounded p-1">
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      )}

      <div className="space-y-8">
        {/* Entry Info */}
        <InfoCard 
          title={`Entry ${currentEntry.id}`}
          sections={[
            {
              title: 'Basic Info',
              icon: <Icons.Entry />,
              items: [
                { label: 'Ledger', value: currentEntry.ledgerName },
                { label: 'Page', value: currentEntry.pageNumber || 'N/A' },
                { label: 'Timestamp', value: new Date(currentEntry.timestamp).toLocaleString('en-GB', { 
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
              title: 'Security',
              icon: <Icons.Shield />,
              items: [
                { label: 'Hash', value: currentEntry.hash, type: 'hash' },
                { label: 'Signatures', value: currentEntry.signatures.length }
              ]
            }
          ]}
        />

        {/* Content Section */}
        <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
          <div className="px-6 py-4 border-b-2 flex items-center space-x-3" style={{ borderColor: 'var(--border)' }}>
            <Icons.Document />
            <h2 className="text-xl font-bold" style={{ color: 'var(--text)' }}>Content</h2>
          </div>
          <div className="p-6">
            <div className="p-4 rounded-xl border" style={{ backgroundColor: 'var(--bg-light)', borderColor: 'var(--border)' }}>
              <p style={{ color: 'var(--text)', whiteSpace: 'pre-wrap' }}>{currentEntry.content}</p>
            </div>
          </div>
        </div>

        {/* Participants Section */}
        <ListingContainer
          title="Participants"
          count={currentEntry.senders.length + currentEntry.recipients.length}
          headerIcon={<Icons.Users />}
        >
          <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
            {/* Senders */}
            {currentEntry.senders.map((sender, idx) => (
              <ListingItem
                key={`sender-${idx}`}
                title={`${sender.fullName} (Sender)`}
                subtitle={sender.email}
                showChevron={false}
              />
            ))}
            {/* Recipients */}
            {currentEntry.recipients.map((recipient, idx) => (
              <ListingItem
                key={`recipient-${idx}`}
                title={`${recipient.fullName} (Recipient)`}
                subtitle={recipient.email}
                showChevron={false}
              />
            ))}
          </div>
        </ListingContainer>

        {/* Keywords Management */}
        <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
          <div className="px-6 py-4 border-b-2 flex items-center space-x-3" style={{ borderColor: 'var(--border)' }}>
            <Icons.Tag />
            <h2 className="text-xl font-bold" style={{ color: 'var(--text)' }}>
              Keywords ({currentEntry.keywords.length})
            </h2>
          </div>
          <div className="p-6">
            <div className="flex flex-wrap gap-2 mb-4">
              {currentEntry.keywords.map((keyword, idx) => (
                <span 
                  key={idx}
                  className="px-3 py-1 rounded-full text-sm flex items-center gap-2"
                  style={{ backgroundColor: 'var(--primary)', color: 'white' }}
                >
                  {keyword}
                  <button 
                    onClick={() => onRemoveKeyword(keyword)}
                    disabled={keywordOperationInProgress}
                    className="hover:bg-white hover:bg-opacity-20 rounded-full p-1 disabled:opacity-50"
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
                disabled={keywordOperationInProgress}
                className="flex-1 px-3 py-2 rounded border disabled:opacity-50"
                style={{ borderColor: 'var(--border)', backgroundColor: 'var(--bg)' }}
              />
              <button 
                onClick={onAddKeywords}
                disabled={keywordOperationInProgress || !newKeywords.trim()}
                className="px-4 py-2 rounded text-white disabled:opacity-50"
                style={{ backgroundColor: 'var(--primary)' }}
              >
                {keywordOperationInProgress ? 'Processing...' : 'Add'}
              </button>
            </div>
          </div>
        </div>

        {/* Sign Entry Section */}
        {!hasUserSigned() && hasKeyPair() && (
          <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
            <div className="px-6 py-4 border-b-2 flex items-center space-x-3" style={{ borderColor: 'var(--border)' }}>
              <Icons.Signature />
              <h2 className="text-xl font-bold" style={{ color: 'var(--text)' }}>Sign Entry</h2>
            </div>
            <div className="p-6">
              <p className="mb-4" style={{ color: 'var(--text)' }}>
                Sign this entry with your private key to confirm your participation.
              </p>
              <button
                onClick={handleSignEntry}
                disabled={signingInProgress}
                className="px-6 py-2 rounded text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ backgroundColor: 'var(--primary)' }}
              >
                {signingInProgress ? 'Signing...' : 'Sign Entry'}
              </button>
            </div>
          </div>
        )}

        {/* Show message if already signed */}
        {hasUserSigned() && (
          <div className="rounded-2xl shadow-xl border-2 overflow-hidden p-6" style={{ background: 'var(--success)', borderColor: 'var(--border)' }}>
            <div className="flex items-center space-x-2">
              <Icons.Signature />
              <span style={{ color: 'white' }}>You have already signed this entry</span>
            </div>
          </div>
        )}

        {/* Show message if no key pair */}
        {!hasKeyPair() && (
          <div className="rounded-2xl shadow-xl border-2 overflow-hidden p-6" style={{ background: 'var(--danger)', borderColor: 'var(--border)' }}>
            <div className="flex items-center space-x-2">
              <Icons.Shield />
              <span style={{ color: 'white' }}>Generate or import a key pair to sign entries</span>
            </div>
          </div>
        )}

        {/* Related Entries */}
        {currentEntry.relatedEntryIds.length > 0 && (
          <ListingContainer
            title="Related Entries"
            count={currentEntry.relatedEntryIds.length}
            headerIcon={<Icons.Link />}
          >
            <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
              {currentEntry.relatedEntryIds.map((relatedId, idx) => (
                <ListingItem
                  key={idx}
                  title={`Entry ${relatedId}`}
                  icon={<Icons.Link />}
                  onClick={() => onNavigateToRelatedEntry(relatedId)}
                />
              ))}
            </div>
          </ListingContainer>
        )}

        {/* Signatures */}
        {currentEntry.signatures.length > 0 && (
          <ListingContainer
            title="Signatures"
            count={currentEntry.signatures.length}
            headerIcon={<Icons.Signature />}
          >
            <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
              {currentEntry.signatures.map((sig, idx) => (
                <div key={idx} className="p-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <h4 className="font-medium mb-2" style={{ color: 'var(--text)' }}>{sig.participant}</h4>
                      <p className="text-sm mb-2" style={{ color: 'var(--text-muted)' }}>Email: {sig.email}</p>
                      <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Algorithm: <span style={{ color: 'var(--text)' }}>{sig.algorithm}</span></p>
                    </div>
                    <div className="space-y-3">
                      <div>
                        <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-muted)' }}>Public Key:</p>
                        <p className="text-xs font-mono p-2 rounded border" style={{ 
                          color: 'var(--text)', 
                          wordBreak: 'break-all', 
                          backgroundColor: 'var(--bg)', 
                          borderColor: 'var(--border)' 
                        }}>
                          {sig.publicKey}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-muted)' }}>Signature:</p>
                        <p className="text-xs font-mono p-2 rounded border" style={{ 
                          color: 'var(--text)', 
                          wordBreak: 'break-all', 
                          backgroundColor: 'var(--bg)', 
                          borderColor: 'var(--border)' 
                        }}>
                          {sig.signature}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </ListingContainer>
        )}
      </div>
    </PageContainer>
  );
}