import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { I18nProvider } from '@lingui/react';
import { i18n, detectLocale, loadCatalog } from './i18n';
import App from './App';

await loadCatalog(detectLocale()); // before first render: no flash of raw msgids

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider i18n={i18n}>
      <App />
    </I18nProvider>
  </StrictMode>,
);
