import { Fetcher, ApiResponse } from '@/viewmodels/Fetcher';
import { StorageService } from './StorageService';
import {
  RegisterRequest,
  LoginRequest,
  VerifyCodeRequest,
  ValidateTokenRequest,
  SimpleAuthResult,
  AuthenticatedUser,
  User,
  Token,
} from '@/types/auth';

export class AuthService {
  private fetcher: Fetcher;

  constructor(baseUrl?: string) {
    this.fetcher = new Fetcher(baseUrl);
  }

  private async hashPassword(password: string): Promise<number[]> {
    const encoder = new TextEncoder();
    const data = encoder.encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(hashBuffer));
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  }

  private isValidPassword(password: string): boolean {
    return password.length >= 8 && password.length <= 128;
  }

  private isValidName(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= 2 && trimmed.length <= 50;
  }

  private isValid2FA(code: string): boolean {
    return /^\d{6}$/.test(code.trim());
  }

  async register(
    fullName: string,
    email: string,
    password: string,
    verifyPassword: string
  ): Promise<ApiResponse<SimpleAuthResult>> {
    const errors: Record<string, string> = {};

    if (!this.isValidName(fullName)) {
      errors.fullName = 'Name must be between 2 and 50 characters';
    }
    if (!this.isValidEmail(email)) {
      errors.email = 'Please enter a valid email address';
    }
    if (!this.isValidPassword(password)) {
      errors.password = 'Password must be at least 8 characters long';
    }
    if (password !== verifyPassword) {
      errors.verifyPassword = 'Passwords do not match';
    }

    if (Object.keys(errors).length > 0) {
      return { success: false, errors, data: null };
    }

    const passwordHash = await this.hashPassword(password);
    const request: RegisterRequest = { fullName, email, passwordHash };

    const response = await this.fetcher.request<SimpleAuthResult>('/auth/register', {
      method: 'POST',
      body: request,
    });

    if (response.success && response.data) {
      StorageService.saveUser({
        email: response.data.email,
        fullName: response.data.fullName,
      });
    }

    return response;
  }

  async login(email: string, password: string): Promise<ApiResponse<SimpleAuthResult>> {
    const errors: Record<string, string> = {};

    if (!this.isValidEmail(email)) {
      errors.email = 'Please enter a valid email address';
    }
    if (!this.isValidPassword(password)) {
      errors.password = 'Please enter your password';
    }

    if (Object.keys(errors).length > 0) {
      return { success: false, errors, data: null };
    }

    const passwordHash = await this.hashPassword(password);
    const request: LoginRequest = { email, passwordHash };

    const response = await this.fetcher.request<SimpleAuthResult>('/auth/login', {
      method: 'POST',
      body: request,
    });

    if (response.success && response.data) {
      StorageService.saveUser({
        email: response.data.email,
        fullName: response.data.fullName,
      });
    }

    return response;
  }

  async verify2FA(code: string): Promise<ApiResponse<AuthenticatedUser>> {
    const user = StorageService.getUser();
    
    if (!user) {
      return {
        success: false,
        data: null,
        message: 'No active authentication session found. Please login again.',
      };
    }

    const errors: Record<string, string> = {};
    if (!this.isValid2FA(code)) {
      errors.code = 'Please enter a valid 6-digit code';
    }

    if (Object.keys(errors).length > 0) {
      return { success: false, errors, data: null };
    }

    const request: VerifyCodeRequest = { email: user.email, code };

    const response = await this.fetcher.request<AuthenticatedUser>('/auth/verify', {
      method: 'POST',
      body: request,
    });

    if (response.success && response.data) {
      const token: Token = {
        accessToken: response.data.accessToken,
        expiresAt: new Date(response.data.expiresAt * 1000),
      };
      StorageService.saveToken(token);
    }

    return response;
  }

  async validateToken(token?: string): Promise<boolean> {
    const tokenToValidate = token || StorageService.getToken()?.accessToken;
    if (!tokenToValidate) return false;

    const request: ValidateTokenRequest = { token: tokenToValidate };

    const response = await this.fetcher.request<AuthenticatedUser>('/auth/validate', {
      method: 'POST',
      body: request,
      requireAuth: true,
    });

    if (response.success && response.data) {
      const token: Token = {
        accessToken: response.data.accessToken,
        expiresAt: new Date(response.data.expiresAt * 1000),
      };
      StorageService.saveToken(token);
      StorageService.saveUser({
        email: response.data.email,
        fullName: response.data.fullName,
      });
      return true;
    }

    return false;
  }

  async logout(): Promise<void> {
    const token = StorageService.getToken();
    if (token?.accessToken) {
      const request: ValidateTokenRequest = { token: token.accessToken };
      await this.fetcher.request('/auth/logout', {
        method: 'POST',
        body: request,
        requireAuth: true,
      });
    }
    StorageService.clearSession();
  }
}