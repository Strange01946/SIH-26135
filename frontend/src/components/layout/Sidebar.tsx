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
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export function Sidebar({
  currentRole,
  isCollapsed = false,
  isMobileOpen = false,
  onCloseMobile,
}: SidebarProps): React.JSX.Element {
  const visibleItems = currentRole
    ? NAV_ITEMS.filter((item) => item.roles.includes(currentRole))
    : NAV_ITEMS;

  const navList = (
    <ul className="flex-1 py-4 space-y-0.5 overflow-y-auto" role="list">
      {visibleItems.map((item) => (
        <li key={item.to}>
          <NavLink
            to={item.to}
            onClick={() => onCloseMobile?.()}
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
            {(!isCollapsed || isMobileOpen) && <span>{item.label}</span>}
          </NavLink>
        </li>
      ))}
    </ul>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <nav
        id="main-sidebar"
        aria-label="Main navigation"
        className={cn(
          'hidden md:flex bg-white border-r border-gray-200 flex-col h-full transition-all duration-200 flex-shrink-0',
          isCollapsed ? 'w-16' : 'w-56',
        )}
      >
        {navList}
        {!isCollapsed && (
          <div className="p-4 border-t border-gray-100">
            <p className="text-xs text-gray-400 text-center">
              SIH-26135 · v1.0.0
            </p>
          </div>
        )}
      </nav>

      {/* Mobile drawer & backdrop */}
      {isMobileOpen && (
        <div className="md:hidden fixed inset-0 z-30 flex">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/40 transition-opacity"
            onClick={onCloseMobile}
            aria-hidden="true"
          />

          {/* Drawer content */}
          <nav
            aria-label="Mobile navigation"
            className="relative flex flex-col w-64 max-w-[80vw] bg-white h-full shadow-xl z-40"
          >
            <div className="p-4 bg-navy text-white flex items-center justify-between">
              <span className="font-semibold text-sm">Navigation Menu</span>
              <button
                type="button"
                onClick={onCloseMobile}
                aria-label="Close menu"
                className="p-1 rounded text-white hover:bg-navy-800"
              >
                ✕
              </button>
            </div>
            {navList}
            <div className="p-4 border-t border-gray-100">
              <p className="text-xs text-gray-400 text-center">
                SIH-26135 · v1.0.0
              </p>
            </div>
          </nav>
        </div>
      )}
    </>
  );
}
