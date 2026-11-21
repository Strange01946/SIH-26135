import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '@/App';

describe('App', () => {
  it('renders without crashing', () => {
    expect(() => render(<App />)).not.toThrow();
  });

  it('renders the portal branding and national emblem', () => {
    render(<App />);
    expect(screen.getByText(/National Skill.*Placement Tracking/i)).toBeInTheDocument();
  });

  it('renders the official sign in portal on initial unauthenticated visit', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: /Sign In to Official Portal/i })).toBeInTheDocument();
  });
});
