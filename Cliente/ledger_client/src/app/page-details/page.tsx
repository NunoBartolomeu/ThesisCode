import { Layout } from '@/components/ui/Layout';
import { PageDetails } from '@/components/ledger/PageDetails';
import { Suspense } from 'react';

export default function PageDetailsPage() {
  return (
    <Layout>
      <Suspense fallback={<div>Loading...</div>}>
        <PageDetails />
      </Suspense>
    </Layout>
  );
}