'use client';

interface ListingItemProps {
  title: string;
  subtitle?: string;
  icon?: React.ReactNode;
  onClick?: () => void;
  showChevron?: boolean;
}

export function ListingItem({ title, subtitle, icon, onClick, showChevron = true }: ListingItemProps) {
  const DefaultDot = () => (
    <div 
      className="w-3 h-3 rounded-full" 
      style={{ backgroundColor: 'var(--text-muted)' }}
    />
  );

  return (
    <div 
      className={`px-6 py-4 transition-all duration-200 ${onClick ? 'cursor-pointer' : ''}`}
      style={{ backgroundColor: 'transparent' }}
      onMouseEnter={(e) => {
        if (onClick) {
          e.currentTarget.style.background = 'var(--gradient-hover)';
        }
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'transparent';
      }}
      onClick={onClick}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3 flex-1 min-w-0">
          <div className="flex-shrink-0">
            {icon || <DefaultDot />}
          </div>
          <div className="min-w-0 flex-1">
            <p className="font-medium truncate" style={{ color: 'var(--text)' }}>
              {title}
            </p>
            {subtitle && (
              <p className="text-sm truncate" style={{ color: 'var(--text-muted)' }}>
                {subtitle}
              </p>
            )}
          </div>
        </div>
        {showChevron && onClick && (
          <svg className="h-5 w-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" style={{ color: 'var(--text-muted)' }}>
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        )}
      </div>
    </div>
  );
}
