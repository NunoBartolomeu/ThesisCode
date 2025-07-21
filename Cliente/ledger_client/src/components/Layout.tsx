import '../app/globals.css';
import { useEffect, useState } from 'react';
import {
  Sun,
  Moon,
  Home,
  User,
  LogIn,
  UserPlus,
  FileText,
  Book
} from 'lucide-react';
import { Button } from './Button';
import { StorageService } from '@/viewmodels/StorageService';
import { Token, User as UserT } from '@/types/auth';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  const [light, setLight] = useState(false);
  const [user, setUser] = useState<UserT | null>(null); // Use proper User type if available
  const [token, setToken] = useState<Token | null>(null); // Use proper Token type if available
  const [isClient, setIsClient] = useState(false);

  // Handle hydration by only accessing storage after component mounts
  useEffect(() => {
    setIsClient(true);
    setUser(StorageService.getUser());
    setToken(StorageService.getToken());
  }, []);

  useEffect(() => {
    if (light) {
      document.documentElement.setAttribute('data-theme', 'light');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }, [light]);

  const toggleTheme = () => {
    setLight(!light);
  };

  // Don't render navigation until we're on the client side
  const renderNavigation = () => {
    if (!isClient) {
      // Return a placeholder or minimal nav during SSR
      return (
        <div className="flex items-center space-x-6">
          <div style={{ width: '200px' }}></div> {/* Placeholder space */}
        </div>
      );
    }

    return (
      <>
        {/* No user, no token - show Home, Login, Register */}
        {!user && !token && (
          <>
            <a
              href="/"
              className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
              style={{ color: 'var(--text-muted)' }}
              onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
              onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
            >
              <Home size={18} />
              <span>Home</span>
            </a>

            <a
              href="/login"
              className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
              style={{ color: 'var(--text-muted)' }}
              onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
              onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
            >
              <LogIn size={18} />
              <span>Login</span>
            </a>
            
            <Button as="a" href="/register" className="flex items-center space-x-2">
              <UserPlus size={18} />
              <span>Register</span>
            </Button>
          </>
        )}

        {/* User but no token - show Home and user.fullName */}
        {user && !token && (
          <>
            <a
              href="/"
              className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
              style={{ color: 'var(--text-muted)' }}
              onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
              onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
            >
              <Home size={18} />
              <span>Home</span>
            </a>

            <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
              <User size={16} />
              <span className="text-sm">
                {user.fullName}
              </span>
            </div>
          </>
        )}

        {/* User and token - show Files, Ledgers and user.fullName */}
        {user && token && (
          <>
            <a
              href="/files"
              className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
              style={{ color: 'var(--text-muted)' }}
              onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
              onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
            >
              <FileText size={18} />
              <span>Files</span>
            </a>

            <a
              href="/ledgers"
              className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
              style={{ color: 'var(--text-muted)' }}
              onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
              onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
            >
              <Book size={18} />
              <span>Ledgers</span>
            </a>

            <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
              <User size={16} />
              <span className="text-sm">
                {user.fullName}
              </span>
            </div>
          </>
        )}
      </>
    );
  };

  return (
    <div className="min-h-screen transition-colors duration-500" style={{ backgroundColor: 'var(--bg-dark)' }}>
      <header
        className="shadow-sm transition-colors duration-500"
        style={{
          backgroundColor: 'var(--bg)',
          borderBottom: '1px solid var(--border-muted)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold" style={{ color: 'var(--text)' }}>
                FileManager
              </h1>
            </div>

            <nav className="flex items-center space-x-6">
              {renderNavigation()}

              <button
                onClick={toggleTheme}
                className="relative inline-flex items-center justify-center w-12 h-6 rounded-full transition-all duration-500 focus:outline-none focus:ring-2 focus:ring-offset-2"
                style={{
                  backgroundColor: 'var(--border)',
                  '--tw-ring-color': 'var(--primary)',
                  '--tw-ring-offset-color': 'var(--bg)',
                } as React.CSSProperties}
                aria-label="Toggle theme"
              >
                <span
                  className={`inline-block w-5 h-5 rounded-full shadow-md transform transition-transform duration-500 ${
                    light ? 'translate-x-3' : '-translate-x-3'
                  }`}
                  style={{ backgroundColor: 'var(--highlight)' }}
                />
                <Sun
                  size={14}
                  className={`absolute left-1 transition-opacity duration-500 ${
                    light ? 'opacity-100' : 'opacity-0'
                  }`}
                  style={{ color: 'var(--secondary)' }}
                />
                <Moon
                  size={14}
                  className={`absolute right-1 transition-opacity duration-500 ${
                    light ? 'opacity-0' : 'opacity-100'
                  }`}
                  style={{ color: 'var(--text-muted)' }}
                />
              </button>
            </nav>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 transition-opacity duration-500 opacity-100">
        {children}
      </main>

      <footer
        className="mt-auto transition-colors duration-500"
        style={{
          backgroundColor: 'var(--bg)',
          borderTop: '1px solid var(--border-muted)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="text-center" style={{ color: 'var(--text-muted)' }}>
            <p>2025, Nuno Bartolomeu.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};