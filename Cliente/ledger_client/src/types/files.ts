// types/files.ts

export interface FileItem {
  id: string;
  name: string;
  size: number;
  type: string;
  createdAt: Date;
  modifiedAt: Date;
  owner: string;
  isShared: boolean;
}

// DTOs for API communication
export interface UploadFileResponse {
  message: string;
  fileName: string;
  size: number;
}

export interface ListFilesResponse {
  files: {
    name: string;
    size: number;
    lastModified: number;
  }[];
}

export interface DeleteFileResponse {
  message: string;
}

export interface ApiErrorResponse {
  error: string;
}

// Request types
export interface FileUploadRequest {
  file: File;
}

// View model types
export interface FileListItem {
  name: string;
  size: number;
  lastModified: Date;
  formattedSize: string;
  formattedDate: string;
  fileType: string;
  icon: string;
}

export interface FileDetailsData {
  name: string;
  size: number;
  lastModified: Date;
  formattedSize: string;
  formattedDate: string;
  fileType: string;
  icon: string;
  downloadUrl: string;
}