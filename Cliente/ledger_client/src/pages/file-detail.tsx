import '../app/globals.css'

import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/Button';
import { FileItem } from '@/types/files';

// Mock data - in a real app, this would come from your API
const mockFiles: FileItem[] = [
  {
    id: '1',
    name: 'Project_Report.pdf',
    size: 2048576,
    type: 'application/pdf',
    createdAt: new Date('2024-01-15'),
    modifiedAt: new Date('2024-01-20'),
    owner: 'john.doe@example.com',
    isShared: true
  },
  {
    id: '2',
    name: 'Budget_2024.xlsx',
    size: 512000,
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    createdAt: new Date('2024-01-10'),
    modifiedAt: new Date('2024-01-18'),
    owner: 'jane.smith@example.com',
    isShared: false
  },
  {
    id: '3',
    name: 'Team_Photo.jpg',
    size: 1024000,
    type: 'image/jpeg',
    createdAt: new Date('2024-01-12'),
    modifiedAt: new Date('2024-01-12'),
    owner: 'mike.wilson@example.com',
    isShared: true
  },
  {
    id: '4',
    name: 'Meeting_Notes.docx',
    size: 256000,
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    createdAt: new Date('2024-01-14'),
    modifiedAt: new Date('2024-01-16'),
    owner: 'sarah.johnson@example.com',
    isShared: false
  }
];

