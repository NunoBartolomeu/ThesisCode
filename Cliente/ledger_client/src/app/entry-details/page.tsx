import { Layout } from '@/components/ui/Layout';
import { EntryDetails } from '@/components/ledger/EntryDetails';
import { Suspense } from 'react';

export default function EntryDetailsPage() {
  return (
    <Layout>
      <Suspense fallback={<div>Loading...</div>}>
        <EntryDetails />
      </Suspense>
    </Layout>
  );
}