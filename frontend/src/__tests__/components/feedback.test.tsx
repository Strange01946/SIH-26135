import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner, PageLoading } from '@/components/feedback/LoadingSpinner';
import type { ApiError } from '@/api/types';

describe('EmptyState', () => {
  it('renders default title and description', () => {
    render(<EmptyState />);
    expect(screen.getByText('No data found')).toBeInTheDocument();
    expect(screen.getByText('There are no records to display.')).toBeInTheDocument();
  });

  it('renders custom title and description', () => {
    render(<EmptyState title="No trainees" description="Add a trainee to get started." />);
    expect(screen.getByText('No trainees')).toBeInTheDocument();
    expect(screen.getByText('Add a trainee to get started.')).toBeInTheDocument();
  });
});

describe('ErrorState', () => {
  it('renders error message from ApiError', () => {
    const error: ApiError = { code: 'HTTP_500', message: 'Server error', status: 500 };
    render(<ErrorState error={error} />);
    expect(screen.getByText('Server error')).toBeInTheDocument();
  });

  it('renders retry button when onRetry provided', () => {
    const error: ApiError = { code: 'HTTP_503', message: 'Service unavailable', status: 503 };
    render(<ErrorState error={error} onRetry={() => {}} />);
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('does not render retry button without onRetry', () => {
    const error: ApiError = { code: 'HTTP_503', message: 'Service unavailable', status: 503 };
    render(<ErrorState error={error} />);
    expect(screen.queryByText('Retry')).toBeNull();
  });

  it('has role="alert" for accessibility', () => {
    const error: ApiError = { code: 'HTTP_404', message: 'Not found', status: 404 };
    const { container } = render(<ErrorState error={error} />);
    expect(container.querySelector('[role="alert"]')).toBeInTheDocument();
  });
});

describe('LoadingSpinner', () => {
  it('renders with role="status"', () => {
    render(<LoadingSpinner />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders sr-only label', () => {
    render(<LoadingSpinner label="Loading data" />);
    expect(screen.getByText('Loading data')).toBeInTheDocument();
  });
});

describe('PageLoading', () => {
  it('renders with custom message', () => {
    render(<PageLoading message="Fetching records…" />);
    expect(screen.getByText('Fetching records…')).toBeInTheDocument();
  });
});
