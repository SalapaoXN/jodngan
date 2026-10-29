import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { App } from './app/App';
import { AppProviders } from './app/providers/AppProviders';
import { bootstrapCsrfToken } from './lib/api/csrf';
import './styles.css';
import './redesign.css';

const root = document.getElementById('root');

if (!root) {
  throw new Error('Root element not found');
}

void bootstrapCsrfToken().catch(() => undefined);

createRoot(root).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
);
