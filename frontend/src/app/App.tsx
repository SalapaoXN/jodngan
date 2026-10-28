import { useQuery } from '@tanstack/react-query';

import { apiFetch } from '../lib/api/client';

interface HealthResponse {
  status: string;
}

export function App() {
  const healthQuery = useQuery({
    queryKey: ['health'],
    queryFn: () => apiFetch<HealthResponse>('/api/health'),
    retry: false,
  });

  return (
    <main className="app-shell">
      <section className="foundation-card">
        <p className="eyebrow">JodNgan</p>
        <h1>Foundation ready for the first product slice.</h1>
        <p>
          This temporary screen only verifies that React can reach the Spring Boot API through the
          development proxy.
        </p>
        <p aria-live="polite">
          API status:{' '}
          {healthQuery.isPending
            ? 'checking...'
            : healthQuery.isError
              ? 'unavailable'
              : healthQuery.data.status}
        </p>
      </section>
    </main>
  );
}
