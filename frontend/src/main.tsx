import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/globals.css';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element #root not found in DOM');
}

const root = ReactDOM.createRoot(rootElement);

if ('serviceWorker' in navigator) {
  const retiredWorker = new URL('/mockServiceWorker.js', window.location.origin).href;
  void navigator.serviceWorker.getRegistrations().then(registrations => Promise.all(
    registrations.filter(registration => [registration.active, registration.waiting, registration.installing]
      .some(worker => worker?.scriptURL === retiredWorker))
      .map(registration => registration.unregister()),
  )).catch(() => undefined);
}

root.render(<React.StrictMode><App /></React.StrictMode>);
