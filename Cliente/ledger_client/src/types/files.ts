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

