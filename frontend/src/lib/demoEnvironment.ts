const loopback = typeof window !== 'undefined'
  && ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);

export const localEvaluatorEnabled = import.meta.env.VITE_LOCAL_DEMO === 'true' && loopback;
export const simulationBaseUrl = '/__sih_simulation';
const SIMULATION_KEY = 'sih_evaluator_simulation';
export const simulationRefreshKey = 'sih_evaluator_refresh_token';

function hasSimulationOptIn(): boolean {
  try {
    return localEvaluatorEnabled && sessionStorage.getItem(SIMULATION_KEY) === 'enabled';
  } catch {
    return false;
  }
}

// The browsing document retains one mode throughout its lifetime.
export const simulationActive = hasSimulationOptIn();

export function apiBaseUrl(): string {
  return simulationActive ? simulationBaseUrl : import.meta.env.VITE_API_BASE_URL ?? '';
}

export function changeSimulationMode(enabled: boolean): void {
  if (!localEvaluatorEnabled) throw new Error('Evaluator tooling is available on loopback only.');
  if (enabled) sessionStorage.setItem(SIMULATION_KEY, 'enabled');
  else {
    sessionStorage.removeItem(SIMULATION_KEY);
    sessionStorage.removeItem(simulationRefreshKey);
  }
  window.location.assign('/evaluator');
}
