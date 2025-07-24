import { FileDetailsDto, FileListResponse, FileInfoDTO, DeleteFileResponse, FileUploadRequest } from "@/dto/files_dto";
import { Fetcher, ApiResponse } from "./Fetcher";
import { FileListItem, FileDetailsData} from '@/types/files';


export class FilesService {
  private fetcher: Fetcher;

  constructor(baseUrl?: string) {
    this.fetcher = new Fetcher(baseUrl);
  }

  async uploadFile(file: File): Promise<ApiResponse<void>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.fetcher.request<void>('/file/upload', {
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
      const files = response.data.files.map(file => this.mapToFileListItem(file));
      return { ...response, data: files };
    }

    return { ...response, data: null };
  }

  async getFileDetails(fileName: string): Promise<ApiResponse<FileDetailsData>> {
    const response = await this.fetcher.request<FileDetailsDto>(
      `/file/details/${encodeURIComponent(fileName)}`,
      {
        method: 'GET',
        requireAuth: true,
      }
    );

    if (response.success && response.data) {
      const details = this.transformToFileDetailsData(response.data);
      return { ...response, data: details };
    }

    return { ...response, data: null };
  }

  async deleteFile(fileName: string): Promise<ApiResponse<void>> {
    return this.fetcher.request<void>(`/file/delete/${encodeURIComponent(fileName)}`, {
      method: 'DELETE',
      requireAuth: true,
    });
  }

  async downloadFile(fileName: string): Promise<void> {
    const downloadUrl = `/file/download/${encodeURIComponent(fileName)}`;

    const response = await this.fetcher.request<Blob>(downloadUrl, {
      method: 'GET',
      requireAuth: true,
      responseType: 'blob',
    });

    if (!response.success || !response.data) {
      throw new Error(`Failed to download file`);
    }

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


  private mapToFileListItem(file: { name: string; size: number; lastModified: number }): FileListItem {
    const lastModified = new Date(file.lastModified);
    const fileExtension = this.getFileExtension(file.name);
    const fileType = this.getFileTypeFromExtension(fileExtension);

    return {
      name: file.name,
      size: file.size,
      lastModified,
      formattedSize: this.formatBytes(file.size),
      formattedDate: this.formatDate(lastModified),
      fileType,
      icon: this.getFileIcon(fileType),
    };
  }

  private transformToFileDetailsData(dto: FileDetailsDto): FileDetailsData {
    const lastModified = new Date(dto.uploadedAt);
    const fileExtension = dto.actualFileName.split('.').pop()?.toLowerCase() || '';
    const fileType = this.getFileTypeFromExtension(fileExtension);

    return {
      name: dto.originalFileName,
      size: dto.fileSize,
      lastModified,
      formattedSize: this.formatBytes(dto.fileSize),
      formattedDate: this.formatDate(lastModified),
      fileType,
      icon: this.getFileIcon(fileType),
      downloadUrl: `/file/download/${encodeURIComponent(dto.actualFileName)}`,
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