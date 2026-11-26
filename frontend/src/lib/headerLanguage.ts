export type HeaderLanguage = 'en' | 'hi';
const STORAGE_KEY = 'sih_header_language';

export function readHeaderLanguage(): HeaderLanguage {
  try { return localStorage.getItem(STORAGE_KEY) === 'hi' ? 'hi' : 'en'; }
  catch { return 'en'; }
}

export function writeHeaderLanguage(language: HeaderLanguage) {
  try { localStorage.setItem(STORAGE_KEY, language); }
  catch { /* The current page still uses the selected language. */ }
}

export const headerText = {
  en: {
    name: 'National Skill & Placement Tracking Platform',
    subtitle: 'SIH26135 · Ministry of Skill Development and Entrepreneurship',
    logout: 'Sign Out', login: 'Sign In', selectRole: 'Switch operational role',
    role: 'Role:', roleContext: 'Switch Role Context',
    openMenu: 'Open navigation menu', closeMenu: 'Close navigation menu',
  },
  hi: {
    name: 'राष्ट्रीय कौशल एवं रोजगार निगरानी मंच',
    subtitle: 'SIH26135 · कौशल विकास और उद्यमशीलता मंत्रालय',
    logout: 'साइन आउट', login: 'साइन इन', selectRole: 'भूमिका बदलें',
    role: 'भूमिका:', roleContext: 'कार्य भूमिका बदलें',
    openMenu: 'नेविगेशन मेनू खोलें', closeMenu: 'नेविगेशन मेनू बंद करें',
  },
};
