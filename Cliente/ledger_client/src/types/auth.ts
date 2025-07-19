// Types
export interface User {
  email: string;
  fullName: string;
}

export interface AuthToken {
  accessToken: string;
  expiresAt: Date;
}

export interface AuthState {
  user: User | null;
  token: AuthToken | null;
  isLoading: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  errors?: Record<string, string>;
  message?: string;
}

// API Request/Response types
export interface RegisterRequest {
  email: string;
  passwordHash: string;
  fullName: string;
}

export interface LoginRequest {
  email: string;
  passwordHash: string;
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
  needsVerification: boolean;
}

export interface AuthenticatedUser {
  email: string;
  fullName: string;
  accessToken: string;
  expiresAt: number;
}
