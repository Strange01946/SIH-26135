import React from 'react';
import { Link } from 'react-router-dom';

export function NotFoundPage(): React.JSX.Element {
  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] text-center px-4">
      <div className="text-6xl mb-6" aria-hidden="true">
        404
      </div>
      <h1 className="text-2xl font-semibold text-gray-900 mb-2">
        Page not found
      </h1>
      <p className="text-gray-500 mb-6 max-w-sm">
        The page you are looking for does not exist or has been moved.
      </p>
      <Link to="/dashboard" className="btn-primary">
        Return to Dashboard
      </Link>
    </div>
  );
}
