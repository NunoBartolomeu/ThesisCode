import { createContext, useContext, useEffect, useState } from 'react';
import { StorageService } from './StorageService';
import { AuthService } from './AuthService';
import { AuthSession } from '@/types/auth';

interface SessionContextType {
  session: AuthSession;
  isLoading: boolean;
  updateSession: () => void;
  logout: () => Promise<void>;
}

const SessionContext = createContext<SessionContextType | undefined>(undefined);

export const SessionProvider = ({ children }: { children: React.ReactNode }) => {
  const [session, setSession] = useState<AuthSession>({ user: null, token: null });
  const [isLoading, setIsLoading] = useState(true);
  const authService = new AuthService();

  const updateSession = () => {
    const newSession = StorageService.getSession();
    setSession(newSession);
  };

  const logout = async () => {
    await authService.logout();
    setSession({ user: null, token: null });
  };

  useEffect(() => {
    const initializeSession = async () => {
      const storedSession = StorageService.getSession();
      
      if (storedSession.token && !StorageService.isTokenExpired(storedSession.token)) {
        const isValid = await authService.validateToken();
        if (isValid) {
          updateSession();
        } else {
          StorageService.clearSession();
        }
      } else if (storedSession.token && StorageService.isTokenExpired(storedSession.token)) {
        StorageService.clearSession();
      } else {
        setSession(storedSession);
      }
      
      setIsLoading(false);
    };

    initializeSession();
  }, []);

  return (
    <SessionContext.Provider value={{ session, isLoading, updateSession, logout }}>
      {children}
    </SessionContext.Provider>
  );
};

export const useSession = () => {
  const context = useContext(SessionContext);
  if (context === undefined) {
    throw new Error('useSession must be used within a SessionProvider');
  }
  return context;
};
