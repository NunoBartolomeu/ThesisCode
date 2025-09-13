import { Layout } from '@/components/ui/Layout';
import { FileDetailsPage } from '@/components/files/FileDetailsPage';
import { Suspense } from 'react';

export default function FileDetailsPageRoute() {
  return (
    <Layout>
      <Suspense fallback={<div>Loading...</div>}>
        <FileDetailsPage />
      </Suspense>
    </Layout>
  );
}