'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { FilesService } from '@/viewmodels/FilesService';
import { FileDetailsData } from '@/types/files';

interface LoadingState {
  isLoading: boolean;
  error: string | null;
  data: FileDetailsData | null;
}

export default function FileDetailsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const fileName = searchParams?.get('file');

  const [state, setState] = useState<LoadingState>({
    isLoading: true,
    error: null,
    data: null,
  });

  const filesService = new FilesService();

  useEffect(() => {
    if (!fileName) {
      setState({ isLoading: false, error: 'No file name provided', data: null });
      return;
    }
    fetchFileDetails(fileName);
  }, [fileName]);

  const fetchFileDetails = async (fileName: string) => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await filesService.getFileDetails(fileName);

      if (response.success && response.data) {
        setState({ isLoading: false, error: null, data: response.data });
      } else {
        setState({ isLoading: false, error: 'Failed to fetch file details', data: null });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState({ isLoading: false, error: errorMessage, data: null });
    }
  };

  const handleGoBack = () => router.push('/files');

  const handleTryAgain = () => {
    if (fileName) fetchFileDetails(fileName);
  };

  const handleDownload = async () => {
    if (!state.data) return;
    try {
      await filesService.downloadFile(state.data.name);
    } catch {
      alert('Download failed. Please try again.');
    }
  };

  const handleDelete = async () => {
    if (!state.data) return;
    if (!confirm(`Are you sure you want to delete "${state.data.name}"? This action cannot be undone.`)) return;

    try {
      const response = await filesService.deleteFile(state.data.name);
      if (response.success) {
        router.push('/files');
      } else {
        alert(`Delete failed: ${response.message || 'Unknown error'}`);
      }
    } catch {
      alert('Delete failed. Please try again.');
    }
  };

  return (
    <div className="flex items-center justify-center p-1 pb-20">
      <div className="w-4/5 max-w-6xl space-y-8">
        <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
          File Details
        </h1>

        {state.isLoading && (
          <div className="p-12 text-center rounded-2xl border-2" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
            <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
              Loading details for: <strong>{fileName}</strong>
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
          <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
            <div className="flex items-center space-x-4 p-6 border-b-2" style={{ borderColor: 'var(--border)' }}>
              <div className="text-4xl">{state.data.icon}</div>
              <div>
                <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>{state.data.name}</h2>
                <p style={{ color: 'var(--text-muted)' }}>{state.data.fileType} file</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6">
              <div>
                <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>📊 File Info</h3>
                <p style={{ color: 'var(--text-muted)' }}>Size: <span style={{ color: 'var(--text)' }}>{state.data.formattedSize}</span></p>
                <p style={{ color: 'var(--text-muted)' }}>Type: <span style={{ color: 'var(--text)' }}>{state.data.fileType}</span></p>
              </div>
              <div>
                <h3 className="text-lg font-semibold border-b pb-2 mb-3" style={{ color: 'var(--text)' }}>📅 Dates</h3>
                <p style={{ color: 'var(--text-muted)' }}>Last Modified: <span style={{ color: 'var(--text)' }}>{state.data.formattedDate}</span></p>
                <p style={{ color: 'var(--text-muted)' }}>Timestamp: <span style={{ color: 'var(--text)' }}>{state.data.lastModified.toISOString()}</span></p>
              </div>
            </div>

            <div className="px-6 pb-6 flex gap-4">
              <button onClick={handleDownload} className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition">
                💾 Download
              </button>
              <button onClick={handleGoBack} className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition">
                📁 Back
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
              >
                🗑️ Delete
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
