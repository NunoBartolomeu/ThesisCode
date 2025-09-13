import { User, Token, AuthSession } from '@/models/auth'

export class AuthStorageService {
  private static USER_KEY = 'auth_user';
  private static TOKEN_KEY = 'auth_token';

  static saveUser(user: User): void {
    if (typeof window === 'undefined') return;
    try { localStorage.setItem(this.USER_KEY, JSON.stringify(user)); } catch (e) { console.error('Failed to save user:', e); }
  }

  static getUser(): User | null {
    if (typeof window === 'undefined') return null;
    try {
      const stored = localStorage.getItem(this.USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch (e) { console.error('Failed to get user:', e); return null; }
  }

  static saveToken(token: Token): void {
    if (typeof window === 'undefined') return;
    try { localStorage.setItem(this.TOKEN_KEY, JSON.stringify(token)); } catch (e) { console.error('Failed to save token:', e); }
  }

  static getToken(): Token | null {
    if (typeof window === 'undefined') return null;
    try {
      const stored = localStorage.getItem(this.TOKEN_KEY);
      if (!stored) return null;
      const token = JSON.parse(stored);
      token.expiresAt = new Date(token.expiresAt);
      return token;
    } catch (e) { console.error('Failed to get token:', e); return null; }
  }

  static getSession(): AuthSession {
    return { user: this.getUser(), token: this.getToken() };
  }

  static clearSession(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(this.USER_KEY);
      localStorage.removeItem(this.TOKEN_KEY);
    } catch (e) { console.error('Failed to clear session:', e); }
  }

  static isTokenExpired(token: Token | null): boolean {
    if (!token) return true;
    return new Date() >= token.expiresAt;
  }

}