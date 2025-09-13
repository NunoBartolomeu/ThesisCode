import { AuthStorageService } from "@/services/storage/AuthStorageService";

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  errors?: Record<string, string>;
}

export class Fetcher {
  private baseUrl: string;

  constructor(baseUrl: string = 'http://localhost:8080') {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  private getToken(): string | null {
    if (typeof window === 'undefined') return null;
    
    try {
      const token = AuthStorageService.getToken()
      if (!token) return null;
      return token.accessToken;
    } catch {
      return null;
    }
  }

  async request<T>(
    endpoint: string,
    options: {
      method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
      body?: any;
      headers?: Record<string, string>;
      requireAuth?: boolean;
      isFormData?: boolean;
      responseType?: 'json' | 'blob';
    } = {}
  ): Promise<ApiResponse<T>> {
    const {
      method = 'GET',
      body,
      headers = {},
      requireAuth = false,
      isFormData = false,
      responseType = 'json', // default to JSON
    } = options;

    try {
      const requestHeaders: Record<string, string> = { ...headers };

      if (!isFormData && responseType === 'json') {
        requestHeaders['Content-Type'] = 'application/json';
      }

      if (requireAuth) {
        const token = this.getToken();
        if (token) {
          requestHeaders['Authorization'] = `Bearer ${token}`;
        }
      }

      const config: RequestInit = {
        method,
        headers: requestHeaders,
      };

      if (body) {
        config.body = isFormData ? body : JSON.stringify(body);
      }

      const response = await fetch(`${this.baseUrl}${endpoint}`, config);

      if (!response.ok) {
        const errorText = await response.text();
        return {
          success: false,
          data: null,
          message: `HTTP ${response.status}: ${response.statusText} - ${errorText}`,
        };
      }

      let data: T | null = null;
      if (responseType === 'blob') {
        data = (await response.blob()) as unknown as T;
      } else {
        // JSON (default)
        const rawText = await response.text();
        try {
          data = JSON.parse(rawText);
        } catch {
          data = null;
        }
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      return {
        success: false,
        data: null,
        message: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }
}