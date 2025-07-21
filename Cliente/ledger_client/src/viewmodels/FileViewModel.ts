// viewmodels/FilesViewModel.ts

import { 
  FileListItem, 
  FileDetailsData, 
  UploadFileResponse, 
  ListFilesResponse, 
  DeleteFileResponse, 
  ApiErrorResponse, 
  FileDetailsDto
} from '@/types/files';

export class FilesViewModel {
  private baseUrl: string;

  constructor(baseUrl?: string) {
    this.baseUrl = (baseUrl || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080').replace(/\/$/, '');
  }


  // Add this method to your FilesViewModel class

  async getFileDetails(fileName: string): Promise<FileDetailsData> {
    try {
      const response = await fetch(`/file/details/${encodeURIComponent(fileName)}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          // Add your auth headers here
          // 'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to get file details: ${response.status}`);
      }

      const dto: FileDetailsDto = await response.json();
      
      // Transform backend DTO to frontend FileDetailsData
      return this.transformToFileDetailsData(dto);
    } catch (error) {
      console.error('Error getting file details:', error);
      throw error;
    }
  }

  private transformToFileDetailsData(dto: FileDetailsDto): FileDetailsData {
    const lastModified = new Date(dto.createdDate);
    const fileExtension = dto.name.split('.').pop()?.toLowerCase() || '';
    
    return {
      name: dto.name,
      size: dto.size,
      lastModified: lastModified,
      formattedSize: this.formatBytes(dto.size),
      formattedDate: lastModified.toLocaleDateString() + ' ' + lastModified.toLocaleTimeString(),
      fileType: this.getFileTypeFromExtension(fileExtension),
      icon: this.getIconFromFileType(this.getFileTypeFromExtension(fileExtension)),
      downloadUrl: `/file/download/${encodeURIComponent(dto.name)}`
    };
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  private getFileTypeFromExtension(extension: string): string {
    const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'];
    const documentExts = ['pdf', 'doc', 'docx', 'txt', 'rtf'];
    const spreadsheetExts = ['xls', 'xlsx', 'csv'];
    
    if (imageExts.includes(extension)) {
      return 'image';
    } else if (documentExts.includes(extension)) {
      return 'document';
    } else if (spreadsheetExts.includes(extension)) {
      return 'spreadsheet';
    }
    return 'file';
  }

  private getIconFromFileType(fileType: string): string {
    // Return appropriate icon identifier based on your icon system
    return fileType;
  }










  private getAuthToken(): string | null {
    if (typeof window !== 'undefined') {
      try {
        const storedToken = localStorage.getItem('auth_token');
        if (!storedToken) return null;
        const tokenData = JSON.parse(storedToken);
        return tokenData.accessToken;
      } catch (error) {
        console.error('Failed to retrieve auth token:', error);
        return null;
      }
    }
    return null;
  }

  private getAuthHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    const token = this.getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
  }

  private getFileUploadHeaders(): HeadersInit {
    const headers: HeadersInit = {};

    const token = this.getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    // Note: Don't set Content-Type for FormData - let browser set it with boundary

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
      let errorMessage = 'Upload failed';
      try {
        const errorData: ApiErrorResponse = await response.json();
        errorMessage = errorData.error || errorMessage;
      } catch {
        errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      }
      throw new Error(errorMessage);
    }

    return response.json();
  }

  public async listFiles(): Promise<FileListItem[]> {
    const response = await fetch(`${this.baseUrl}/file/list`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      let errorMessage = 'Failed to fetch files';
      try {
        const errorData: ApiErrorResponse = await response.json();
        errorMessage = errorData.error || errorMessage;
      } catch {
        errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      }
      throw new Error(errorMessage);
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
      let errorMessage = 'Failed to delete file';
      try {
        const errorData: ApiErrorResponse = await response.json();
        errorMessage = errorData.error || errorMessage;
      } catch {
        errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      }
      throw new Error(errorMessage);
    }
  }


  public getDownloadUrl(fileName: string): string {
    return `${this.baseUrl}/file/download/${encodeURIComponent(fileName)}`;
  }

  public async downloadFile(fileName: string): Promise<void> {
    const downloadUrl = this.getDownloadUrl(fileName);
    
    const response = await fetch(downloadUrl, {
      method: 'GET',
      headers: this.getFileUploadHeaders(), // Use auth headers for download
    });

    if (!response.ok) {
      let errorMessage = 'Failed to download file';
      if (response.status === 404) {
        errorMessage = 'File not found';
      } else if (response.status === 401) {
        errorMessage = 'Unauthorized - please login again';
      }
      throw new Error(errorMessage);
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