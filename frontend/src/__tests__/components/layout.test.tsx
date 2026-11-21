import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Header } from '@/components/layout/Header';
import { Sidebar } from '@/components/layout/Sidebar';

describe('Header', () => {
  it('renders portal title', () => {
    render(<Header />);
    expect(screen.getByText(/National Skill.*Placement Tracking/i)).toBeInTheDocument();
  });

  it('renders SIH-26135 identifier', () => {
    render(<Header />);
    expect(screen.getByText(/SIH-26135/i)).toBeInTheDocument();
  });

  it('renders username when provided', () => {
    render(<Header username="admin" role="Administrator" />);
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('Administrator')).toBeInTheDocument();
  });

  it('does not render user info without username', () => {
    render(<Header />);
    expect(screen.queryByText('admin')).toBeNull();
  });
});

describe('Sidebar', () => {
  it('renders navigation landmark', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });

  it('renders all nav items without role filter', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Analytics')).toBeInTheDocument();
  });

  it('filters nav items for ROLE_TRAINEE', () => {
    render(
      <MemoryRouter>
        <Sidebar currentRole="ROLE_TRAINEE" />
      </MemoryRouter>,
    );
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.queryByText('User Management')).toBeNull();
    expect(screen.queryByText('Audit Log')).toBeNull();
  });

  it('shows all items for ROLE_ADMIN', () => {
    render(
      <MemoryRouter>
        <Sidebar currentRole="ROLE_ADMIN" />
      </MemoryRouter>,
    );
    expect(screen.getByText('User Management')).toBeInTheDocument();
    expect(screen.getByText('Audit Log')).toBeInTheDocument();
    expect(screen.getByText('Analytics')).toBeInTheDocument();
  });
});
