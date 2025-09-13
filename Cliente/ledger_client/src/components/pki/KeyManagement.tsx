'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { usePKIViewModel } from '@/viewmodels/usePKIViewModel';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';
import { AuthStorageService } from '@/services/storage/AuthStorageService';

interface PKIManagementProps {
  onGoBack?: () => void;
}

export function PKIManagement({ onGoBack }: PKIManagementProps) {
  const router = useRouter();
  const { user } = useAuthViewModel();
  const {
    loading,
    errors,
    keyPair,
    userCertificate,
    systemCertificate,
    generateKeyPair,
    createCertificate,
    fetchUserCertificate,
    fetchSystemCertificate,
    exportKeyPairAsPEM,
    importKeyPairFromPEM,
    exportCertificateAsPEM,
    importCertificateFromPEM,
    clearErrors,
    hasKeyPair,
    hasUserCertificate,
    hasSystemCertificate
  } = usePKIViewModel();

  // File upload states
  const [pemUploadType, setPemUploadType] = useState<'keys' | 'user-cert' | 'system-cert' | null>(null);
  const [pemContent, setPemContent] = useState('');

  // Get userId from the authenticated user or PKI storage
  const userId = AuthStorageService.getUser()?.id

  useEffect(() => {
    if (userId) {
      fetchUserCertificate(userId);
    }
    fetchSystemCertificate();
  }, [userId, fetchUserCertificate, fetchSystemCertificate]);

  const handleGoBack = () => {
    if (onGoBack) onGoBack();
    else router.push('/dashboard');
  };

  // File download utility
  const downloadFile = (content: string, filename: string) => {
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 100);
  };

  const handleGenerateKeys = async () => {
    await generateKeyPair();
  };

  const handleCreateCertificate = async () => {
    if (userId) {
      await createCertificate(userId);
    }
  };

  const handleExportKeys = () => {
    if (!keyPair) return;
    const keys = exportKeyPairAsPEM();
    downloadFile(keys.privateKey, `${userId}_private.pem`);
    downloadFile(keys.publicKey, `${userId}_public.pem`);
  };

  const handleExportCertificate = (type: 'user' | 'system') => {
    const cert = type === 'user' ? userCertificate : systemCertificate;
    if (!cert) return;
    const pem = exportCertificateAsPEM(cert);
    downloadFile(pem, `${type === 'user' ? userId : 'system'}_certificate.pem`);
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      setPemContent(content);
    };
    reader.readAsText(file);
  };

  const handlePemImport = async () => {
    if (!pemContent || !pemUploadType) return;

    try {
      if (pemUploadType === 'keys') {
        // For keys, we expect both private and public key in the same file or separate calls
        // This is a simplified version - in practice you'd handle separate files
        const result = importKeyPairFromPEM(pemContent, pemContent);
        if (result.success) {
          setPemUploadType(null);
          setPemContent('');
        }
      } else if (pemUploadType === 'user-cert') {
        const result = importCertificateFromPEM(pemContent, userId);
        if (result.success) {
          setPemUploadType(null);
          setPemContent('');
        }
      } else if (pemUploadType === 'system-cert') {
        const result = importCertificateFromPEM(pemContent);
        if (result.success) {
          setPemUploadType(null);
          setPemContent('');
        }
      }
    } catch (error) {
      console.error('Import failed:', error);
    }
  };

  const KeyPairRow = () => (
    <div className="rounded-2xl shadow-xl border-2 p-6" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
      <h3 className="text-lg font-semibold mb-4 flex items-center" style={{ color: 'var(--text)' }}>
        <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-3a1 1 0 011-1h2.586l6.982-6.982A6 6 0 0121 9z" />
        </svg>
        Key Pair
      </h3>
      
      {hasKeyPair() ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1" style={{ color: 'var(--text)' }}>Private Key (Hex)</label>
              <textarea 
                readOnly 
                value={keyPair?.privateKey} 
                className="w-full h-24 p-2 border rounded text-xs font-mono"
                style={{ 
                  backgroundColor: 'var(--bg-secondary)', 
                  borderColor: 'var(--border)',
                  color: 'var(--text-muted)'
                }}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1" style={{ color: 'var(--text)' }}>Public Key (Hex)</label>
              <textarea 
                readOnly 
                value={keyPair?.publicKey} 
                className="w-full h-24 p-2 border rounded text-xs font-mono"
                style={{ 
                  backgroundColor: 'var(--bg-secondary)', 
                  borderColor: 'var(--border)',
                  color: 'var(--text-muted)'
                }}
              />
            </div>
          </div>
          
          <div className="flex gap-2">
            <button 
              onClick={handleExportKeys}
              className="px-4 py-2 rounded transition-all duration-200"
              style={{ 
                backgroundColor: 'var(--primary)', 
                color: 'var(--primary-foreground)' 
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary)';
              }}
            >
              Export PEM
            </button>
            <button 
              onClick={handleGenerateKeys}
              className="px-4 py-2 border rounded transition-all duration-200"
              style={{ 
                borderColor: 'var(--primary)', 
                color: 'var(--primary)',
                backgroundColor: 'transparent'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
                e.currentTarget.style.color = 'var(--primary-foreground)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = 'var(--primary)';
              }}
            >
              Regenerate
            </button>
          </div>
        </div>
      ) : (
        <div className="text-center py-8">
          <p className="mb-4" style={{ color: 'var(--text-muted)' }}>No key pair found</p>
          <div className="flex justify-center gap-2">
            <button 
              onClick={handleGenerateKeys}
              className="px-4 py-2 rounded transition-all duration-200"
              style={{ 
                backgroundColor: 'var(--primary)', 
                color: 'var(--primary-foreground)' 
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary)';
              }}
              disabled={loading}
            >
              Generate New
            </button>
            <button 
              onClick={() => setPemUploadType('keys')}
              className="px-4 py-2 border rounded transition-all duration-200"
              style={{ 
                borderColor: 'var(--primary)', 
                color: 'var(--primary)',
                backgroundColor: 'transparent'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
                e.currentTarget.style.color = 'var(--primary-foreground)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = 'var(--primary)';
              }}
            >
              Import PEM
            </button>
          </div>
        </div>
      )}
    </div>
  );

  const CertificateCard = ({ 
    title, 
    certificate, 
    type, 
    onFetch, 
    icon 
  }: { 
    title: string;
    certificate: any;
    type: 'user' | 'system';
    onFetch: () => void;
    icon: React.ReactNode;
  }) => (
    <div className="rounded-2xl shadow-xl border-2 p-6" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
      <h4 className="text-md font-semibold mb-4 flex items-center" style={{ color: 'var(--text)' }}>
        {icon}
        {title}
      </h4>
      
      {certificate ? (
        <div className="space-y-3">
          <div className="grid grid-cols-1 gap-2 text-sm">
            <div style={{ color: 'var(--text)' }}><strong>Subject:</strong> {certificate.subject}</div>
            <div style={{ color: 'var(--text)' }}><strong>Issuer:</strong> {certificate.issuer}</div>
            <div style={{ color: 'var(--text)' }}><strong>Valid From:</strong> {new Date(certificate.validFrom).toLocaleDateString()}</div>
            <div style={{ color: 'var(--text)' }}><strong>Valid To:</strong> {new Date(certificate.validTo).toLocaleDateString()}</div>
            <div style={{ color: 'var(--text)' }}><strong>Serial:</strong> {certificate.serialNumber}</div>
          </div>
          
          <div className="pt-2">
            <label className="block text-sm font-medium mb-1" style={{ color: 'var(--text)' }}>Certificate (Base64)</label>
            <textarea 
              readOnly 
              value={certificate.certificateBase64} 
              className="w-full h-20 p-2 border rounded text-xs font-mono"
              style={{ 
                backgroundColor: 'var(--bg-secondary)', 
                borderColor: 'var(--border)',
                color: 'var(--text-muted)'
              }}
            />
          </div>
          
          <button 
            onClick={() => handleExportCertificate(type)}
            className="px-3 py-1 rounded text-sm transition-all duration-200"
            style={{ 
              backgroundColor: 'var(--success)', 
              color: 'var(--success-foreground)' 
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--success-hover)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--success)';
            }}
          >
            Export PEM
          </button>
        </div>
      ) : (
        <div className="text-center py-4">
          <p className="mb-3" style={{ color: 'var(--text-muted)' }}>No certificate found</p>
          <div className="flex justify-center gap-2">
            <button 
              onClick={onFetch}
              className="px-3 py-1 rounded text-sm transition-all duration-200"
              style={{ 
                backgroundColor: 'var(--primary)', 
                color: 'var(--primary-foreground)' 
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary)';
              }}
              disabled={loading}
            >
              Fetch from Server
            </button>
            {type === 'user' && hasKeyPair() && (
              <button 
                onClick={handleCreateCertificate}
                className="px-3 py-1 rounded text-sm transition-all duration-200"
                style={{ 
                  backgroundColor: 'var(--success)', 
                  color: 'var(--success-foreground)' 
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = 'var(--success-hover)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'var(--success)';
                }}
                disabled={loading}
              >
                Create New
              </button>
            )}
            <button 
              onClick={() => setPemUploadType(`${type}-cert` as any)}
              className="px-3 py-1 border rounded text-sm transition-all duration-200"
              style={{ 
                borderColor: 'var(--primary)', 
                color: 'var(--primary)',
                backgroundColor: 'transparent'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
                e.currentTarget.style.color = 'var(--primary-foreground)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = 'var(--primary)';
              }}
            >
              Import PEM
            </button>
          </div>
        </div>
      )}
    </div>
  );

  // Don't render if no user is available
  if (!userId) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="text-center py-8">
          <p style={{ color: 'var(--text-muted)' }}>No user information available for PKI management.</p>
          <button 
            onClick={handleGoBack}
            className="mt-4 px-4 py-2 border rounded transition-all duration-200"
            style={{ 
              borderColor: 'var(--border)', 
              color: 'var(--text)',
              backgroundColor: 'transparent'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--bg-secondary)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
            }}
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>PKI Management</h1>
          <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>User: {userId}</p>
        </div>
        <button 
          onClick={handleGoBack}
          className="px-4 py-2 border rounded transition-all duration-200"
          style={{ 
            borderColor: 'var(--border)', 
            color: 'var(--text)',
            backgroundColor: 'transparent'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--bg-secondary)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'transparent';
          }}
        >
          Back
        </button>
      </div>

      {/* Error Messages */}
      {Object.keys(errors).length > 0 && (
        <div className="border rounded-lg p-4 mb-6" style={{ backgroundColor: 'var(--destructive-bg)', borderColor: 'var(--destructive-border)' }}>
          <div className="flex justify-between items-start">
            <div>
              <h4 className="text-sm font-medium" style={{ color: 'var(--destructive-text)' }}>Errors:</h4>
              <ul className="mt-1 text-sm" style={{ color: 'var(--destructive-text)' }}>
                {Object.entries(errors).map(([key, message]) => (
                  <li key={key}>• {message}</li>
                ))}
              </ul>
            </div>
            <button 
              onClick={clearErrors}
              className="transition-colors duration-200"
              style={{ color: 'var(--destructive-text)' }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'var(--destructive-text-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'var(--destructive-text)';
              }}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Loading Indicator */}
      {loading && (
        <div className="border rounded-lg p-4 mb-6" style={{ backgroundColor: 'var(--info-bg)', borderColor: 'var(--info-border)' }}>
          <div className="flex items-center">
            <svg className="animate-spin h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" style={{ color: 'var(--info-text)' }}>
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span style={{ color: 'var(--info-text)' }}>Processing...</span>
          </div>
        </div>
      )}

      <div className="space-y-6">
        {/* Key Pair Row */}
        <KeyPairRow />

        {/* Certificates Row */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <CertificateCard 
            title="User Certificate"
            certificate={userCertificate}
            type="user"
            onFetch={() => fetchUserCertificate(userId)}
            icon={
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            }
          />
          
          <CertificateCard 
            title="System Certificate"
            certificate={systemCertificate}
            type="system"
            onFetch={fetchSystemCertificate}
            icon={
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            }
          />
        </div>
      </div>

      {/* PEM Upload Modal */}
      {pemUploadType && (
        <div className="fixed inset-0 flex items-center justify-center p-4 z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="rounded-lg p-6 w-full max-w-2xl" style={{ backgroundColor: 'var(--bg)', borderColor: 'var(--border)' }}>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold" style={{ color: 'var(--text)' }}>
                Import {pemUploadType === 'keys' ? 'Key Pair' : 
                       pemUploadType === 'user-cert' ? 'User Certificate' : 'System Certificate'} PEM
              </h3>
              <button 
                onClick={() => {setPemUploadType(null); setPemContent('');}}
                className="transition-colors duration-200"
                style={{ color: 'var(--text-muted)' }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.color = 'var(--text)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.color = 'var(--text-muted)';
                }}
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: 'var(--text)' }}>
                  Upload PEM file
                </label>
                <input 
                  type="file" 
                  accept=".pem,.txt"
                  onChange={handleFileUpload}
                  className="w-full p-2 border rounded"
                  style={{ 
                    backgroundColor: 'var(--bg-secondary)', 
                    borderColor: 'var(--border)',
                    color: 'var(--text)'
                  }}
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: 'var(--text)' }}>
                  Or paste PEM content
                </label>
                <textarea 
                  value={pemContent}
                  onChange={(e) => setPemContent(e.target.value)}
                  placeholder="-----BEGIN ... -----"
                  className="w-full h-32 p-2 border rounded font-mono text-sm"
                  style={{ 
                    backgroundColor: 'var(--bg-secondary)', 
                    borderColor: 'var(--border)',
                    color: 'var(--text)'
                  }}
                />
              </div>
              
              <div className="flex justify-end gap-2">
                <button 
                  onClick={() => {setPemUploadType(null); setPemContent('');}}
                  className="px-4 py-2 border rounded transition-all duration-200"
                  style={{ 
                    borderColor: 'var(--border)', 
                    color: 'var(--text)',
                    backgroundColor: 'transparent'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = 'var(--bg-secondary)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = 'transparent';
                  }}
                >
                  Cancel
                </button>
                <button 
                  onClick={handlePemImport}
                  disabled={!pemContent}
                  className="px-4 py-2 rounded transition-all duration-200"
                  style={{ 
                    backgroundColor: !pemContent ? 'var(--muted)' : 'var(--primary)', 
                    color: !pemContent ? 'var(--muted-foreground)' : 'var(--primary-foreground)' 
                  }}
                  onMouseEnter={(e) => {
                    if (pemContent) {
                      e.currentTarget.style.backgroundColor = 'var(--primary-hover)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (pemContent) {
                      e.currentTarget.style.backgroundColor = 'var(--primary)';
                    }
                  }}
                >
                  Import
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}