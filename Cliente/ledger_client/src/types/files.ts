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
  id: string;
  name: string;
  size: number;
  formattedSize: string;
  fileType: string;
  icon: string;
}

export interface FileMetadataData {
  id: string;
  name: string;
  size: number;
  formattedSize: string;
  fileType: string;
  icon: string;
  contentType?: string;
  uploaderId: string;
  senders: string[];
  receivers: string[];
  ledgerEntries: string[]; // ⚠️ Consider: Internal ledger references
  downloadUrl: string;
}
