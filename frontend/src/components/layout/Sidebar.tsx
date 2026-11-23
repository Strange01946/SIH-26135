import React, { useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useAuth } from '@/context/AuthContext';
import type { UserPersona, UserRole } from '@/api/types';

export interface NavItem {
  label: string;
  to: string;
  icon: string;
  persona: UserPersona;
}

export const NAV_ITEMS: NavItem[] = [
  // Admin Persona Console
  {
    label: 'Admin Console',
    to: '/admin/dashboard',
    icon: '🛡️',
    persona: 'ADMIN',
  },
  {
    label: 'Onboarding Requests',
    to: '/admin/onboarding',
    icon: '📥',
    persona: 'ADMIN',
  },
  {
    label: 'User Provisioning',
    to: '/admin/users',
    icon: '👤',
    persona: 'ADMIN',
  },

  // Training Provider Persona Console
  {
    label: 'Provider Console',
    to: '/provider/dashboard',
    icon: '🏫',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Training Centers',
    to: '/provider/centers',
    icon: '🏢',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Approved Courses',
    to: '/provider/courses',
    icon: '📚',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Batch Management',
    to: '/provider/batches',
    icon: '👥',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Candidate Enrollment',
    to: '/provider/enrollment',
    icon: '✍️',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Bulk Ingestion',
    to: '/provider/imports',
    icon: '📥',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Import Quarantine',
    to: '/provider/quarantine',
    icon: '⚠️',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Quality Remediation',
    to: '/provider/data-quality',
    icon: '🔍',
    persona: 'TRAINING_PROVIDER',
  },
  {
    label: 'Center Scorecards',
    to: '/provider/scorecards',
    icon: '📊',
    persona: 'TRAINING_PROVIDER',
  },

  // Assessor Persona Console
  {
    label: 'Assessor Console',
    to: '/assessor/dashboard',
    icon: '⚖️',
    persona: 'ASSESSOR',
  },

  // Employer Persona Console
  {
    label: 'Employer Console',
    to: '/employer/dashboard',
    icon: '💼',
    persona: 'EMPLOYER',
  },

  // Trainee Persona Console
  {
    label: 'Trainee Portal',
    to: '/trainee/dashboard',
    icon: '🎓',
    persona: 'TRAINEE',
  },
  {
    label: 'My Training & Attendance',
    to: '/trainee/training',
    icon: '📖',
    persona: 'TRAINEE',
  },
  {
    label: 'DPDP Privacy & Consent',
    to: '/trainee/consent',
    icon: '🔒',
    persona: 'TRAINEE',
  },
  {
    label: 'Assessments & Certificates',
    to: '/trainee/assessments',
    icon: '📜',
    persona: 'TRAINEE',
  },
  {
    label: 'Career Journey & Surveys',
    to: '/trainee/journey',
    icon: '🚀',
    persona: 'TRAINEE',
  },
];

function resolvePersonaFromRole(roleStr: string): UserPersona | null {
  const normalized = roleStr.toUpperCase().replace(/^ROLE_/, '');
  if (
    [
      'ADMIN',
      'SUPER_ADMIN',
      'GOVERNMENT_ADMIN',
      'GOVERNMENT_OFFICER',
      'DISTRICT_OFFICER',
      'ANALYST',
    ].includes(normalized)
  ) {
    return 'ADMIN';
  }
  if (['TRAINING_PROVIDER', 'TP'].includes(normalized)) {
    return 'TRAINING_PROVIDER';
  }
  if (['ASSESSOR', 'VERIFIER'].includes(normalized)) {
    return 'ASSESSOR';
  }
  if (['EMPLOYER'].includes(normalized)) {
    return 'EMPLOYER';
  }
  if (['TRAINEE'].includes(normalized)) {
    return 'TRAINEE';
  }
  return null;
}

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
  const { user, activePersona, primaryPersona } = useAuth();

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

  // Determine effective persona strictly to eliminate cross-role navigation leakage
  let effectivePersona: UserPersona | null = null;
  if (currentRole) {
    effectivePersona = resolvePersonaFromRole(currentRole);
  } else if (activePersona) {
    effectivePersona = activePersona;
  } else if (primaryPersona) {
    effectivePersona = primaryPersona;
  }

  // Filter items strictly by effective persona
  const visibleItems = NAV_ITEMS.filter((item) => {
    if (effectivePersona) {
      return item.persona === effectivePersona;
    }
    // Standalone / unauthenticated test view: show all items
    if (!user && !currentRole) {
      return true;
    }
    return false;
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