export default function FileDetailsPage() {
  const router = useRouter();
  const { id } = router.query;
  const [file, setFile] = useState<FileItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (id) {
      // Simulate API call to fetch file details
      const foundFile = mockFiles.find(f => f.id === id);
      setFile(foundFile || null);
      setIsLoading(false);
    }
  }, [id]);

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  const getFileIcon = (type: string, size: string = 'h-20 w-20') => {
    if (type.startsWith('image/')) {
      return (
        <svg className={size} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--primary)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      );
    } else if (type.includes('pdf')) {
      return (
        <svg className={size} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--danger)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
      );
    } else if (type.includes('sheet') || type.includes('excel')) {
      return (
        <svg className={size} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--success)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      );
    } else {
      return (
        <svg className={size} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      );
    }
  };

  const handleDownload = () => {
    // Simulate file download
    console.log('Downloading file:', file?.name);
    // In a real app, you would trigger the actual download
  };

  const handleShare = () => {
    if (!file) return;
    setFile({ ...file, isShared: !file.isShared });
    console.log('Toggling share for file:', file.name);
  };

  const handleDelete = async () => {
    if (!file) return;
    setIsDeleting(true);
    // Simulate delete operation
    setTimeout(() => {
      console.log('Deleting file:', file.name);
      setIsDeleting(false);
      setShowDeleteConfirm(false);
      router.push('/files');
    }, 1000);
  };

  const handleBack = () => {
    router.push('/files');
  };

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-screen">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2" style={{ borderColor: 'var(--primary)' }}></div>
        </div>
      </Layout>
    );
  }

  if (!file) {
    return (
      <Layout>
        <div className="flex items-center justify-center p-1 pb-20">
          <div className="w-4/5 max-w-4xl text-center">
            <h1 className="text-4xl font-bold mb-4" style={{ color: 'var(--text)' }}>
              File Not Found
            </h1>
            <p className="text-xl mb-8" style={{ color: 'var(--text-muted)' }}>
              The file you're looking for doesn't exist or has been deleted.
            </p>
            <Button onClick={handleBack}>
              Back to Files
            </Button>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="flex items-center justify-center p-1 pb-20">
        <div className="w-4/5 max-w-4xl space-y-8">
          {/* Header with back button */}
          <div className="flex items-center space-x-4">
            <button
              onClick={handleBack}
              className="p-2 rounded-full transition-all duration-200 hover:scale-110"
              style={{ backgroundColor: 'var(--bg)', border: '2px solid var(--border)' }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary)';
                e.currentTarget.style.borderColor = 'var(--primary)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--bg)';
                e.currentTarget.style.borderColor = 'var(--border)';
              }}
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text)' }}>
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <h1 className="text-4xl font-bold" style={{ color: 'var(--text)' }}>
              File Details
            </h1>
          </div>

          {/* File details card */}
          <div
            className="rounded-2xl shadow-xl border-2 overflow-hidden"
            style={{
              backgroundColor: 'var(--bg)',
              background: 'var(--gradient)',
              borderColor: 'var(--border)'
            }}
          >
            {/* File header */}
            <div className="px-8 py-8 text-center border-b-2" style={{ borderColor: 'var(--border)' }}>
              <div className="flex justify-center mb-6">
                {getFileIcon(file.type)}
              </div>
              <h2 className="text-3xl font-bold mb-2" style={{ color: 'var(--text)' }}>
                {file.name}
              </h2>
              <p className="text-lg" style={{ color: 'var(--text-muted)' }}>
                {formatFileSize(file.size)}
              </p>
              {file.isShared && (
                <span 
                  className="inline-flex items-center px-4 py-2 rounded-full text-sm font-medium mt-4"
                  style={{ 
                    backgroundColor: 'var(--primary)', 
                    color: 'white'
                  }}
                >
                  <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
                  </svg>
                  Shared
                </span>
              )}
            </div>

            {/* File properties */}
            <div className="px-8 py-6 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>
                    File Information
                  </h3>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span style={{ color: 'var(--text-muted)' }}>Type:</span>
                      <span style={{ color: 'var(--text)' }}>{file.type}</span>
                    </div>
                    <div className="flex justify-between">
                      <span style={{ color: 'var(--text-muted)' }}>Size:</span>
                      <span style={{ color: 'var(--text)' }}>{formatFileSize(file.size)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span style={{ color: 'var(--text-muted)' }}>Owner:</span>
                      <span style={{ color: 'var(--text)' }}>{file.owner}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-semibold mb-3" style={{ color: 'var(--text)' }}>
                    Timestamps
                  </h3>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span style={{ color: 'var(--text-muted)' }}>Created:</span>
                      <span style={{ color: 'var(--text)' }}>{formatDate(file.createdAt)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span style={{ color: 'var(--text-muted)' }}>Modified:</span>
                      <span style={{ color: 'var(--text)' }}>{formatDate(file.modifiedAt)}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Action buttons */}
              <div className="pt-6 border-t-2" style={{ borderColor: 'var(--border)' }}>
                <h3 className="text-lg font-semibold mb-4" style={{ color: 'var(--text)' }}>
                  Actions
                </h3>
                <div className="flex flex-wrap gap-4">
                  <button
                    onClick={handleDownload}
                    className="flex items-center px-6 py-3 rounded-xl font-medium transition-all duration-200 hover:scale-105"
                    style={{ 
                      backgroundColor: 'var(--primary)', 
                      color: 'white',
                      border: '2px solid var(--primary)'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.transform = 'scale(1.05)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.transform = 'scale(1)';
                    }}
                  >
                    <svg className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Download
                  </button>

                  <button
                    onClick={handleShare}
                    className="flex items-center px-6 py-3 rounded-xl font-medium transition-all duration-200 hover:scale-105"
                    style={{ 
                      backgroundColor: file.isShared ? 'var(--success)' : 'var(--bg)', 
                      color: file.isShared ? 'white' : 'var(--text)',
                      border: `2px solid ${file.isShared ? 'var(--success)' : 'var(--border)'}`
                    }}
                  >
                    <svg className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
                    </svg>
                    {file.isShared ? 'Unshare' : 'Share'}
                  </button>

                  <button
                    onClick={() => setShowDeleteConfirm(true)}
                    className="flex items-center px-6 py-3 rounded-xl font-medium transition-all duration-200 hover:scale-105"
                    style={{ 
                      backgroundColor: 'var(--bg)', 
                      color: 'var(--danger)',
                      border: '2px solid var(--danger)'
                    }}
                  >
                    <svg className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Delete confirmation modal */}
          {showDeleteConfirm && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div
                className="rounded-2xl p-8 max-w-md mx-4"
                style={{
                  backgroundColor: 'var(--bg)',
                  background: 'var(--gradient)',
                  border: '2px solid var(--border)'
                }}
              >
                <h3 className="text-2xl font-bold mb-4" style={{ color: 'var(--text)' }}>
                  Delete File
                </h3>
                <p className="mb-6" style={{ color: 'var(--text-muted)' }}>
                  Are you sure you want to delete "{file.name}"? This action cannot be undone.
                </p>
                <div className="flex space-x-4">
                  <button
                    onClick={handleDelete}
                    disabled={isDeleting}
                    className="flex-1 px-4 py-2 rounded-xl font-medium transition-all duration-200"
                    style={{ 
                      backgroundColor: 'var(--danger)', 
                      color: 'white',
                      opacity: isDeleting ? 0.7 : 1
                    }}
                  >
                    {isDeleting ? (
                      <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                        Deleting...
                      </div>
                    ) : (
                      'Delete'
                    )}
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={isDeleting}
                    className="flex-1 px-4 py-2 rounded-xl font-medium transition-all duration-200"
                    style={{ 
                      backgroundColor: 'var(--bg)', 
                      color: 'var(--text)',
                      border: '2px solid var(--border)',
                      opacity: isDeleting ? 0.7 : 1
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}