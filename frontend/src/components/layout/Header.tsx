import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

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
  const { user, isAuthenticated, logout, primaryPersona } = useAuth();
  const navigate = useNavigate();

  const displayUsername = propUsername || user?.username;
  const displayRole = propRole || primaryPersona || (user?.authorities?.[0] ? user.authorities[0].replace(/^ROLE_/, '') : 'User');
  const showAuthInfo = Boolean(displayUsername || isAuthenticated);

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
              <div className="hidden sm:block text-right">
                <div className="font-medium text-white text-xs">{displayUsername}</div>
                <div className="text-blue-200 text-[10px] uppercase tracking-wider">
                  {displayRole}
                </div>
              </div>
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
