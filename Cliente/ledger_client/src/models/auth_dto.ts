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
  userId: string;
  email: string;
  fullName: string;
}

export interface AccessTokenResult {
  accessToken: string;
  expiresAt: number;
}
