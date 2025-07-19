import '../app/globals.css';
import { useEffect, useState } from 'react';
import {
  Sun,
  Moon,
  Home,
  User,
  LogIn,
  UserPlus,
  Shield,
  LogOut,
  Loader2
} from 'lucide-react';
import { Button } from './Button'; // adjust the path if needed
import { useAuthViewModel } from '@/viewmodels/AuthViewModel';

interface LayoutProps {
  children: React.ReactNode;
  baseUrl?: string; // Optional prop to override the default auth API URL
}

export const Layout = ({ children, baseUrl = 'http://localhost:8080' }: LayoutProps) => {
  const [light, setLight] = useState(false);
  const { authState, viewModel } = useAuthViewModel(baseUrl);
  const { user, token, isLoading } = authState;

  // Check if user is fully authenticated (has both user and token)
  const isAuthenticated = !!(user && token);
  
  // Check if user is waiting for 2FA (has user but no token)
  const needsTwoFA = !!(user && !token);

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



  return (
    <div className="min-h-screen transition-colors duration-500" style={{ backgroundColor: 'var(--bg-dark)' }}>
      {/* Header */}
      <header
        className="shadow-sm transition-colors duration-500"
        style={{
          backgroundColor: 'var(--bg)',
          borderBottom: '1px solid var(--border-muted)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo/Brand */}
            <div className="flex items-center">
              <h1 className="text-xl font-bold" style={{ color: 'var(--text)' }}>
                FileManager
              </h1>
            </div>

            {/* Desktop Navigation */}
            <nav className="flex items-center space-x-6">
              {/* Show loading spinner during auth initialization */}
              {isLoading ? (
                <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Loading...</span>
                </div>
              ) : isAuthenticated ? (
                // Fully authenticated user navigation (user + token present)
                <>
                  <a
                    href="/files"
                    className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
                    style={{ color: 'var(--text-muted)' }}
                    onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
                    onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
                  >
                    <Home size={18} />
                    <span>Files</span>
                  </a>
                  
                  {/* User info display */}
                  <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
                    <User size={16} />
                    <span className="text-sm">
                      {user.fullName || user.email}
                    </span>
                  </div>

                  {/* Logout link */}
                  <a
                    href="/logout"
                    className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
                    style={{ color: 'var(--text-muted)' }}
                    onMouseEnter={(e) => (e.target as HTMLElement).style.color = 'var(--primary)'}
                    onMouseLeave={(e) => (e.target as HTMLElement).style.color = 'var(--text-muted)'}
                  >
                    <LogOut size={18} />
                    <span>Logout</span>
                  </a>
                </>
              ) : (
                // Not logged in (includes users without token who need 2FA)
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

              {/* Theme Toggle */}
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

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 transition-opacity duration-500 opacity-100">
        {children}
      </main>

      {/* Footer */}
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