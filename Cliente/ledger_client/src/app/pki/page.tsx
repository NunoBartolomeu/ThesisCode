'use client';

import { Suspense } from 'react';
import { Layout } from '@/components/ui/Layout';
import { PKIManagement } from '@/components/pki/KeyManagement';
import { useAuthViewModel } from '@/viewmodels/useAuthViewModel';

export default function PKIPage() {
  const { user } = useAuthViewModel();
  
  if (!user) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-screen">
          <p>Please log in to access PKI management.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <Suspense fallback={<div>Loading PKI Management...</div>}>
        <PKIManagement />
      </Suspense>
    </Layout>
  );
}