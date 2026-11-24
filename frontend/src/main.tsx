import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { changeSimulationMode, simulationActive } from './lib/demoEnvironment';
import './styles/globals.css';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element #root not found in DOM');
}

const root = ReactDOM.createRoot(rootElement);

async function mount() {
  if (simulationActive) {
    root.render(<p role="status" className="p-6 font-semibold text-navy">Starting offline simulation…</p>);
    try {
      const { startSimulation } = await import('./evaluator/simulation/worker');
      await startSimulation();
    } catch {
      root.render(
        <main className="mx-auto max-w-lg space-y-4 p-6">
          <h1 className="text-xl font-bold text-navy">Offline simulation could not start</h1>
          <p role="alert">Check that service workers are enabled for this local site.</p>
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn-primary" onClick={() => window.location.reload()}>Retry simulation</button>
            <button type="button" className="btn-secondary" onClick={() => changeSimulationMode(false)}>Return to live mode</button>
          </div>
        </main>,
      );
      return;
    }
  }

  root.render(<React.StrictMode><App /></React.StrictMode>);
}

void mount();
