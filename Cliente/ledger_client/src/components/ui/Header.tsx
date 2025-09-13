'use client';

import { useEffect, useState } from 'react';
import {
  Sun,
  Moon,
  Home,
  User,
  LogIn,
  UserPlus,
  FileText,
  Book,
  LogOut,
  Shield
} from 'lucide-react';
import { Button } from './Button';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';

export const Header = () => {
  const { user, token, isAuthenticated, logout } = useAuthViewModel();
  const [light, setLight] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (light) document.documentElement.setAttribute('data-theme', 'light');
    else document.documentElement.removeAttribute('data-theme');
  }, [light]);

  return (
    <header
      className="shadow-sm transition-colors duration-500"
      style={{
        backgroundColor: 'var(--bg)',
        borderBottom: '1px solid var(--border-muted)',
      }}
    >
      {/* Container set to 80% of viewport (max 1200px) and centered */}
      <div className="mx-auto px-4 sm:px-6 lg:px-8" style={{ width: 'min(80%, 1200px)', margin: '0 auto' }}>
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center">
            <h1 className="text-xl font-bold" style={{ color: 'var(--text)' }}>
              FileManager
            </h1>
          </div>

          <nav className="flex items-center space-x-6 flex-wrap">
            {!user && !token && (
              <>
                <a href="/" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <Home size={18} />
                  <span>Home</span>
                </a>

                <a href="/login" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <LogIn size={18} />
                  <span>Login</span>
                </a>

                <Button as="a" href="/register" className="flex items-center space-x-2">
                  <UserPlus size={18} />
                  <span>Register</span>
                </Button>
              </>
            )}

            {user && !token && (
              <>
                <a href="/" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <Home size={18} />
                  <span>Home</span>
                </a>

                <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
                  <User size={16} />
                  <span className="text-sm">{user.fullName}</span>
                </div>
              </>
            )}

            {isAuthenticated && (
              <>
                <a href="/files" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <FileText size={18} />
                  <span>Files</span>
                </a>

                <a href="/ledgers" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <Book size={18} />
                  <span>Ledgers</span>
                </a>

                <a href="/pki" className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80" style={{ color: 'var(--text-muted)' }}>
                  <Shield size={18} />
                  <span>PKI</span>
                </a>

                <div className="flex items-center space-x-2" style={{ color: 'var(--text-muted)' }}>
                  <User size={16} />
                  <span className="text-sm">{user?.fullName}</span>
                </div>

                <button
                  onClick={logout}
                  className="flex items-center space-x-2 transition-colors duration-500 hover:opacity-80"
                  style={{ color: 'var(--text-muted)' }}
                >
                  <LogOut size={18} />
                  <span>Logout</span>
                </button>
              </>
            )}

            <button
              onClick={() => setLight(l => !l)}
              className="relative inline-flex items-center justify-center w-12 h-6 rounded-full transition-all duration-500 focus:outline-none"
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
                className={`absolute left-1 transition-opacity duration-500 ${light ? 'opacity-100' : 'opacity-0'}`}
                style={{ color: 'var(--secondary)' }}
              />
              <Moon
                size={14}
                className={`absolute right-1 transition-opacity duration-500 ${light ? 'opacity-0' : 'opacity-100'}`}
                style={{ color: 'var(--text-muted)' }}
              />
            </button>
          </nav>
        </div>
      </div>
    </header>
  );
};