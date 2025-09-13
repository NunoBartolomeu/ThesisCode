'use client';

export const Footer = () => {
  return (
    <footer
      className="mt-auto transition-colors duration-500"
      style={{
        backgroundColor: 'var(--bg)',
        borderTop: '1px solid var(--border-muted)',
      }}
    >
      <div className="mx-auto px-4 sm:px-6 lg:px-8" style={{ width: 'min(80%, 1200px)', margin: '0 auto' }}>
        <div className="text-center" style={{ color: 'var(--text-muted)' }}>
          <p>2025, Nuno Bartolomeu.</p>
        </div>
      </div>
    </footer>
  );
};
