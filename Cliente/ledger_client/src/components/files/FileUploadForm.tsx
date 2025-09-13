'use client';

import { useCallback } from 'react';
import { Icons } from '@/components/ui/Icons';

interface FileUploadFormProps {
  onFileUpload: (file: File) => void;
  dragActive: boolean;
  isLoading: boolean;
  error?: string;
  onDragEnter: (e: React.DragEvent) => void;
  onDragLeave: (e: React.DragEvent) => void;
  onDragOver: (e: React.DragEvent) => void;
  onDrop: (e: React.DragEvent) => void;
  onClearError: () => void;
}

export function FileUploadForm({
  onFileUpload,
  dragActive,
  isLoading,
  error,
  onDragEnter,
  onDragLeave,
  onDragOver,
  onDrop,
  onClearError,
}: FileUploadFormProps) {
  const handleFileInput = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    if (selectedFiles.length > 0) {
      onFileUpload(selectedFiles[0]);
    }
  }, [onFileUpload]);

  return (
    <div className="space-y-4">
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

      <div
        className={`rounded-2xl p-8 border-2 text-center transition-all duration-200 ${
          dragActive ? 'scale-105' : ''
        }`}
        style={{
          backgroundColor: 'var(--bg)',
          background: dragActive ? 'var(--gradient-hover)' : 'var(--gradient)',
          borderColor: dragActive ? 'var(--primary)' : 'var(--border)',
          borderStyle: 'dashed',
        }}
        onDragEnter={onDragEnter}
        onDragLeave={onDragLeave}
        onDragOver={onDragOver}
        onDrop={onDrop}
        onMouseEnter={(e) => {
          if (!dragActive) {
            e.currentTarget.style.background = 'var(--gradient-hover)';
            e.currentTarget.style.borderColor = 'var(--primary)';
          }
        }}
        onMouseLeave={(e) => {
          if (!dragActive) {
            e.currentTarget.style.background = 'var(--gradient)';
            e.currentTarget.style.borderColor = 'var(--border)';
          }
        }}
      >
        <input
          type="file"
          id="fileInput"
          className="hidden"
          onChange={handleFileInput}
          multiple
          disabled={isLoading}
        />
        <label htmlFor="fileInput" className="cursor-pointer">
          <div>
            <div className="mx-auto h-16 w-16 mb-4 flex items-center justify-center">
              <Icons.Document className="h-16 w-16" style={{ color: 'var(--primary)' }} />
            </div>
            <p className="text-2xl font-semibold mb-2" style={{ color: 'var(--text)' }}>
              {isLoading ? 'Uploading...' : 'Drop files here or click to upload'}
            </p>
            <p style={{ color: 'var(--text-muted)' }}>
              Drag and drop files or browse from your computer
            </p>
          </div>
        </label>
        {isLoading && (
          <div className="mt-4">
            <div
              className="animate-spin rounded-full h-8 w-8 border-b-2 mx-auto"
              style={{ borderColor: 'var(--primary)' }}
            ></div>
          </div>
        )}
      </div>
    </div>
  );
}