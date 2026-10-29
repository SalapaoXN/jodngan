interface CsrfMetadata {
  headerName: string;
  parameterName: string;
  token: string;
}

let csrfMetadata: CsrfMetadata | null = null;
let csrfRequest: Promise<CsrfMetadata> | null = null;

async function fetchCsrfMetadata(): Promise<CsrfMetadata> {
  const response = await fetch('/api/csrf', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`CSRF initialization failed with status ${response.status}`);
  }
  return (await response.json()) as CsrfMetadata;
}

async function ensureCsrfMetadata(): Promise<CsrfMetadata> {
  if (csrfMetadata) return csrfMetadata;
  if (csrfRequest) return csrfRequest;

  csrfRequest = fetchCsrfMetadata()
    .then((metadata) => {
      csrfMetadata = metadata;
      return metadata;
    })
    .finally(() => {
      csrfRequest = null;
    });

  return csrfRequest;
}

export async function bootstrapCsrfToken(): Promise<void> {
  await ensureCsrfMetadata();
}

export async function refreshCsrfToken(): Promise<void> {
  csrfMetadata = null;
  await ensureCsrfMetadata();
}

export async function getCsrfHeader(): Promise<[string, string]> {
  const metadata = await ensureCsrfMetadata();
  return [metadata.headerName, metadata.token];
}
