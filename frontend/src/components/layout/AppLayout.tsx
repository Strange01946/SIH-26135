import React from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';

export function AppLayout(): React.JSX.Element {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main
          id="main-content"
          className="flex-1 overflow-y-auto p-6 focus:outline-none"
          tabIndex={-1}
        >
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
      </div>
    </div>
  );
}
