import React from 'react';

interface HeaderProps {
  username?: string;
  role?: string;
}

export function Header({ username, role }: HeaderProps): React.JSX.Element {
  return (
    <header className="bg-navy text-white shadow-md">
      {/* Top stripe: Indian flag tricolor accent */}
      <div className="h-1 bg-gradient-to-r from-saffron via-white to-india-green" />
      <div className="flex items-center justify-between px-4 py-3 max-w-screen-2xl mx-auto">
        {/* Portal identity */}
        <div className="flex items-center gap-3">
          {/* Government emblem placeholder */}
          <div
            className="h-10 w-10 rounded-full bg-white/10 flex items-center justify-center text-lg"
            aria-hidden="true"
          >
            🏛️
          </div>
          <div>
            <div className="text-sm font-bold leading-tight tracking-wide">
              National Skill &amp; Placement Tracking
            </div>
            <div className="text-xs text-blue-200 leading-tight">
              SIH-26135 · Ministry of Skill Development
            </div>
          </div>
        </div>

        {/* User info (Phase 30.2 will populate this from auth store) */}
        {username && (
          <div className="flex items-center gap-2 text-sm">
            <div className="text-blue-200 text-xs">{role ?? 'User'}</div>
            <div className="font-medium">{username}</div>
            <div className="h-8 w-8 rounded-full bg-white/20 flex items-center justify-center font-semibold text-sm">
              {username.charAt(0).toUpperCase()}
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
