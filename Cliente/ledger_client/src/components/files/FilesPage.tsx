'use client';

import { useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { FileUploadForm } from '@/components/files/FileUploadForm';
import { FilesListing } from '@/components/files/FilesSListing';
import { useFilesViewModel } from '@/viewmodels/useFilesViewModel';

export function FilesPage() {
  const router = useRouter();
  const {
    files,
    loading,
    uploadLoading,
    dragActive,
    errors,
    loadFiles,
    uploadFile,
    deleteFile,
    setDragActive,
    clearError,
  } = useFilesViewModel();

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  }, [setDragActive]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    const droppedFiles = Array.from(e.dataTransfer.files);
    if (droppedFiles.length > 0) {
      uploadFile(droppedFiles[0]);
    }
  }, [setDragActive, uploadFile]);

  const handleFileUpload = useCallback((file: File) => {
    uploadFile(file, [], []);
  }, [uploadFile]);

  const handleFileClick = useCallback((fileId: string) => {
    router.push(`/file-details?fileId=${encodeURIComponent(fileId)}`);
  }, [router]);

  const handleDeleteClick = useCallback(async (fileId: string) => {
    await deleteFile(fileId);
  }, [deleteFile]);

  return (
    <div className="flex items-center justify-center p-1 pb-20">
      <div className="w-4/5 max-w-6xl space-y-8">
        <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
          File Manager
        </h1>

        <FileUploadForm
          onFileUpload={handleFileUpload}
          dragActive={dragActive}
          isLoading={uploadLoading}
          error={errors.upload}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          onClearError={() => clearError('upload')}
        />

        <FilesListing
          files={files}
          loading={loading}
          onRefresh={loadFiles}
          onFileClick={handleFileClick}
          onDeleteClick={handleDeleteClick}
          error={errors.delete || errors.general}
          onClearError={() => clearError()}
        />
      </div>
    </div>
  );
}