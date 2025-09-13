'use client';

import { FileMetadataData } from '@/models/files';
import { InfoCard } from '@/components/ui/InfoCard';
import { Button } from '@/components/ui/Button';
import { Icons } from '@/components/ui/Icons';

interface FileDetailsFormProps {
  file: FileMetadataData;
  loading: boolean;
  error?: string;
  onDownload: () => void;
  onDelete: () => void;
  onBack: () => void;
  onLedgerEntryClick: (entry: string) => void;
  onClearError: () => void;
}

export function FileDetailsForm({
  file,
  loading,
  error,
  onDownload,
  onDelete,
  onBack,
  onLedgerEntryClick,
  onClearError,
}: FileDetailsFormProps) {
  const handleDelete = async () => {
    if (!confirm(`Are you sure you want to delete "${file.name}"? This action cannot be undone.`)) {
      return;
    }
    onDelete();
  };

  const fileInfoSection = {
    title: 'File Information',
    icon: <Icons.Document className="h-5 w-5" />,
    items: [
      { label: 'Size', value: file.formattedSize },
      { label: 'Type', value: file.fileType },
      { label: 'Content Type', value: file.contentType || 'Unknown' },
    ],
  };

  const ownershipSection = {
    title: 'Ownership',
    icon: <Icons.Users className="h-5 w-5" />,
    items: [
      { label: 'Uploader ID', value: file.uploaderId },
    ],
  };

  const participantsSection = {
    title: 'Participants',
    icon: <Icons.Shield className="h-5 w-5" />,
    items: [
      { label: `Senders (${file.senders.length})`, value: file.senders.length > 0 ? file.senders.join(', ') : 'None' },
      { label: `Receivers (${file.receivers.length})`, value: file.receivers.length > 0 ? file.receivers.join(', ') : 'None' },
    ],
  };

  return (
    <div className="space-y-6">
      {error && (
        <div
          className="rounded-2xl p-4 border-2"
          style={{
            backgroundColor: 'var(--danger)',
            color: 'white',
            borderColor: 'var(--danger)',
          }}
        >
          <p>{error}</p>
          <button
            onClick={onClearError}
            className="mt-2 px-3 py-1 bg-white text-red-600 rounded"
          >
            Dismiss
          </button>
        </div>
      )}

      <InfoCard
        title={file.name}
        sections={[fileInfoSection, ownershipSection, participantsSection]}
      />

      {file.ledgerEntries.length > 0 && (
        <div
          className="rounded-2xl shadow-xl border-2 overflow-hidden"
          style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}
        >
          <div className="px-6 py-4 border-b-2" style={{ borderColor: 'var(--border)' }}>
            <div className="flex items-center space-x-2">
              <Icons.Ledger className="h-5 w-5" />
              <h3 className="text-lg font-semibold" style={{ color: 'var(--text)' }}>
                Ledger Entries ({file.ledgerEntries.length})
              </h3>
            </div>
          </div>
          <div className="p-4">
            <div className="bg-gray-50 rounded p-3 max-h-32 overflow-y-auto">
              {file.ledgerEntries.map((entry, index) => (
                <button
                  key={index}
                  onClick={() => onLedgerEntryClick(entry)}
                  className="block text-left w-full px-2 py-1 text-sm text-blue-600 hover:underline hover:bg-blue-100 rounded"
                >
                  {entry}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="flex gap-4">
        <Button
          onClick={onDownload}
          disabled={loading}
          className="bg-green-600 text-white border-green-600 hover:bg-green-700"
        >
          <Icons.Document className="h-4 w-4 mr-2" />
          Download
        </Button>

        <Button
          onClick={onBack}
          disabled={loading}
          className="bg-gray-200 text-gray-800 border-gray-300 hover:bg-gray-300"
        >
          <Icons.Link className="h-4 w-4 mr-2" />
          Back to Files
        </Button>
        <Button
          onClick={handleDelete}
          disabled={loading}
          className="bg-red-600 text-white border-red-600 hover:bg-red-700"
        >
          <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          Delete
        </Button>
      </div>
    </div>
  );
}