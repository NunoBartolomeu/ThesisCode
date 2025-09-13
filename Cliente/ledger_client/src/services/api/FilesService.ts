import { Fetcher, ApiResponse } from "./Fetcher";
import { 
  FileMetadataDto, 
  FileListResponse, 
  FileInfoDTO, 
  FileUploadResponse 
} from "@/models/files_dto";
import { FileListItem, FileMetadataData } from '@/models/files';

export class FilesService {
  private baseUrl = "/file";
  private fetcher = new Fetcher();

  async uploadFile(file: File, senders: string[], receivers: string[]): Promise<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (senders?.length > 0) {
      senders.forEach(sender => formData.append('senders', sender));
    }
    if (receivers?.length > 0) {
      receivers.forEach(receiver => formData.append('receivers', receiver));
    }

    return this.fetcher.request<FileUploadResponse>(`${this.baseUrl}/upload`, {
      method: 'POST',
      body: formData,
      requireAuth: true,
      isFormData: true,
    });
  }

  async listFiles(): Promise<ApiResponse<FileListItem[]>> {
    const response = await this.fetcher.request<FileListResponse>(`${this.baseUrl}/list`, {
      method: 'GET',
      requireAuth: true,
    });

    if (response.success && response.data) {
      const files = response.data.files
        .filter(file => !file.wasDeleted)
        .map(file => this.mapToFileListItem(file));
      return { ...response, data: files };
    }

    return { ...response, data: [] };
  }

  async getFileMetadata(fileId: string): Promise<ApiResponse<FileMetadataData>> {
    const response = await this.fetcher.request<FileMetadataDto>(
      `${this.baseUrl}/metadata/${encodeURIComponent(fileId)}`,
      {
        method: 'GET',
        requireAuth: true,
      }
    );

    if (response.success && response.data) {
      const metadata = this.transformToFileMetadataData(response.data);
      return { ...response, data: metadata };
    }

    return { ...response, data: null };
  }

  async deleteFile(fileId: string): Promise<ApiResponse<void>> {
    return this.fetcher.request<void>(`${this.baseUrl}/delete/${encodeURIComponent(fileId)}`, {
      method: 'DELETE',
      requireAuth: true,
    });
  }

  async downloadFile(fileId: string): Promise<void> {
    const downloadUrl = `${this.baseUrl}/download/${encodeURIComponent(fileId)}`;

    const response = await this.fetcher.request<Blob>(downloadUrl, {
      method: 'GET',
      requireAuth: true,
      responseType: 'blob',
    });

    if (!response.success || !response.data) {
      throw new Error(`Failed to download file`);
    }

    const metadataResponse = await this.getFileMetadata(fileId);
    const fileName = metadataResponse.data?.name || `download-${fileId}`;

    const blob = response.data;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }

  private mapToFileListItem(file: FileInfoDTO): FileListItem {
    const fileExtension = this.getFileExtension(file.name);
    const fileType = this.getFileTypeFromExtension(fileExtension);

    return {
      id: file.id,
      name: file.name,
      size: file.size,
      formattedSize: this.formatBytes(file.size),
      fileType,
      icon: this.getFileIcon(fileType),
    };
  }

  private transformToFileMetadataData(dto: FileMetadataDto): FileMetadataData {
    const fileExtension = this.getFileExtension(dto.originalFileName);
    const fileType = this.getFileTypeFromExtension(fileExtension);

    return {
      id: dto.id,
      name: dto.originalFileName,
      size: dto.fileSize,
      formattedSize: this.formatBytes(dto.fileSize),
      fileType,
      icon: this.getFileIcon(fileType),
      contentType: dto.contentType,
      uploaderId: dto.uploaderId,
      senders: dto.senders,
      receivers: dto.receivers,
      ledgerEntries: dto.ledgerEntries,
      downloadUrl: `${this.baseUrl}/download/${encodeURIComponent(dto.id)}`,
    };
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  private getFileExtension(fileName: string): string {
    return fileName.split('.').pop()?.toLowerCase() || '';
  }

  private getFileTypeFromExtension(extension: string): string {
    const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'];
    const documentExts = ['pdf', 'doc', 'docx', 'txt', 'rtf'];
    const spreadsheetExts = ['xls', 'xlsx', 'csv'];
    const presentationExts = ['ppt', 'pptx'];
    const archiveExts = ['zip', 'rar', '7z', 'tar', 'gz'];
    const videoExts = ['mp4', 'avi', 'mov', 'mkv', 'webm'];
    const audioExts = ['mp3', 'wav', 'flac', 'aac'];

    if (imageExts.includes(extension)) return 'image';
    if (documentExts.includes(extension)) return 'document';
    if (spreadsheetExts.includes(extension)) return 'spreadsheet';
    if (presentationExts.includes(extension)) return 'presentation';
    if (archiveExts.includes(extension)) return 'archive';
    if (videoExts.includes(extension)) return 'video';
    if (audioExts.includes(extension)) return 'audio';
    return 'file';
  }

  private getFileIcon(fileType: string): string {
    return fileType;
  }
}