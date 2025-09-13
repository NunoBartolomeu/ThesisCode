import { Fetcher, ApiResponse } from "./Fetcher";
import {
  RegisterRequest,
  LoginRequest,
  VerifyCodeRequest,
  ValidateTokenRequest,
  SimpleAuthResult,
  AccessTokenResult,
} from "@/models/auth_dto";

export class AuthService {
  private baseUrl = "/auth";
  private fetcher = new Fetcher();

  async register(fullName: string, email: string, passwordHash: string): Promise<ApiResponse<SimpleAuthResult>> {
    const request: RegisterRequest = { fullName, email, passwordHash };
    return this.fetcher.request<SimpleAuthResult>(`${this.baseUrl}/register`, {
      method: "POST",
      body: request,
    });
  }

  async login(email: string, passwordHash: string): Promise<ApiResponse<SimpleAuthResult>> {
    const request: LoginRequest = { email, passwordHash };
    return this.fetcher.request<SimpleAuthResult>(`${this.baseUrl}/login`, {
      method: "POST",
      body: request,
    });
  }

  async verify2FA(email: string, code: string): Promise<ApiResponse<AccessTokenResult>> {
    const request: VerifyCodeRequest = { email, code };
    return this.fetcher.request<AccessTokenResult>(`${this.baseUrl}/verify`, {
      method: "POST",
      body: request,
    });
  }

  async validateToken(token: string): Promise<boolean> {
    const request: ValidateTokenRequest = { token };
    const response = await this.fetcher.request<Boolean>(
      `${this.baseUrl}/validate`,
      {
        method: "POST",
        body: request,
        requireAuth: true,
      }
    );
    return response.success && !!response.data && response.data.valueOf();
  }

  async logout(token: string): Promise<void> {
    const request: ValidateTokenRequest = { token };
    await this.fetcher.request(`${this.baseUrl}/logout`, {
      method: "POST",
      body: request,
      requireAuth: true,
    });
  }
}
