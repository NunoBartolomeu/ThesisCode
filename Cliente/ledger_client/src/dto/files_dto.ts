export interface FileDetailsDto {
  originalFileName: string;
  actualFileName: string;
  fileSize: number;
  contentType?: string;
  uploadedAt: number;
  lastAccessed?: number;
  ownerFullName?: string;
  ownerEmail?: string;
}

export interface FileListResponse {
  files: FileInfoDTO[];
}

export interface FileInfoDTO {
  name: string;
  size: number;
  lastModified: number;
}

export interface DeleteFileResponse {
  message: string;
}

export interface FileUploadRequest {
  file: File;
}