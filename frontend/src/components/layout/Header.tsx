import React from 'react';

interface HeaderProps {
  username?: string;
  role?: string;
  onToggleMobileMenu?: () => void;
  isMobileMenuOpen?: boolean;
}

export function Header({
  username,
  role,
  onToggleMobileMenu,
  isMobileMenuOpen = false,
}: HeaderProps): React.JSX.Element {
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
        </div>

        {/* Right: User info badge (Phase 30.2 will populate this from auth store) */}
        {username && (
          <div className="flex items-center gap-2 text-sm flex-shrink-0">
            <div className="hidden sm:block text-right">
              <div className="font-medium text-white text-xs">{username}</div>
              <div className="text-blue-200 text-[10px] uppercase tracking-wider">
                {role ?? 'User'}
              </div>
            </div>
            <div
              className="h-8 w-8 rounded-full bg-white/20 flex items-center justify-center font-semibold text-sm border border-white/30"
              aria-label={`User avatar for ${username}`}
            >
              {username.charAt(0).toUpperCase()}
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
