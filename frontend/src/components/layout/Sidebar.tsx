import React, { useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useAuth } from '@/context/AuthContext';
import type { UserPersona, UserRole } from '@/api/types';

interface NavItem {
  label: string;
  to: string;
  icon: string;
  personas?: UserPersona[];
  roles?: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  // General / Persona Dashboards
  { label: 'Dashboard', to: '/dashboard', icon: '🏠' },
  {
    label: 'Admin Console',
    to: '/admin/dashboard',
    icon: '🛡️',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'Onboarding Requests',
    to: '/admin/onboarding',
    icon: '📥',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'Provision Users',
    to: '/admin/users',
    icon: '👤',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'Provider Console',
    to: '/provider/dashboard',
    icon: '🏫',
    personas: ['TRAINING_PROVIDER'],
    roles: ['ROLE_TRAINING_PROVIDER', 'ROLE_training_provider'],
  },
  {
    label: 'Assessor Console',
    to: '/assessor/dashboard',
    icon: '⚖️',
    personas: ['ASSESSOR'],
    roles: ['ROLE_ASSESSOR', 'ROLE_verifier'],
  },
  {
    label: 'Employer Console',
    to: '/employer/dashboard',
    icon: '💼',
    personas: ['EMPLOYER'],
    roles: ['ROLE_EMPLOYER', 'ROLE_employer'],
  },
  {
    label: 'Trainee Portal',
    to: '/trainee/dashboard',
    icon: '🎓',
    personas: ['TRAINEE'],
    roles: ['ROLE_TRAINEE', 'ROLE_trainee'],
  },
  {
    label: 'My Training & Attendance',
    to: '/trainee/training',
    icon: '📖',
    personas: ['TRAINEE'],
    roles: ['ROLE_TRAINEE', 'ROLE_trainee'],
  },
  {
    label: 'DPDP Privacy & Consent',
    to: '/trainee/consent',
    icon: '🔒',
    personas: ['TRAINEE'],
    roles: ['ROLE_TRAINEE', 'ROLE_trainee'],
  },
  {
    label: 'Assessments & Certificates',
    to: '/trainee/assessments',
    icon: '📜',
    personas: ['TRAINEE'],
    roles: ['ROLE_TRAINEE', 'ROLE_trainee'],
  },
  {
    label: 'Career Journey & Surveys',
    to: '/trainee/journey',
    icon: '🚀',
    personas: ['TRAINEE'],
    roles: ['ROLE_TRAINEE', 'ROLE_trainee'],
  },

  // Operational Sections
  {
    label: 'Trainees',
    to: '/trainees',
    icon: '👥',
    personas: ['ADMIN', 'TRAINING_PROVIDER', 'ASSESSOR'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_TRAINING_PROVIDER', 'ROLE_training_provider', 'ROLE_ASSESSOR', 'ROLE_verifier'],
  },
  {
    label: 'Training Batches',
    to: '/batches',
    icon: '📚',
    personas: ['ADMIN', 'TRAINING_PROVIDER'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_TRAINING_PROVIDER', 'ROLE_training_provider'],
  },
  {
    label: 'Assessments',
    to: '/assessments',
    icon: '📝',
    personas: ['ADMIN', 'ASSESSOR'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_ASSESSOR', 'ROLE_verifier'],
  },
  {
    label: 'Placements',
    to: '/placements',
    icon: '🤝',
    personas: ['ADMIN', 'EMPLOYER', 'TRAINING_PROVIDER'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_EMPLOYER', 'ROLE_employer', 'ROLE_TRAINING_PROVIDER', 'ROLE_training_provider'],
  },
  {
    label: 'Employment',
    to: '/employment',
    icon: '🏢',
    personas: ['ADMIN', 'EMPLOYER'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_EMPLOYER', 'ROLE_employer'],
  },
  {
    label: 'Analytics',
    to: '/analytics',
    icon: '📊',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'Data Quality',
    to: '/data-quality',
    icon: '🔍',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'Bulk Import',
    to: '/import',
    icon: '📤',
    personas: ['ADMIN', 'TRAINING_PROVIDER'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_TRAINING_PROVIDER', 'ROLE_training_provider'],
  },
  {
    label: 'Audit Log',
    to: '/audit',
    icon: '📋',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
  {
    label: 'User Management',
    to: '/users',
    icon: '⚙️',
    personas: ['ADMIN'],
    roles: ['ROLE_ADMIN', 'ROLE_super_admin', 'ROLE_government_admin'],
  },
];

interface SidebarProps {
  currentRole?: UserRole | string;
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
  const { user, personas } = useAuth();

  // Handle ESC key to close mobile drawer
  useEffect(() => {
    if (!isMobileOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCloseMobile?.();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isMobileOpen, onCloseMobile]);

  // Determine item visibility
  const visibleItems = NAV_ITEMS.filter((item) => {
    // Prop override for isolated testing
    if (currentRole) {
      if (!item.roles && !item.personas) return true;
      const roleStr = currentRole.toUpperCase();
      if (roleStr === 'ROLE_TRAINEE' || roleStr === 'TRAINEE') {
        return item.personas?.includes('TRAINEE') || item.to === '/dashboard';
      }
      if (roleStr === 'ROLE_ADMIN' || roleStr === 'ADMIN') {
        return true;
      }
      const personaFromRole = roleStr.replace(/^ROLE_/, '');
      return (
        item.roles?.some((r) => r.toUpperCase() === roleStr) ||
        item.personas?.some((p) => p === currentRole || p === personaFromRole)
      );
    }

    // If authenticated, check user's actual personas and authorities
    if (user) {
      if (!item.personas && !item.roles) return true;
      const matchesPersona = item.personas?.some((p) => personas.includes(p));
      const matchesRole = item.roles?.some((r) =>
        user.authorities.some((a) => a.toLowerCase() === r.toLowerCase()),
      );
      return Boolean(matchesPersona || matchesRole);
    }

    // Standalone / unauthenticated test view: show all items
    return true;
  });

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
                className="p-1 rounded text-white hover:bg-navy-800 focus:outline-none focus:ring-2 focus:ring-white"
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
