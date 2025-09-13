export interface FileMetadataDto {
  id: string;
  originalFileName: string;
  fileSize: number;
  contentType?: string;
  uploadedAt: number;
  uploaderId: string;
  senders: string[];
  receivers: string[];
  ledgerEntries: string[];
  wasDeleted: boolean;
}

export interface FileListResponse {
  files: FileInfoDTO[];
}

export interface FileInfoDTO {
  id: string;
  name: string;
  size: number;
  wasDeleted: boolean;
}

export interface DeleteFileResponse {
  message: string;
}

export interface FileUploadRequest {
  file: File;
  senders?: string[];
  receivers?: string[];
}

export interface FileUploadResponse {
  message: string;
  fileId: string;
  fileName: string;
}