import { useState, useCallback } from 'react';
import { FilesService } from '@/services/api/FilesService';
import { FileListItem, FileMetadataData } from '@/models/files';

interface FilesState {
  files: FileListItem[];
  selectedFile: FileMetadataData | null;
  loading: boolean;
  uploadLoading: boolean;
  error: string | null;
  dragActive: boolean;
}

interface FilesError {
  general?: string;
  upload?: string;
  delete?: string;
}

export function useFilesViewModel() {
  const [state, setState] = useState<FilesState>({
    files: [],
    selectedFile: null,
    loading: false,
    uploadLoading: false,
    error: null,
    dragActive: false,
  });

  const [errors, setErrors] = useState<FilesError>({});
  const filesService = new FilesService();

  const clearError = useCallback((field?: keyof FilesError) => {
    if (field) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    } else {
      setErrors({});
      setState(prev => ({ ...prev, error: null }));
    }
  }, []);

  const loadFiles = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    clearError();

    try {
      const response = await filesService.listFiles();
      
      if (response.success && response.data) {
        setState(prev => ({ 
          ...prev, 
          files: response.data!, 
          loading: false 
        }));
      } else {
        setState(prev => ({ 
          ...prev, 
          files: [], 
          loading: false 
        }));
        setErrors({ general: response.message || 'Failed to load files' });
      }
    } catch (error) {
      setState(prev => ({ 
        ...prev, 
        files: [], 
        loading: false 
      }));
      setErrors({ general: error instanceof Error ? error.message : 'Failed to load files' });
    }
  }, [clearError]);

  const uploadFile = useCallback(async (file: File, senders: string[] = [], receivers: string[] = []) => {
    setState(prev => ({ ...prev, uploadLoading: true }));
    clearError('upload');

    try {
      const response = await filesService.uploadFile(file, senders, receivers);
      
      if (response.success) {
        await loadFiles(); // Reload files after successful upload
        setState(prev => ({ ...prev, uploadLoading: false }));
        return { success: true };
      } else {
        setState(prev => ({ ...prev, uploadLoading: false }));
        setErrors({ upload: response.message || 'Failed to upload file' });
        return { success: false, message: response.message };
      }
    } catch (error) {
      setState(prev => ({ ...prev, uploadLoading: false }));
      const errorMessage = error instanceof Error ? error.message : 'Failed to upload file';
      setErrors({ upload: errorMessage });
      return { success: false, message: errorMessage };
    }
  }, [loadFiles, clearError]);

  const deleteFile = useCallback(async (fileId: string) => {
    clearError('delete');

    try {
      const response = await filesService.deleteFile(fileId);
      
      if (response.success) {
        await loadFiles(); // Reload files after successful deletion
        return { success: true };
      } else {
        setErrors({ delete: response.message || 'Failed to delete file' });
        return { success: false, message: response.message };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to delete file';
      setErrors({ delete: errorMessage });
      return { success: false, message: errorMessage };
    }
  }, [loadFiles, clearError]);

  const getFileMetadata = useCallback(async (fileId: string) => {
    setState(prev => ({ ...prev, loading: true, error: null, selectedFile: null }));
    clearError();

    try {
      const response = await filesService.getFileMetadata(fileId);
      
      if (response.success && response.data) {
        setState(prev => ({ 
          ...prev, 
          selectedFile: response.data!, 
          loading: false 
        }));
        return { success: true, data: response.data };
      } else {
        setState(prev => ({ ...prev, loading: false }));
        setErrors({ general: response.message || 'Failed to fetch file metadata' });
        return { success: false, message: response.message };
      }
    } catch (error) {
      setState(prev => ({ ...prev, loading: false }));
      const errorMessage = error instanceof Error ? error.message : 'Failed to fetch file metadata';
      setErrors({ general: errorMessage });
      return { success: false, message: errorMessage };
    }
  }, [clearError]);

  const downloadFile = useCallback(async (fileId: string) => {
    try {
      await filesService.downloadFile(fileId);
      return { success: true };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Download failed';
      setErrors({ general: errorMessage });
      return { success: false, message: errorMessage };
    }
  }, []);

  const setDragActive = useCallback((active: boolean) => {
    setState(prev => ({ ...prev, dragActive: active }));
  }, []);

  return {
    // State
    files: state.files,
    selectedFile: state.selectedFile,
    loading: state.loading,
    uploadLoading: state.uploadLoading,
    error: state.error,
    dragActive: state.dragActive,
    errors,

    // Actions
    loadFiles,
    uploadFile,
    deleteFile,
    getFileMetadata,
    downloadFile,
    setDragActive,
    clearError,
  };
}