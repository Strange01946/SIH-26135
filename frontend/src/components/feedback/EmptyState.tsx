import React from 'react';

interface EmptyStateProps {
  title?: string;
  description?: string;
  action?: React.ReactNode;
  icon?: string;
}

export function EmptyState({
  title = 'No data found',
  description = 'There are no records to display.',
  action,
  icon = '📋',
}: EmptyStateProps): React.JSX.Element {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      <div className="text-4xl mb-3" aria-hidden="true">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-gray-900 mb-1">{title}</h3>
      <p className="text-sm text-gray-500 mb-4 max-w-sm">{description}</p>
      {action}
    </div>
  );
}
