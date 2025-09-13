'use client';

interface PageContainerProps {
  title: string;
  onGoBack?: () => void;
  children: React.ReactNode;
}

export function PageContainer({ title, onGoBack, children }: PageContainerProps) {
  return (
    <div className="flex items-center justify-center p-1 pb-20">
      <div className="w-4/5 max-w-6xl space-y-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl font-bold" style={{ color: 'var(--text)' }}>
            {title}
          </h1>
          {onGoBack && (
            <button 
              onClick={onGoBack}
              className="px-4 py-2 rounded-lg transition-all duration-200 hover:shadow-md"
              style={{ 
                backgroundColor: 'var(--border)', 
                color: 'var(--text)',
                border: '1px solid var(--border)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--gradient-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--border)';
              }}
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </button>
          )}
        </div>
        {children}
      </div>
    </div>
  );
}