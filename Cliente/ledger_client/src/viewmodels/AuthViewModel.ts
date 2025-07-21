import { useState, useCallback, useEffect, use } from 'react';
import { User, AuthToken, AuthState, ApiResponse, RegisterRequest, LoginRequest, VerifyCodeRequest, ValidateTokenRequest, SimpleAuthResult, AuthenticatedUser } from '@/types/auth';


// ============================================
// VALIDATION FUNCTIONS - Validate user input
// ============================================

export const isValidName = (name: string): boolean => {
  if (!name.trim()) return false;
  if (name.trim().length < 2) return false;
  if (name.trim().length > 50) return false;
  return true;
};

export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email.trim());
};

export const isValidPassword = (password: string): boolean => {
  if (!password) return false;
  if (password.length < 8) return false;
  if (password.length > 128) return false;
  return true;
};

export const isValid2FA = (code: string): boolean => {
  return /^\d{6}$/.test(code.trim());
};

// ====================
// UTILITY FUNCTIONS
// ====================

const sha256 = async (message: string): Promise<string> => {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};

const convertToByteArray = (hashString: string): number[] => {
  const bytes = [];
  for (let i = 0; i < hashString.length; i += 2) {
    bytes.push(parseInt(hashString.substr(i, 2), 16));
  }
  return bytes;
};

const debugLog = (msg: string, level: string) => {
  console.log("[" + level + "] - " + msg)
} 

// ==========================================
// AUTH REQUESTER - Handles all API calls
// ==========================================

