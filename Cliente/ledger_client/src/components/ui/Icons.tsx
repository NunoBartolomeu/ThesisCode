'use client';

export const Icons = {
  Ledger: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--primary)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
    </svg>
  ),

  Page: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--primary)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  ),

  Entry: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--success)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
    </svg>
  ),

  VerifiedEntry: ({ className = "h-5 w-5", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--success)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),

  UnverifiedEntry: ({ className = "h-5 w-5", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--warning)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
    </svg>
  ),

  Settings: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),

  Stats: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
    </svg>
  ),

  Hash: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
    </svg>
  ),

  Clock: ({ className = "h-5 w-5", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--warning)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),

  Users: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
    </svg>
  ),

  Shield: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  ),

  Tag: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
    </svg>
  ),

  Link: ({ className = "h-5 w-5", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--primary)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
    </svg>
  ),

  Signature: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
    </svg>
  ),

  Document: ({ className = "h-6 w-6", style }: { className?: string, style?: React.CSSProperties }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" style={style || { color: 'var(--text)' }}>
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  )
};