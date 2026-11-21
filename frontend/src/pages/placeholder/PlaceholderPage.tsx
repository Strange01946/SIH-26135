import React from 'react';

interface PlaceholderPageProps {
  title: string;
  description?: string;
}

export function PlaceholderPage({
  title,
  description = 'This feature will be implemented in a future phase.',
}: PlaceholderPageProps): React.JSX.Element {
  return (
    <div className="p-6">
      <h1 className="page-title mb-2">{title}</h1>
      <p className="text-sm text-gray-500">{description}</p>
      <div className="mt-6 card p-6 text-center text-gray-400">
        <div className="text-3xl mb-2">🚧</div>
        <p className="text-sm">Coming soon</p>
      </div>
    </div>
  );
}
