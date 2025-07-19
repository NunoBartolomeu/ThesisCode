// viewmodels/FilesViewModel.ts

import { 
  FileListItem, 
  FileDetailsData, 
  UploadFileResponse, 
  ListFilesResponse, 
  DeleteFileResponse, 
  ApiErrorResponse 
} from '@/types/files';

export class FilesViewModel {
  private baseUrl: string;
  private token: string | null;

  constructor() {
    this.baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    this.token = this.getAuthToken();
  }

  private getAuthToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('authToken');
    }
    return null;
  }

  private getAuthHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    return headers;
  }

  private getFileUploadHeaders(): HeadersInit {
    const headers: HeadersInit = {};

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    return headers;
  }

  public async uploadFile(file: File): Promise<UploadFileResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/file/upload`, {
      method: 'POST',
      headers: this.getFileUploadHeaders(),
      body: formData,
    });

    if (!response.ok) {
      const errorData: ApiErrorResponse = await response.json();
      throw new Error(errorData.error || 'Upload failed');
    }

    return response.json();
  }

  public async listFiles(): Promise<FileListItem[]> {
    const response = await fetch(`${this.baseUrl}/file/list`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData: ApiErrorResponse = await response.json();
      throw new Error(errorData.error || 'Failed to fetch files');
    }

    const data: ListFilesResponse = await response.json();
    
    return data.files.map(file => this.mapToFileListItem(file));
  }

  public async deleteFile(fileName: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/file/delete/${encodeURIComponent(fileName)}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('File not found');
      }
      const errorData: ApiErrorResponse = await response.json();
      throw new Error(errorData.error || 'Failed to delete file');
    }
  }

  public async getFileDetails(fileName: string): Promise<FileDetailsData> {
    // First, get the file info from the list
    const files = await this.listFiles();
    const file = files.find(f => f.name === fileName);
    
    if (!file) {
      throw new Error('File not found');
    }

    return {
      name: file.name,
      size: file.size,
      lastModified: file.lastModified,
      formattedSize: file.formattedSize,
      formattedDate: file.formattedDate,
      fileType: file.fileType,
      icon: file.icon,
      downloadUrl: `${this.baseUrl}/file/download/${encodeURIComponent(fileName)}`,
    };
  }

  public getDownloadUrl(fileName: string): string {
    return `${this.baseUrl}/file/download/${encodeURIComponent(fileName)}`;
  }

  public async downloadFile(fileName: string): Promise<void> {
    const downloadUrl = this.getDownloadUrl(fileName);
    
    const response = await fetch(downloadUrl, {
      method: 'GET',
      headers: this.getFileUploadHeaders(), // No content-type for download
    });

    if (!response.ok) {
      throw new Error('Failed to download file');
    }

    // Create blob and download
    const blob = await response.blob();
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
      formattedSize: this.formatFileSize(file.size),
      formattedDate: this.formatDate(lastModified),
      fileType,
      icon: this.getFileIcon(fileType),
    };
  }

  private formatFileSize(bytes: number): string {
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
      minute: '2-digit'
    }).format(date);
  }

  private getFileExtension(fileName: string): string {
    return fileName.split('.').pop()?.toLowerCase() || '';
  }

  private getFileTypeFromExtension(extension: string): string {
    const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'];
    const documentTypes = ['pdf', 'doc', 'docx', 'txt', 'rtf'];
    const spreadsheetTypes = ['xls', 'xlsx', 'csv'];
    const presentationTypes = ['ppt', 'pptx'];
    const archiveTypes = ['zip', 'rar', '7z', 'tar', 'gz'];
    const videoTypes = ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv'];
    const audioTypes = ['mp3', 'wav', 'flac', 'aac', 'ogg'];

    if (imageTypes.includes(extension)) return 'image';
    if (documentTypes.includes(extension)) return 'document';
    if (spreadsheetTypes.includes(extension)) return 'spreadsheet';
    if (presentationTypes.includes(extension)) return 'presentation';
    if (archiveTypes.includes(extension)) return 'archive';
    if (videoTypes.includes(extension)) return 'video';
    if (audioTypes.includes(extension)) return 'audio';

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