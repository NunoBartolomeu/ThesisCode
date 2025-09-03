import '../app/globals.css'

import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { Layout } from '@/components/Layout';
import { FilesService } from '@/viewmodels/FilesService';
import { FileListItem } from '@/types/files';

export default function FilesPage() {
  const [files, setFiles] = useState<FileListItem[]>([]);
  const [dragActive, setDragActive] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingFiles, setIsLoadingFiles] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const filesService = new FilesService();

  useEffect(() => {
    loadFiles();
  }, []);

  const loadFiles = async () => {
    try {
      setIsLoadingFiles(true);
      setError(null);

      const response = await filesService.listFiles();

      if (response.success && response.data) {
        setFiles(response.data); // ✅ Only pass FileListItem[]
      } else {
        setFiles([]); // fallback on failure
      }

    } catch (error) {
      console.error('Error loading files:', error);
      setError(error instanceof Error ? error.message : 'Failed to load files');
      setFiles([]); // fallback on error
    } finally {
      setIsLoadingFiles(false);
    }
  };
  

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    const droppedFiles = Array.from(e.dataTransfer.files);
    if (droppedFiles.length > 0) {
      uploadFile(droppedFiles[0]);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    if (selectedFiles.length > 0) {
      uploadFile(selectedFiles[0]);
    }
  };

  const uploadFile = async (file: File) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await filesService.uploadFile(file, [], []); //missing senders and receivers
      if (response.success) {
        console.log('File uploaded successfully:', response.data);
        // Reload files after successful upload
        await loadFiles();
      } else {
        setError(response.message || 'Failed to upload file');
      }
    } catch (error) {
      console.error('Error uploading file:', error);
      setError(error instanceof Error ? error.message : 'Failed to upload file');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteFile = async (fileId: string) => {
    const fileToDelete = files.find(f => f.id === fileId);
    if (!fileToDelete || !confirm(`Are you sure you want to delete ${fileToDelete.name}?`)) {
      return;
    }

    try {
      const response = await filesService.deleteFile(fileId);
      if (response.success) {
        // Reload files after successful deletion
        await loadFiles();
      } else {
        setError(response.message || 'Failed to delete file');
      }
    } catch (error) {
      console.error('Error deleting file:', error);
      setError(error instanceof Error ? error.message : 'Failed to delete file');
    }
  };

  const getFileIconSvg = (fileType: string) => {
    if (fileType === 'image') {
      return (
        <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--primary)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      );
    } else if (fileType === 'document') {
      return (
        <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--danger)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
      );
    } else if (fileType === 'spreadsheet') {
      return (
        <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--success)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      );
    } else {
      return (
        <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      );
    }
  };

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-6xl space-y-8">
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            File Manager
          </h1>
          
          {error && (
            <div className="rounded-2xl p-4 mb-4 border-2" style={{ backgroundColor: 'var(--danger)', color: 'white', borderColor: 'var(--danger)' }}>
              <p>{error}</p>
              <button 
                onClick={() => setError(null)} 
                className="mt-2 px-3 py-1 bg-white text-red-600 rounded"
              >
                Dismiss
              </button>
            </div>
          )}
          
          {/* Upload Area */}
          <div
            className={`rounded-2xl p-8 border-2 text-center transition-all duration-200 ${
              dragActive ? 'scale-105' : ''
            }`}
            style={{
              backgroundColor: 'var(--bg)',
              background: dragActive ? 'var(--gradient-hover)' : 'var(--gradient)',
              borderColor: dragActive ? 'var(--primary)' : 'var(--border)',
              borderStyle: 'dashed'
            }}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
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
                <svg className="mx-auto h-16 w-16 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--primary)' }}>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
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
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 mx-auto" style={{ borderColor: 'var(--primary)' }}></div>
              </div>
            )}
          </div>

          {/* Files List */}
          <div
            className="rounded-2xl shadow-xl border-2 overflow-hidden transition-all duration-200"
            style={{
              backgroundColor: 'var(--bg)',
              background: 'var(--gradient)',
              borderColor: 'var(--border)'
            }}
          >
            <div className="px-8 py-6 border-b-2" style={{ borderColor: 'var(--border)' }}>
              <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
                  Your Files ({files.length})
                </h2>
                <button
                  onClick={loadFiles}
                  disabled={isLoadingFiles}
                  className="px-4 py-2 rounded transition-colors duration-200"
                  style={{ 
                    backgroundColor: 'var(--primary)', 
                    color: 'white',
                    opacity: isLoadingFiles ? 0.6 : 1 
                  }}
                >
                  {isLoadingFiles ? 'Loading...' : 'Refresh'}
                </button>
              </div>
            </div>
            
            {isLoadingFiles ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
                <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
                  Loading files...
                </p>
              </div>
            ) : files.length === 0 ? (
              <div className="p-12 text-center">
                <svg className="mx-auto h-16 w-16 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h10a2 2 0 012 2v14a2 2 0 01-2 2z" />
                </svg>
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
                    key={file.id} // Use file ID as key instead of name
                    file={file} 
                    onFileClick={(fileId) => {
                      console.log("Clicked here trust")
                      router.push(`/file-details?fileId=${encodeURIComponent(fileId)}`)}}
                    onDeleteClick={handleDeleteFile}
                    getFileIconSvg={getFileIconSvg}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

interface FileRowProps {
  file: FileListItem;
  onFileClick: (fileId: string) => void; // Changed from fileName to fileId
  onDeleteClick: (fileId: string) => void; // Changed from fileName to fileId
  getFileIconSvg: (fileType: string) => React.ReactNode;
}

const FileRow = ({ file, onFileClick, onDeleteClick, getFileIconSvg }: FileRowProps) => {
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
          onClick={() => onFileClick(file.id)} // Use file.id instead of file.name
        >
          <div className="flex-shrink-0">
            {getFileIconSvg(file.fileType)}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold truncate" style={{ color: 'var(--text)' }}>
              {file.name}
            </p>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDeleteClick(file.id); // Use file.id instead of file.name
            }}
            className="p-2 rounded hover:bg-red-100 transition-colors duration-200"
            style={{ color: 'var(--danger)' }}
            title="Delete file"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </div>
  );
};