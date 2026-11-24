import { Link } from 'react-router-dom';
import { simulationActive } from '@/lib/demoEnvironment';

const chapters = [
  ['1 minute', 'Candidate registration and consent', 'Show self-registration, required data processing, and consent for follow-up and messaging.'],
  ['1 minute', 'CSV ingestion and quarantine', 'Stage the sample trainee CSV, process it, and inspect the invalid district and duplicate rows.'],
  ['1 minute', 'Assessment, certification, and placement', 'Open the assessor workbench, view the trainee’s certificate, and show the employer’s joined placement.'],
  ['1 minute', 'Employer verification', 'Review attestation, dispute, and exit options, then inspect the three delivery channels and masked contact hints.'],
  ['1 minute', 'Executive MIS', 'Review enrollment and employment KPIs, then open Maharashtra on the map to drill into districts.'],
];

export default function EvaluatorGuide() {
  return (
    <main className="min-h-screen bg-slate-50 px-4 pb-24 pt-8 text-slate-900 sm:px-8">
      <div className="mx-auto max-w-4xl space-y-6">
        <header className="space-y-2 rounded-xl bg-navy px-5 py-6 text-white sm:px-7">
          <h1 className="text-2xl font-bold sm:text-3xl">SIH26135 evaluator runbook</h1>
          <p className="max-w-xl text-sm text-slate-100">Follow a candidate from registration and consent through training, placement, and outcome reporting.</p>
        </header>
        <section className="card space-y-3" aria-labelledby="runbook-start">
          <h2 id="runbook-start" className="text-lg font-bold text-navy">Start the demonstration</h2>
          <p className="text-sm">Open <strong>Evaluator tools</strong> at the bottom of the page and choose <strong>Start 5-minute tour</strong>.</p>
          <p className="text-sm">{simulationActive
            ? 'Simulation is active in this tab. The sample personas switch in one click. Use the drawer to load edge-case fixtures or restart the scenario.'
            : 'Live mode uses the backend and separately provisioned demo accounts. The tour resumes after signing in to the requested persona.'}</p>
          {!simulationActive && <p className="text-sm">With Vite running locally, enable offline simulation explicitly in the drawer to rehearse with synthetic data.</p>}
        </section>
        <section className="card space-y-3" aria-labelledby="runbook-sequence">
          <h2 id="runbook-sequence" className="text-lg font-bold text-navy">Five-minute walkthrough</h2>
          <ol className="divide-y divide-slate-200">
            {chapters.map(([duration, title, instruction]) => (
              <li key={title} className="grid gap-2 py-3 text-sm sm:grid-cols-[6rem_minmax(0,1fr)]">
                <span className="font-semibold text-slate-700">{duration}</span>
                <div><h3 className="font-semibold text-navy">{title}</h3><p className="mt-1">{instruction}</p></div>
              </li>
            ))}
          </ol>
        </section>
        <section className="card space-y-2 text-sm" aria-labelledby="runbook-recovery">
          <h2 id="runbook-recovery" className="text-lg font-bold text-navy">Recovery and edge cases</h2>
          <p>Use <strong>Explore this page</strong> to operate the portal, then use <strong>Resume guide</strong>. Use <strong>Load edge-case fixtures</strong> in simulation to rehearse withdrawn SMS consent and suppressed analytics.</p>
          <p>Resetting sample fixtures replaces browser state in this tab. Keep the local frontend running when the backend is unavailable.</p>
          <Link to="/login" className="inline-block font-semibold text-navy underline">Open sign-in page</Link>
        </section>
      </div>
    </main>
  );
}
