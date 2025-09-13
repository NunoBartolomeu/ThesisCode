'use client';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
}

export function EmptyState({ icon, title, description }: EmptyStateProps) {
  return (
    <div className="p-12 text-center">
      {icon && (
        <div className="mx-auto h-16 w-16 mb-4 flex items-center justify-center">
          {icon}
        </div>
      )}
      <p className="text-xl font-medium mb-2" style={{ color: 'var(--text-muted)' }}>
        {title}
      </p>
      {description && (
        <p style={{ color: 'var(--text-muted)' }}>
          {description}
        </p>
      )}
    </div>
  );
}