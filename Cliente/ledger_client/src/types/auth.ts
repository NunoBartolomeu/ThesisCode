export interface User {
  email: string;
  fullName: string;
}

export interface Token {
  accessToken: string;
  expiresAt: Date;
}

export interface AuthSession {
  user: User | null;
  token: Token | null;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  passwordHash: number[];
}

export interface LoginRequest {
  email: string;
  passwordHash: number[];
}

export interface VerifyCodeRequest {
  email: string;
  code: string;
}

export interface ValidateTokenRequest {
  token: string;
}

export interface SimpleAuthResult {
  email: string;
  fullName: string;
  needsVerification: Boolean;
}

export interface AuthenticatedUser {
  email: string;
  fullName: string;
  accessToken: string;
  expiresAt: number;
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

export interface FileDetailsDto {
  name: string;
  size: number;
  createdDate: number;
}

export interface UploadFileResponse {
  message: string;
  fileName: string;
}

export interface ListFilesResponse {
  files: { name: string; size: number; lastModified: number }[];
}