'use client';

import { useState, useCallback, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { AuthService } from '@/services/api/AuthService';
import { AuthValidator } from '@/services/validators/AuthValidator';
import { AuthStorageService } from '@/services/storage/AuthStorageService';
import { User, Token } from '@/models/auth';
import { hashProvider } from '@/services/crypto/HashProvider';

export function useAuthViewModel() {
  const router = useRouter();
  const authService = new AuthService();

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<Token | null>(null);

  // Initialize auth state on mount
  useEffect(() => {
    const savedUser = AuthStorageService.getUser();
    const savedToken = AuthStorageService.getToken();
    setUser(savedUser);
    setToken(savedToken);
  }, []);

  const clearErrors = useCallback(() => setErrors({}), []);

  const clearFieldError = useCallback((field: string) => {
    setErrors(prev => ({ ...prev, [field]: '' }));
  }, []);

  // ---- Login ----
  const login = useCallback(async (email: string, password: string) => {
    setLoading(true);
    setErrors({});

    try {
      const validationErrors: Record<string, string> = {};
      if (!AuthValidator.isValidEmail(email)) {
        validationErrors.email = 'Invalid email address';
      }
      if (!AuthValidator.isValidPassword(password)) {
        validationErrors.password = 'Invalid password';
      }
      
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        return { success: false, errors: validationErrors, data: null };
      }

      const passwordHash = await hashProvider.hash(password, hashProvider.getDefaultAlgorithm());
      const result = await authService.login(email, passwordHash);

      if (result.success && result.data) {
        AuthStorageService.saveUser({id: result.data.userId, fullName: result.data.fullName, email: result.data.email});
        setUser({id: result.data.userId, fullName: result.data.fullName, email: result.data.email});
      } else {
        setErrors(result.errors || { general: result.message || 'Login failed' });
      }
      
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  // ---- Register ----
  const register = useCallback(async (
    fullName: string,
    email: string,
    password: string,
    verifyPassword: string
  ) => {
    setLoading(true);
    setErrors({});

    try {
      // Validate first
      const validationErrors: Record<string, string> = {};
      if (!AuthValidator.isValidName(fullName)) {
        validationErrors.fullName = 'Name must be between 2 and 50 characters';
      }
      if (!AuthValidator.isValidEmail(email)) {
        validationErrors.email = 'Please enter a valid email address';
      }
      if (!AuthValidator.isValidPassword(password)) {
        validationErrors.password = 'Password must be at least 8 characters';
      }
      if (password !== verifyPassword) {
        validationErrors.verifyPassword = 'Passwords do not match';
      }

      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        return { success: false, errors: validationErrors, data: null };
      }

      const passwordHash = await hashProvider.hash(password, hashProvider.getDefaultAlgorithm());
      const result = await authService.register(fullName, email, passwordHash);
      
      if (result.success && result.data) {
        AuthStorageService.saveUser({id: result.data.userId, fullName: result.data.fullName, email: result.data.email});
        setUser({id: result.data.userId, fullName: result.data.fullName, email: result.data.email});
      } else {
        setErrors(result.errors || { general: result.message || 'Registration failed' });
      }
      
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  // ---- Verify 2FA ----
  const verify2FA = useCallback(async (code: string) => {
    setLoading(true);
    setErrors({});

    try {
      if (!AuthValidator.isValid2FA(code)) {
        const error = { code: 'Invalid 6-digit code' };
        setErrors(error);
        return { success: false, errors: error, data: null };
      }

      const email = AuthStorageService.getUser()?.email;
      if (!email) {
        const error = { general: 'No active session found. Please log in again.' };
        setErrors(error);
        return { success: false, errors: error, data: null };
      }

      const result = await authService.verify2FA(email, code);
      
      if (result.success && result.data) {
        const token: Token = {
          accessToken: result.data.accessToken,
          expiresAt: new Date(result.data.expiresAt * 1000),
        };
        AuthStorageService.saveToken(token);
        setToken(token);
      } else {
        setErrors(result.errors || { general: result.message || 'Verification failed' });
      }
      
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  // ---- Logout ----
  const logout = useCallback(async () => {
    const token = AuthStorageService.getToken()?.accessToken;
    if (token) {
      await authService.logout(token);
    }
    AuthStorageService.clearSession();
    setUser(null);
    setToken(null);
    router.push('/login');
  }, [router]);

  return {
    // State
    loading,
    errors,
    user,
    token,
    isAuthenticated: !!user && !!token,
    
    // Actions
    login,
    register,
    verify2FA,
    logout,
    clearErrors,
    clearFieldError,
  };
}