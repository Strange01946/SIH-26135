import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '@/App';

describe('App', () => {
  it('renders without crashing', () => {
    expect(() => render(<App />)).not.toThrow();
  });

  it('renders the portal header', () => {
    render(<App />);
    expect(screen.getByText(/National Skill.*Placement Tracking/i)).toBeInTheDocument();
  });

  it('renders the main navigation', () => {
    render(<App />);
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });
});
