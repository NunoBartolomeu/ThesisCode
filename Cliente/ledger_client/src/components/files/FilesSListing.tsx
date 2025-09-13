'use client';

import { FileListItem } from '@/models/files';
import { ListingContainer } from '@/components/ui/ListingContainer';
import { Icons } from '@/components/ui/Icons';
import { Button } from '@/components/ui/Button';

interface FilesListingProps {
  files: FileListItem[];
  loading: boolean;
  onRefresh: () => void;
  onFileClick: (fileId: string) => void;
  onDeleteClick: (fileId: string) => void;
  error?: string;
  onClearError: () => void;
}

export function FilesListing({ 
  files, 
  loading, 
  onRefresh, 
  onFileClick, 
  onDeleteClick, 
  error,
  onClearError 
}: FilesListingProps) {
  const getFileIcon = (fileType: string) => {
    switch (fileType) {
      case 'image':
        return <Icons.Page className="h-8 w-8" style={{ color: 'var(--primary)' }} />;
      case 'document':
        return <Icons.Document className="h-8 w-8" style={{ color: 'var(--danger)' }} />;
      case 'spreadsheet':
        return <Icons.Stats className="h-8 w-8" style={{ color: 'var(--success)' }} />;
      default:
        return <Icons.Document className="h-8 w-8" style={{ color: 'var(--text-muted)' }} />;
    }
  };

  const handleDeleteClick = async (file: FileListItem) => {
    if (!confirm(`Are you sure you want to delete "${file.name}"? This action cannot be undone.`)) {
      return;
    }
    onDeleteClick(file.id);
  };

  return (
    <>
      {error && (
        <div
          className="rounded-2xl p-4 mb-4 border-2"
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

      <ListingContainer
        title="Your Files"
        count={files.length}
        headerIcon={<Icons.Document className="h-6 w-6" style={{ color: 'var(--primary)' }} />}
        actions={
          <Button
            onClick={onRefresh}
            disabled={loading}
          >
            {loading ? 'Loading...' : 'Refresh'}
          </Button>
        }
      >
        {loading ? (
          <div className="p-12 text-center">
            <div
              className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4"
              style={{ borderColor: 'var(--primary)' }}
            />
            <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
              Loading files...
            </p>
          </div>
        ) : files.length === 0 ? (
          <div className="p-12 text-center">
            <Icons.Document className="mx-auto h-16 w-16 mb-4" style={{ color: 'var(--text-muted)' }} />
            <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
              No files yet
            </p>
            <p style={{ color: 'var(--text-muted)' }}>
              Upload your first file to get started
            </p>
          </div>
        ) : (
          <div className="divide-y-2" style={{ borderColor: 'var(--border)' }}>
            {files.map((file) => (
              <FileRow
                key={file.id}
                file={file}
                onFileClick={onFileClick}
                onDeleteClick={() => handleDeleteClick(file)}
                getFileIcon={getFileIcon}
              />
            ))}
          </div>
        )}
      </ListingContainer>
    </>
  );
}

interface FileRowProps {
  file: FileListItem;
  onFileClick: (fileId: string) => void;
  onDeleteClick: () => void;
  getFileIcon: (fileType: string) => React.ReactNode;
}

const FileRow = ({ file, onFileClick, onDeleteClick, getFileIcon }: FileRowProps) => {
  return (
    <div
      className="px-8 py-6 transition-all duration-200"
      style={{ backgroundColor: 'transparent' }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = 'var(--gradient-hover)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'transparent';
      }}
    >
      <div className="flex items-center justify-between">
        <div
          className="flex items-center space-x-4 flex-1 min-w-0 cursor-pointer"
          onClick={() => onFileClick(file.id)}
        >
          <div className="flex-shrink-0">
            {getFileIcon(file.fileType)}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold truncate" style={{ color: 'var(--text)' }}>
              {file.name}
            </p>
            <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
              {file.formattedSize} • {file.fileType}
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDeleteClick();
            }}
            className="p-2 rounded hover:bg-red-100 transition-colors duration-200"
            style={{ color: 'var(--danger)' }}
            title="Delete file"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
          </button>
          <span className="text-xl font-bold" style={{ color: 'var(--text-muted)' }}>
            &gt;
          </span>
        </div>
      </div>
    </div>
  );
};