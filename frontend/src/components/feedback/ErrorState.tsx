import React from 'react';
import type { ApiError } from '@/api/types';

interface ErrorStateProps {
  error: ApiError | Error | null;
  onRetry?: () => void;
  title?: string;
}

export function ErrorState({
  error,
  onRetry,
  title = 'Failed to load data',
}: ErrorStateProps): React.JSX.Element {
  const message =
    error instanceof Error
      ? error.message
      : (error as ApiError)?.message ?? 'An unexpected error occurred.';

  return (
    <div
      role="alert"
      className="flex flex-col items-center justify-center py-12 px-4 text-center"
    >
      <div className="text-4xl mb-3" aria-hidden="true">
        ❌
      </div>
      <h3 className="text-base font-semibold text-gray-900 mb-1">{title}</h3>
      <p className="text-sm text-gray-500 mb-4 max-w-sm">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-primary">
          Retry
        </button>
      )}
    </div>
  );
}
