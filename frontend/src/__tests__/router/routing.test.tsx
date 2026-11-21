import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { AuthProvider } from '@/context/AuthContext';
import { AppRouter } from '@/router/index';
import { clearSession } from '@/lib/sessionManager';

function TestWrapper({
  children,
  initialEntries = ['/'],
}: {
  children: React.ReactNode;
  initialEntries?: string[];
}): React.JSX.Element {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={initialEntries}>
        <AuthProvider>{children}</AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('AppRouter', () => {
  beforeEach(() => {
    clearSession();
  });

  it('redirects unauthenticated root / to /login', async () => {
    render(
      <TestWrapper initialEntries={['/']}>
        <AppRouter />
      </TestWrapper>,
    );
    expect(
      await screen.findByRole('heading', { name: /Sign In to Official Portal/i }),
    ).toBeInTheDocument();
  });

  it('renders 404 page for unknown routes', async () => {
    render(
      <TestWrapper initialEntries={['/this-route-does-not-exist']}>
        <AppRouter />
      </TestWrapper>,
    );
    expect(await screen.findByText('Page not found')).toBeInTheDocument();
  });

  it('redirects unauthenticated access to /dashboard to /login', async () => {
    render(
      <TestWrapper initialEntries={['/dashboard']}>
        <AppRouter />
      </TestWrapper>,
    );
    expect(
      await screen.findByRole('heading', { name: /Sign In to Official Portal/i }),
    ).toBeInTheDocument();
  });

  it('renders /login form with official credentials inputs', async () => {
    render(
      <TestWrapper initialEntries={['/login']}>
        <AppRouter />
      </TestWrapper>,
    );
    expect(
      await screen.findByRole('heading', { name: /Sign In to Official Portal/i }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Username or Official Email/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter your password/i)).toBeInTheDocument();
  });
});
