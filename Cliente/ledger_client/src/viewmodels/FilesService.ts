import { FileMetadataDto, FileListResponse, FileInfoDTO, DeleteFileResponse, FileUploadRequest, FileUploadResponse } from "@/dto/files_dto";
import { Fetcher, ApiResponse } from "./Fetcher";
import { FileListItem, FileMetadataData} from '@/types/files';

export class FilesService {
  private fetcher: Fetcher;

  constructor(baseUrl?: string) {
    this.fetcher = new Fetcher(baseUrl);
  }

  async uploadFile(file: File, senders: string[], receivers: string[]): Promise<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    
    // Add senders and receivers if provided
    if (senders && senders.length > 0) {
      senders.forEach(sender => formData.append('senders', sender));
    }
    if (receivers && receivers.length > 0) {
      receivers.forEach(receiver => formData.append('receivers', receiver));
    }

    return this.fetcher.request<FileUploadResponse>('/file/upload', {
      method: 'POST',
      body: formData,
      requireAuth: true,
      isFormData: true,
    });
  }

  async listFiles(): Promise<ApiResponse<FileListItem[]>> {
    const response = await this.fetcher.request<FileListResponse>('/file/list', {
      method: 'GET',
      requireAuth: true,
    });

    if (response.success && response.data) {
      const files = response.data.files
        .filter(file => !file.wasDeleted) // Filter out deleted files
        .map(file => this.mapToFileListItem(file));
      return { ...response, data: files };
    }

    return { ...response, data: null };
  }

  async getFileMetadata(fileId: string): Promise<ApiResponse<FileMetadataData>> {
    const response = await this.fetcher.request<FileMetadataDto>(
      `/file/metadata/${encodeURIComponent(fileId)}`,
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
    return this.fetcher.request<void>(`/file/delete/${encodeURIComponent(fileId)}`, {
      method: 'DELETE',
      requireAuth: true,
    });
  }

  async downloadFile(fileId: string): Promise<void> {
    const downloadUrl = `/file/download/${encodeURIComponent(fileId)}`;

    const response = await this.fetcher.request<Blob>(downloadUrl, {
      method: 'GET',
      requireAuth: true,
      responseType: 'blob',
    });

    if (!response.success || !response.data) {
      throw new Error(`Failed to download file`);
    }

    // Get the filename from the metadata first for proper download
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
      id: file.id, // Now include the file ID
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
      downloadUrl: `/file/download/${encodeURIComponent(dto.id)}`, // Use ID for download
    };
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  private formatDate(date: Date): string {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
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
    const iconMap: Record<string, string> = {
      image: '🖼️',
      document: '📄',
      spreadsheet: '📊',
      presentation: '📽️',
      archive: '📦',
      video: '🎥',
      audio: '🎵',
      file: '📁',
    };
    return iconMap[fileType] || '📁';
  }
}