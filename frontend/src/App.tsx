import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { queryClient } from '@/lib/queryClient';
import { AppRouter } from '@/router/index';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';

export default function App(): React.JSX.Element {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AppRouter />
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              classNames: {
                toast: 'font-sans text-sm',
                success: 'bg-green-50 border-green-200 text-green-900',
                error: 'bg-red-50 border-red-200 text-red-900',
              },
            }}
          />
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
