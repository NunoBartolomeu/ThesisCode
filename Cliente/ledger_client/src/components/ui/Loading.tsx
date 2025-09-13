interface LoadingProps {
  message?: string;
  title?: string;
}

export function Loading({ message = "Loading...", title }: LoadingProps) {
  return (
    <div className="flex items-center justify-center p-1 pb-20">
      <div className="w-4/5 max-w-6xl space-y-8">
        {title && (
          <h1 className="text-4xl font-bold text-center mb-8" style={{ color: 'var(--text)' }}>
            {title}
          </h1>
        )}
        <div className="p-12 text-center rounded-2xl border-2" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
          <p className="text-xl font-medium" style={{ color: 'var(--text-muted)' }}>
            {message}
          </p>
        </div>
      </div>
    </div>
  );
}