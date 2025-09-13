'use client';

import { Header } from './Header';
import { Footer } from './Footer';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  return (
    <div className="min-h-screen flex flex-col transition-colors duration-500" style={{ backgroundColor: 'var(--bg-dark)' }}>
      <Header />

      {/* Main content centered and set to 80% of viewport (max 1200px) */}
      <main
        className="flex-grow py-16 transition-opacity duration-500 opacity-100"
        style={{ width: 'min(80%, 1200px)', margin: '0 auto' }}
      >
        {children}
      </main>

      <Footer />
    </div>
  );
};
