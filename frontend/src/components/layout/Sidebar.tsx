import React from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import type { UserRole } from '@/api/types';

interface NavItem {
  label: string;
  to: string;
  icon: string;
  roles: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: '🏠', roles: ['ROLE_ADMIN', 'ROLE_TRAINING_PROVIDER', 'ROLE_ASSESSOR', 'ROLE_EMPLOYER', 'ROLE_TRAINEE'] },
  { label: 'Trainees', to: '/trainees', icon: '👥', roles: ['ROLE_ADMIN', 'ROLE_TRAINING_PROVIDER', 'ROLE_ASSESSOR'] },
  { label: 'Training Batches', to: '/batches', icon: '📚', roles: ['ROLE_ADMIN', 'ROLE_TRAINING_PROVIDER'] },
  { label: 'Assessments', to: '/assessments', icon: '📝', roles: ['ROLE_ADMIN', 'ROLE_ASSESSOR'] },
  { label: 'Placements', to: '/placements', icon: '💼', roles: ['ROLE_ADMIN', 'ROLE_EMPLOYER', 'ROLE_TRAINING_PROVIDER'] },
  { label: 'Employment', to: '/employment', icon: '🏢', roles: ['ROLE_ADMIN', 'ROLE_EMPLOYER'] },
  { label: 'Analytics', to: '/analytics', icon: '📊', roles: ['ROLE_ADMIN'] },
  { label: 'Data Quality', to: '/data-quality', icon: '🔍', roles: ['ROLE_ADMIN'] },
  { label: 'Bulk Import', to: '/import', icon: '📤', roles: ['ROLE_ADMIN', 'ROLE_TRAINING_PROVIDER'] },
  { label: 'Audit Log', to: '/audit', icon: '📋', roles: ['ROLE_ADMIN'] },
  { label: 'User Management', to: '/users', icon: '⚙️', roles: ['ROLE_ADMIN'] },
];

interface SidebarProps {
  currentRole?: UserRole;
  isCollapsed?: boolean;
}

export function Sidebar({
  currentRole,
  isCollapsed = false,
}: SidebarProps): React.JSX.Element {
  const visibleItems = currentRole
    ? NAV_ITEMS.filter((item) => item.roles.includes(currentRole))
    : NAV_ITEMS; // Show all when no role (placeholder mode)

  return (
    <nav
      aria-label="Main navigation"
      className={cn(
        'bg-white border-r border-gray-200 flex flex-col h-full transition-all duration-200',
        isCollapsed ? 'w-16' : 'w-56',
      )}
    >
      <ul className="flex-1 py-4 space-y-0.5 overflow-y-auto" role="list">
        {visibleItems.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-4 py-2.5 text-sm font-medium transition-colors',
                  'hover:bg-navy-50 hover:text-navy focus:outline-none focus:ring-2 focus:ring-inset focus:ring-navy',
                  isActive
                    ? 'bg-navy-50 text-navy border-l-4 border-navy font-semibold'
                    : 'text-gray-700 border-l-4 border-transparent',
                )
              }
            >
              <span aria-hidden="true" className="text-base flex-shrink-0">
                {item.icon}
              </span>
              {!isCollapsed && <span>{item.label}</span>}
            </NavLink>
          </li>
        ))}
      </ul>

      {/* Footer */}
      {!isCollapsed && (
        <div className="p-4 border-t border-gray-100">
          <p className="text-xs text-gray-400 text-center">
            SIH-26135 · v1.0.0
          </p>
        </div>
      )}
    </nav>
  );
}
