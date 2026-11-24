import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { queryClient } from '@/lib/queryClient';
import { AuthProvider } from '@/context/AuthContext';
import { AppRouter } from '@/router/index';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { localEvaluatorEnabled, simulationActive } from '@/lib/demoEnvironment';

const EvaluatorControls = React.lazy(() => import('@/evaluator/EvaluatorControls'));

export default function App(): React.JSX.Element {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            {simulationActive && (
              <div role="status" className="sticky top-0 z-40 border-b border-amber-800 bg-amber-100 px-4 py-2 text-center text-sm font-bold text-amber-950">
                Simulation mode · Synthetic data and local actions
              </div>
            )}
            <AppRouter />
            {localEvaluatorEnabled && <React.Suspense fallback={null}><EvaluatorControls /></React.Suspense>}
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
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
