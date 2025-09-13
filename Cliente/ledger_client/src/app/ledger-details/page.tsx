import { Layout } from '@/components/ui/Layout';
import { LedgerDetails } from '@/components/ledger/LedgerDetails';
import { Suspense } from 'react';

export default function LedgerDetailsPage() {
  return (
    <Layout>
      <Suspense fallback={<div>Loading...</div>}>
        <LedgerDetailsWrapper />
      </Suspense>
    </Layout>
  );
}

function LedgerDetailsWrapper() {
  return <LedgerDetails />;
}