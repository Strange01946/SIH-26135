import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { AppRouter } from '@/router/index';

function TestWrapper({ children }: { children: React.ReactNode }): React.JSX.Element {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('AppRouter', () => {
  it('redirects / to /dashboard', () => {
    render(
      <TestWrapper>
        <MemoryRouter initialEntries={['/']}>
          <AppRouter />
        </MemoryRouter>
      </TestWrapper>,
    );
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });

  it('renders 404 page for unknown routes', () => {
    render(
      <TestWrapper>
        <MemoryRouter initialEntries={['/this-route-does-not-exist']}>
          <AppRouter />
        </MemoryRouter>
      </TestWrapper>,
    );
    expect(screen.getByText('Page not found')).toBeInTheDocument();
  });

  it('renders /dashboard placeholder', () => {
    render(
      <TestWrapper>
        <MemoryRouter initialEntries={['/dashboard']}>
          <AppRouter />
        </MemoryRouter>
      </TestWrapper>,
    );
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });

  it('renders /login placeholder', () => {
    render(
      <TestWrapper>
        <MemoryRouter initialEntries={['/login']}>
          <AppRouter />
        </MemoryRouter>
      </TestWrapper>,
    );
    expect(screen.getByText('Login')).toBeInTheDocument();
  });
});