export class AuthRequester {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  // Generic API request method
  private async makeRequest<T>(
    endpoint: string,
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    body?: any,
    includeAuth: boolean = false,
  ): Promise<ApiResponse<T>> {
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (includeAuth) {
        const token = this.getStoredToken();
        if (token?.accessToken) {
          headers['Authorization'] = `Bearer ${token.accessToken}`;
        }
      }

      const url = `${this.baseUrl}${endpoint}`;
      const requestConfig = {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      };

      debugLog('Making request to: ' + url, 'DEBUG');

      const response = await fetch(url, requestConfig);

      debugLog('Response status:' + response.status, 'DEBUG');

      if (!response.ok) {
        let errorText = '';
        try {
          errorText = await response.text();
          debugLog('Error response body:' + errorText, 'WARN');
        } catch (e) {
          debugLog('Could not read error response body:' + e, 'WARN');
        }
        
        return {
          success: false,
          data: null,
          message: `HTTP ${response.status}: ${response.statusText}${errorText ? ' - ' + errorText : ''}`,
        };
      }

      const data = await response.json();
      debugLog('Response parsed data:' + JSON.stringify(data), 'DEBUG');
      
      return {
        success: true,
        data,
      };
    } catch (error) {
      debugLog('API Request failed with exception:' + error, 'DEBUG');
      return {
        success: false,
        data: null,
        message: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

    
  // ===================
  // AUTH API ENDPOINTS
  // ===================

  async register(request: RegisterRequest): Promise<ApiResponse<SimpleAuthResult>> {
    return this.makeRequest<SimpleAuthResult>('/auth/register', 'POST', request);
  }

  async login(request: LoginRequest): Promise<ApiResponse<SimpleAuthResult>> {
    return this.makeRequest<SimpleAuthResult>('/auth/login', 'POST', request);
  }

  async verify2FA(request: VerifyCodeRequest): Promise<ApiResponse<AuthenticatedUser>> {
    return this.makeRequest<AuthenticatedUser>('/auth/verify', 'POST', request, false);
  }

  async validateToken(request: ValidateTokenRequest): Promise<ApiResponse<AuthenticatedUser>> {
    return this.makeRequest<AuthenticatedUser>('/auth/validate', 'POST', request, true);
  }

  async logout(request: ValidateTokenRequest): Promise<ApiResponse<any>> {
    return this.makeRequest('/auth/logout', 'POST', request, true);
  }

  // Token management utilities
  getStoredToken(): AuthToken | null {
    try {
      const storedToken = localStorage.getItem('auth_token');
      if (!storedToken) return null;
      return JSON.parse(storedToken);
    } catch (error) {
      console.error('Failed to retrieve tokens:', error);
      return null;
    }
  }
}

// ===================================================
// AUTH VIEW MODEL - Handles business logic and state
// ===================================================

export class AuthViewModel {
  private setAuthState: React.Dispatch<React.SetStateAction<AuthState>>;
  private requester: AuthRequester;

  constructor(baseUrl: string, setAuthState: React.Dispatch<React.SetStateAction<AuthState>>) {
    this.setAuthState = setAuthState;
    this.requester = new AuthRequester(baseUrl);
  }

  private STORAGE_USER  = 'auth_user' 
  private STORAGE_TOKEN = 'auth_token' 

  // ========================
  // USER MANAGEMENT
  // ========================
  private storeUser = (user: User): void => {
    try {
      localStorage.setItem(this.STORAGE_USER, JSON.stringify(user));
    } catch (error) {
      console.error('Failed to store auth session:', error);
    }
  };

  private getStoredUser = (): User | null => {
    try {
      const storedUser = localStorage.getItem(this.STORAGE_USER);
      if (!storedUser) return null;
      return JSON.parse(storedUser);
    } catch (error) {
      console.error('Failed to retrieve auth session:', error);
      return null;
    }
  };

  private clearUser = (): void => {
    try {
      localStorage.removeItem(this.STORAGE_USER);
    } catch (error) {
      console.error('Failed to clear auth session:', error);
    }
  };

  // ==========================================
  // TOKEN MANAGEMENT
  // ==========================================
  private storeToken = (token: AuthToken): void => {
    try {
      localStorage.setItem(this.STORAGE_TOKEN, JSON.stringify(token));
      debugLog("Successfully stored token: " + token, "STORAGE")
    } catch (error) {
      console.error('Failed to store tokens:', error);
    }
  };

    getStoredToken(): AuthToken | null {
    try {
      const storedToken = localStorage.getItem(this.STORAGE_TOKEN);
      if (!storedToken) return null;
      return JSON.parse(storedToken);
    } catch (error) {
      console.error('Failed to retrieve tokens:', error);
      return null;
    }
  }

  private clearToken = (): void => {
    try {
      localStorage.removeItem(this.STORAGE_TOKEN);
    } catch (error) {
      console.error('Failed to clear tokens:', error);
    }
  };

  isTokenExpired = (): boolean => {
    const token = this.getStoredToken();
    debugLog("Token JSON: " + JSON.stringify(token), 'EXPIRED')
    if (!token) { 
      return true;
    }
    let res = new Date() >= token.expiresAt;
    debugLog("Is it really expired? " + res, "EXPIRED")
    return res
  };

  // ==========================================
  // AUTH BUSINESS LOGIC
  // ==========================================
  async register(fullName: string, email: string, password: string, verifyPassword: string): Promise<ApiResponse<SimpleAuthResult>> {
    debugLog('Starting registration process', 'DEBUG');

    // Input validation
    const errors: Record<string, string> = {};
    if (!isValidName(fullName)) errors.fullName = "Name must be between 2 and 50 characters";
    if (!isValidEmail(email)) errors.email = "Please enter a valid email address";
    if (!isValidPassword(password)) errors.password = "Password must be at least 8 characters long";
    if (password !== verifyPassword) errors.verifyPassword = "Passwords do not match";

    if (Object.keys(errors).length > 0) {
      debugLog('Failed input validation', 'WARN');
      return { success: false, errors, data: null };
    }

    const passwordHash = await sha256(password);
    const request: RegisterRequest = {
      fullName,
      email,
      passwordHash: convertToByteArray(passwordHash) as any,
    };

    const response = await this.requester.register(request);
    
    if (response.success && response.data) {
      this.handleAuthSuccess(response.data, false);
    }

    return response;
  }

  async login(email: string, password: string): Promise<ApiResponse<SimpleAuthResult>> {
    debugLog('Starting login process', 'DEBUG');

    // Input validation
    const errors: Record<string, string> = {};
    if (!isValidEmail(email)) errors.email = "Please enter a valid email address";
    if (!isValidPassword(password)) errors.password = "Please enter your password";

    if (Object.keys(errors).length > 0) {
      debugLog('Failed input validation', 'WARN');
      return { success: false, errors, data: null };
    }

    const passwordHash = await sha256(password);
    const request: LoginRequest = {
      email,
      passwordHash: convertToByteArray(passwordHash) as any,
    };

    const response = await this.requester.login(request);
    
    if (response.success && response.data) {
      this.handleAuthSuccess(response.data, false);
    }

    return response;
  }

  async verify2FA(code: string): Promise<ApiResponse<AuthenticatedUser>> {
    debugLog('Starting 2fa process with code: ' + code, 'DEBUG');
    const user = this.getStoredUser();
    const token = this.getStoredToken();
    
    // Validation
    // If no user = no login
    // If token = 2fa already complete
    // If user but no token = needs 2fa
    if (!user) {
      debugLog('No user found', 'WARN')
      return {
        success: false,
        data: null,
        message: "No active authentication session found. Please login again.",
      };
    }
    if(token) {
      debugLog('User already logged in', 'WARN')
      return {
        success: false,
        data: null,
        message: "User is already logged in. Please go to home page.",
      };
    }
    const errors: Record<string, string> = {};
    
    if (!isValid2FA(code)) {
      debugLog('Invalid 2FA code format', 'WARN');
      errors.code = "Please enter a valid 6-digit code";
    }

    if (Object.keys(errors).length > 0) {
      return { success: false, errors, data: null };
    }

    const request: VerifyCodeRequest = {
      email: user.email,
      code,
    };

    this.setAuthState(prev => ({ ...prev, isLoading: true }));

    const response = await this.requester.verify2FA(request);

    if (response.success && response.data) {
      this.handleAuthSuccess(response.data, true);
    } else {
      this.setAuthState(prev => ({ ...prev, isLoading: false }));
    }

    return response;
  }

  async validateToken(token?: string): Promise<boolean> {
    debugLog('Validating token', 'DEBUG');

    const tokenToValidate = token || this.getStoredToken()?.accessToken;
    if (!tokenToValidate) {
      return false;
    }

    const request: ValidateTokenRequest = {
      token: tokenToValidate,
    };

    const response = await this.requester.validateToken(request);
    
    if (response.success && response.data) {
      this.handleAuthSuccess(response.data, true);
      return true;
    }

    return false;
  }

  async logout(): Promise<void> {
    debugLog('Starting logout', 'DEBUG');

    try {
      const tokens = this.getStoredToken();
      if (tokens?.accessToken) {
        const request: ValidateTokenRequest = {
          token: tokens.accessToken,
        };
        await this.requester.logout(request);
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      this.clearToken();
      this.clearUser();
      this.setAuthState({
        user: null,
        token: null,
        isLoading: false,
      });
    }
  }

  // ================
  // HELPER METHODS
  // ================

  private handleAuthSuccess(data: SimpleAuthResult | AuthenticatedUser, hasToken: boolean): void {
    debugLog('Handling successful authentication, hasToken: ' + hasToken, 'DEBUG')
    if (hasToken && 'accessToken' in data) {
      // Full authentication (from 2FA or token validation)
      debugLog('Data from auth success:' + JSON.stringify(data), 'DEBUG')

      const token: AuthToken = {
        accessToken: data.accessToken,
        expiresAt: new Date(data.expiresAt * 1000),
      };

      debugLog('Created token:' + JSON.stringify(token), 'DEBUG')
      this.storeToken(token);
      debugLog('New stored token:' + JSON.stringify(this.getStoredToken()), 'DEBUG')

      const user: User = {
        email: data.email,
        fullName: data.fullName,
      };

      const storedUser = this.getStoredUser()

      if(user.email != storedUser?.email || user.fullName != storedUser.fullName) {
        debugLog('Stored user does not match requerst user', 'WARN')
      }

      this.setAuthState({
        user: user,
        token: token,
        isLoading: false
      });

      debugLog('Auth state updated', 'DEBUG')
    } else {
      // Partial authentication (from register/login) - needs 2FA
      const user = {
        email: data.email,
        fullName: data.fullName,
      };

      this.storeUser(user);

      this.setAuthState({
        user: user,
        token: null,
        isLoading: false
      });
    }
  }

  // ==========================================
  // INITIALIZATION
  // ==========================================
  async initializeAuth(): Promise<void> {
    debugLog('Initializing authentication state', 'DEBUG');

    const token = this.getStoredToken();  
    const user = this.getStoredUser();

    this.setAuthState(_ => ({ token: token, user: user, isLoading: true }));
    
    debugLog("user: " + JSON.stringify(user), "INIT")
    debugLog("token: " + JSON.stringify(token), "INIT")

    try {
      // Check for existing valid tokens (user is already authenticated)
      if (token) {
        if(!this.isTokenExpired() && await this.validateToken(token.accessToken)){
          return;
        } else {
            debugLog("Clearing token because it was not valid, or expired", "WARN")
            this.clearToken();
            this.clearUser();
        }
      }

      // If it gets here there is no token, either because it wasn't there or because it was expired/not valid
      if(user) {
        debugLog('User is logged in, waiting for 2FA', 'DEBUG')
        this.setAuthState({
          user,
          token: null,
          isLoading: false
        })
        return;
      }
      
      // If it gets here there is no user and no token
      this.setAuthState({
        user: null,
        token: null,
        isLoading: false
      })
    } catch (error) {
      debugLog('Failed to initialize auth:' + error, 'WARN');
      this.clearToken();
      this.clearUser();
      this.setAuthState({
        user: null,
        token: null,
        isLoading: false
      });
    }
  }
}

// =====
// HOOK
// =====
export const useAuthViewModel = (baseUrl: string = 'http://localhost:8080') => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    token: null,
    isLoading: true,
  });

  const viewModel = new AuthViewModel(baseUrl, setAuthState);

  // Initialize auth state on mount
  useEffect(() => {
    viewModel.initializeAuth();
  }, []);

  // Auto-logout when tokens expire
  useEffect(() => {
    if (authState.token && viewModel.isTokenExpired()) {
      debugLog('Auto logout activated', 'INFO')
      debugLog('token:' + JSON.stringify(authState.token), 'DEBUG')
      debugLog('Expired:' + viewModel.isTokenExpired(), 'DEBUG')
      viewModel.logout();
    }
  }, [authState.token]);

  return {
    authState,
    viewModel,
  };
};