import { afterEach, describe, expect, it, vi } from 'vitest';

describe('evaluator API routing', () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('keeps live HTTP routing until the local tab explicitly opts in', async () => {
    vi.stubEnv('VITE_LOCAL_DEMO', 'true');
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080');
    const mode = await import('@/lib/demoEnvironment');
    expect(mode.localEvaluatorEnabled).toBe(true);
    expect(mode.simulationActive).toBe(false);
    expect(mode.apiBaseUrl()).toBe('http://localhost:8080');
  });

  it('uses an isolated API prefix in a simulated document', async () => {
    vi.stubEnv('VITE_LOCAL_DEMO', 'true');
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080');
    sessionStorage.setItem('sih_evaluator_simulation', 'enabled');
    const mode = await import('@/lib/demoEnvironment');
    expect(mode.simulationActive).toBe(true);
    expect(mode.apiBaseUrl()).toBe('/__sih_simulation');
    sessionStorage.removeItem('sih_evaluator_simulation');
    expect(mode.apiBaseUrl()).toBe('/__sih_simulation');
  });

  it('ignores previous opt-in when local tooling is disabled', async () => {
    vi.stubEnv('VITE_LOCAL_DEMO', 'false');
    sessionStorage.setItem('sih_evaluator_simulation', 'enabled');
    const mode = await import('@/lib/demoEnvironment');
    expect(mode.localEvaluatorEnabled).toBe(false);
    expect(mode.simulationActive).toBe(false);
  });
});
