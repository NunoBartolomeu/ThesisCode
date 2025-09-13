'use client';

interface ListingContainerProps {
  title: string;
  count: number;
  headerIcon?: React.ReactNode;
  children: React.ReactNode;
  actions?: React.ReactNode;
}

export function ListingContainer({ title, count, headerIcon, children, actions }: ListingContainerProps) {
  return (
    <div
      className="rounded-2xl shadow-xl border-2 overflow-hidden transition-all duration-200"
      style={{
        backgroundColor: 'var(--bg)',
        background: 'var(--gradient)',
        borderColor: 'var(--border)'
      }}
    >
      <div className="px-6 py-4 border-b-2 flex items-center justify-between" style={{ borderColor: 'var(--border)' }}>
        <div className="flex items-center space-x-3">
          {headerIcon}
          <h2 className="text-xl font-bold" style={{ color: 'var(--text)' }}>
            {title} ({count})
          </h2>
        </div>
        {actions && (
          <div className="flex items-center space-x-2">
            {actions}
          </div>
        )}
      </div>
      {children}
    </div>
  );
}