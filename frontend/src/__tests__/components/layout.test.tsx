import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Header } from '@/components/layout/Header';
import { Sidebar } from '@/components/layout/Sidebar';
import { AppLayout } from '@/components/layout/AppLayout';

describe('Header', () => {
  it('renders portal title', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );
    expect(screen.getByText(/National Skill.*Placement Tracking/i)).toBeInTheDocument();
  });

  it('renders SIH-26135 identifier', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );
    expect(screen.getByText(/SIH26135/i)).toBeInTheDocument();
  });

  it('renders username when provided', () => {
    render(
      <MemoryRouter>
        <Header username="admin" role="Administrator" />
      </MemoryRouter>,
    );
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('Administrator')).toBeInTheDocument();
  });

  it('does not render user info without username', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );
    expect(screen.queryByText('admin')).toBeNull();
  });

  it('renders mobile menu button and calls handler on click', () => {
    const handleToggle = vi.fn();
    render(
      <MemoryRouter>
        <Header onToggleMobileMenu={handleToggle} isMobileMenuOpen={false} />
      </MemoryRouter>,
    );
    const button = screen.getByRole('button', { name: 'Open navigation menu' });
    expect(button).toBeInTheDocument();
    fireEvent.click(button);
    expect(handleToggle).toHaveBeenCalledTimes(1);
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
    expect(screen.getByText('Admin Console')).toBeInTheDocument();
    expect(screen.getByText('Provider Console')).toBeInTheDocument();
  });

  it('filters nav items for ROLE_TRAINEE', () => {
    render(
      <MemoryRouter>
        <Sidebar currentRole="ROLE_TRAINEE" />
      </MemoryRouter>,
    );
    expect(screen.getByText('Trainee Portal')).toBeInTheDocument();
    expect(screen.queryByText('User Provisioning')).toBeNull();
    expect(screen.queryByText('Provider Console')).toBeNull();
  });

  it('shows all items for ROLE_ADMIN', () => {
    render(
      <MemoryRouter>
        <Sidebar currentRole="ROLE_ADMIN" />
      </MemoryRouter>,
    );
    expect(screen.getByText('Admin Console')).toBeInTheDocument();
    expect(screen.getByText('Onboarding Requests')).toBeInTheDocument();
    expect(screen.getByText('User Provisioning')).toBeInTheDocument();
  });

  it('renders mobile drawer when isMobileOpen is true', () => {
    const handleClose = vi.fn();
    render(
      <MemoryRouter>
        <Sidebar isMobileOpen={true} onCloseMobile={handleClose} />
      </MemoryRouter>,
    );
    expect(screen.getByRole('navigation', { name: 'Mobile navigation' })).toBeInTheDocument();
    const closeBtn = screen.getByRole('button', { name: 'Close menu' });
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});

describe('AppLayout', () => {
  it('renders skip to main content link for accessibility', () => {
    render(
      <MemoryRouter>
        <AppLayout />
      </MemoryRouter>,
    );
    const skipLink = screen.getByText('Skip to main content');
    expect(skipLink).toBeInTheDocument();
    expect(skipLink).toHaveAttribute('href', '#main-content');
  });
});
