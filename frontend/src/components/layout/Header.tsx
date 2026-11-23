import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  type UserPersona,
  formatPersonaLabel,
  getDefaultLandingRouteForPersona,
} from '@/api/types';

interface HeaderProps {
  username?: string;
  role?: string;
  onToggleMobileMenu?: () => void;
  isMobileMenuOpen?: boolean;
}

export function Header({
  username: propUsername,
  role: propRole,
  onToggleMobileMenu,
  isMobileMenuOpen = false,
}: HeaderProps): React.JSX.Element {
  const {
    user,
    isAuthenticated,
    logout,
    personas,
    primaryPersona,
    activePersona,
    setActivePersona,
  } = useAuth();
  const navigate = useNavigate();

  const [roleMenuOpen, setRoleMenuOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const displayUsername = propUsername || user?.username;
  const currentPersona = activePersona || primaryPersona;
  const displayRole =
    propRole ||
    (currentPersona ? formatPersonaLabel(currentPersona) : null) ||
    (user?.authorities?.[0] ? user.authorities[0].replace(/^ROLE_/, '') : 'User');
  const showAuthInfo = Boolean(displayUsername || isAuthenticated);

  // Close role selector dropdown on outside click
  useEffect(() => {
    if (!roleMenuOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setRoleMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [roleMenuOpen]);

  const handleSwitchRole = (selectedPersona: UserPersona) => {
    setActivePersona(selectedPersona);
    setRoleMenuOpen(false);
    navigate(getDefaultLandingRouteForPersona(selectedPersona));
  };

  const handleSignOut = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className="bg-navy text-white shadow-md relative z-20">
      {/* Top stripe: Indian flag tricolor accent (GIGW branding) */}
      <div className="h-1 bg-gradient-to-r from-saffron via-white to-india-green" />

      <div className="flex items-center justify-between px-4 py-3 max-w-screen-2xl mx-auto">
        {/* Left: Mobile hamburger + Portal identity */}
        <div className="flex items-center gap-3">
          {onToggleMobileMenu && (
            <button
              type="button"
              onClick={onToggleMobileMenu}
              aria-label={isMobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
              aria-expanded={isMobileMenuOpen}
              aria-controls="main-sidebar"
              className="md:hidden p-2 rounded-md hover:bg-navy-800 focus:outline-none focus:ring-2 focus:ring-white text-white text-lg transition-colors"
            >
              {isMobileMenuOpen ? '✕' : '☰'}
            </button>
          )}

          {/* Government emblem */}
          <Link
            to="/"
            className="flex items-center gap-3 hover:opacity-95 transition-opacity focus:outline-none focus:ring-2 focus:ring-white rounded p-0.5"
          >
            <div
              className="h-10 w-10 rounded-full bg-white/10 flex items-center justify-center text-xl flex-shrink-0"
              aria-hidden="true"
            >
              🏛️
            </div>

            <div>
              <div className="text-sm md:text-base font-bold leading-tight tracking-wide">
                National Skill &amp; Placement Tracking Platform
              </div>
              <div className="text-xs text-blue-200 leading-tight">
                SIH-26135 · Ministry of Skill Development &amp; Entrepreneurship
              </div>
            </div>
          </Link>
        </div>

        {/* Right: User session badge & Sign Out action */}
        <div className="flex items-center gap-3">
          {showAuthInfo ? (
            <div className="flex items-center gap-3 text-sm flex-shrink-0">
              {/* Multi-role switcher dropdown or single role badge */}
              {personas.length > 1 ? (
                <div className="relative inline-block text-left" ref={dropdownRef}>
                  <button
                    type="button"
                    onClick={() => setRoleMenuOpen(!roleMenuOpen)}
                    className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded bg-white/10 hover:bg-white/20 border border-white/30 text-white transition-colors focus:outline-none focus:ring-2 focus:ring-white"
                    aria-label="Switch operational role"
                    aria-expanded={roleMenuOpen}
                  >
                    <span className="text-blue-200">Role:</span>
                    <span className="font-bold text-white">
                      {formatPersonaLabel(currentPersona)}
                    </span>
                    <span className="text-[10px]">▼</span>
                  </button>
                  {roleMenuOpen && (
                    <div className="absolute right-0 mt-1 w-48 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-50 py-1 text-gray-800">
                      <div className="px-3 py-1 text-[10px] font-semibold text-gray-400 uppercase tracking-wider border-b border-gray-100">
                        Switch Role Context
                      </div>
                      {personas.map((p) => {
                        const isCurrent = p === currentPersona;
                        return (
                          <button
                            key={p}
                            type="button"
                            onClick={() => handleSwitchRole(p)}
                            className={`w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-navy-50 hover:text-navy ${
                              isCurrent ? 'font-bold text-navy bg-navy-50/60' : 'text-gray-700'
                            }`}
                          >
                            <span>{formatPersonaLabel(p)}</span>
                            {isCurrent && (
                              <span className="text-navy font-bold text-xs">✓</span>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              ) : (
                <div className="hidden sm:block text-right">
                  <div className="font-medium text-white text-xs">{displayUsername}</div>
                  <div className="text-blue-200 text-[10px] uppercase tracking-wider font-semibold">
                    {displayRole}
                  </div>
                </div>
              )}

              <div
                className="h-8 w-8 rounded-full bg-white/20 flex items-center justify-center font-semibold text-sm border border-white/30"
                aria-label={`User avatar for ${displayUsername}`}
              >
                {displayUsername ? displayUsername.charAt(0).toUpperCase() : 'U'}
              </div>
              <button
                type="button"
                onClick={handleSignOut}
                className="px-2.5 py-1 text-xs font-medium rounded border border-white/30 hover:bg-white/10 text-white transition-colors focus:outline-none focus:ring-2 focus:ring-white"
              >
                Sign Out
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              className="px-3 py-1.5 text-xs font-semibold rounded bg-white text-navy hover:bg-gray-100 transition-colors focus:outline-none focus:ring-2 focus:ring-white"
            >
              Sign In
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
