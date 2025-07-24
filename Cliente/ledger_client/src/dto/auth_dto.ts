export interface LoginRequest {
  email: string;
  passwordHash: string;
}

export interface RegisterRequest {
  email: string;
  passwordHash: string;
  fullName: string;
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

export interface AccessTokenResult {
  accessToken: string;
  expiresAt: number; // Epoch milliseconds
}
