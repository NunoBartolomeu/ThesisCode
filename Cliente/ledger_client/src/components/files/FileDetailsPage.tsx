'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { FileDetailsForm } from '@/components/files/FileDetailsForm';
import { useFilesViewModel } from '@/viewmodels/useFilesViewModel';
import { Loading } from '../ui/Loading';
import { PageContainer } from '../ui/PageContainer';
import { EmptyState } from '../ui/EmptyState';
import { ListingContainer } from '../ui/ListingContainer';
import { ListingItem } from '../ui/ListingItem';

export function FileDetailsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const fileId = searchParams?.get('fileId');

  const {
    selectedFile,
    loading,
    errors,
    getFileMetadata,
    downloadFile,
    deleteFile,
    clearError,
  } = useFilesViewModel();

  useEffect(() => {
    if (!fileId) {
      router.push('/files');
      return;
    }
    getFileMetadata(fileId);
  }, [fileId, getFileMetadata, router]);

  const handleDownload = async () => {
    if (!selectedFile) return;
    const result = await downloadFile(selectedFile.id);
    if (!result.success) {
      console.error('Download failed:', result.message);
    }
  };

  const handleDelete = async () => {
    if (!selectedFile) return;
    const result = await deleteFile(selectedFile.id);
    if (result.success) {
      router.push('/files');
    }
  };

  const handleBack = () => {
    router.push('/files');
  };

  const handleLedgerEntryClick = (entry: string) => {
    router.push(`/entry-details?entry=${encodeURIComponent(entry)}`);
  };

  if (loading) {
    return (
      <Loading title="File Details" message={`Loading file: ${fileId}`} />
    );
  }

  if (!selectedFile) {
    return (
      <PageContainer title="File Details" onGoBack={handleBack}>
        <EmptyState 
          title="File not found" 
          description={errors.general || 'File not found or failed to load'}
        />
        <div className="mt-4 space-x-4">
          <button 
            onClick={() => fileId && getFileMetadata(fileId)} 
            className="px-4 py-2 bg-white text-red-600 rounded"
          >
            Try Again
          </button>
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="File Details" onGoBack={handleBack}>
      <div className="space-y-8">
        <FileDetailsForm
          file={selectedFile}
          loading={loading}
          error={errors.general}
          onDownload={handleDownload}
          onDelete={handleDelete}
          onBack={handleBack}
          onLedgerEntryClick={handleLedgerEntryClick}
          onClearError={() => clearError()}
        />

        <ListingContainer title="Ledger Entries" count={selectedFile.ledgerEntries?.length || 0}>
          {selectedFile.ledgerEntries && selectedFile.ledgerEntries.length > 0 ? (
            selectedFile.ledgerEntries.map((entryId: string) => (
              <ListingItem
                key={entryId}
                title={entryId}
                subtitle="Ledger Entry"
                onClick={() => handleLedgerEntryClick(entryId)}
              />
            ))
          ) : (
            <div className="px-6 py-4 text-sm" style={{ color: 'var(--text-muted)' }}>
              No entries associated with this file.
            </div>
          )}
        </ListingContainer>
      </div>
    </PageContainer>
  );
}
