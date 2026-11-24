import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import './evaluator.css';
import { Modal } from '@/components/common/Modal';
import { useAuth } from '@/context/AuthContext';
import { demoAccounts } from '@/lib/demoAccounts';
import { changeSimulationMode, simulationActive } from '@/lib/demoEnvironment';
import { isRouteAuthorizedForAuthorities } from '@/api/types';
import { accounts, mockSignInProof, sampleCsv } from './simulation/fixtures';
import { journey, stopTarget } from './journey';

type Account = (typeof demoAccounts)[number];
type Confirmation = 'mode' | 'journey' | 'edgeCases';

function accountCanOpen(account: Account, authorities: string[], pathname: string) {
  if (!isRouteAuthorizedForAuthorities(pathname, authorities)) return false;
  if (account.persona !== 'SUPER_ADMIN') return true;
  const normalized = authorities.map((authority) => authority.toLowerCase());
  if (pathname === '/analytics') return normalized.includes('analytics.read');
  return normalized.some((authority) => ['role_super_admin', 'role_government_admin'].includes(authority));
}

function downloadSample() {
  const url = URL.createObjectURL(new Blob([sampleCsv], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = 'sih-sample-trainees.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export default function EvaluatorControls() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [index, setIndex] = useState<number | null>(null);
  const [paused, setPaused] = useState(false);
  const [switching, setSwitching] = useState(false);
  const [awaitingLogin, setAwaitingLogin] = useState(false);
  const [confirmation, setConfirmation] = useState<Confirmation>();
  const [sampleSignIn, setSampleSignIn] = useState<{ path: string; account: Account }>();
  const [handoff, setHandoff] = useState<{ path: string; userId: number }>();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [complete, setComplete] = useState(false);
  const busy = useRef(false);
  const launcher = useRef<HTMLButtonElement>(null);
  const stop = index == null ? undefined : journey[index];
  const matchesPath = stop?.path.split('?')[0] === location.pathname;

  async function openPortal(path: string, account?: Account) {
    const pathname = path.split('?')[0];
    if (!account) {
      navigate(path);
      return;
    }
    const reuseUser = !simulationActive || auth.user?.username === account.username || pathname === '/trainee/consent';
    if (auth.user && reuseUser && accountCanOpen(account, auth.user.authorities, pathname)) {
      if (auth.personas.includes(account.persona)) auth.setActivePersona(account.persona);
      navigate(stopTarget(path, auth.user.id));
      return;
    }
    if (simulationActive) {
      setSampleSignIn({ path, account });
      if (location.pathname !== '/evaluator') navigate('/evaluator');
    } else {
      const target = new URL(stopTarget(path), window.location.origin);
      setAwaitingLogin(true);
      navigate('/login?demo=1&account=' + account.username,
        { state: { from: { pathname: target.pathname, search: target.search } } });
    }
  }

  const run = useCallback(async (action: () => Promise<void>) => {
    if (busy.current) return;
    busy.current = true;
    setSwitching(true);
    setError('');
    try { await action(); }
    catch (err) { setError((err as { message?: string })?.message ?? 'Unable to open this demonstration.'); }
    finally { busy.current = false; setSwitching(false); }
  }, []);

  function finish(completed = false) {
    setIndex(null);
    setPaused(false);
    setAwaitingLogin(false);
    setComplete(completed);
    if (completed) {
      setMessage('All five chapters completed.');
      setOpen(true);
    } else {
      window.requestAnimationFrame(() => launcher.current?.focus());
    }
  }

  function go(next: number) {
    if (next >= journey.length) { finish(true); return; }
    if (busy.current || next < 0) return;
    const target = journey[next];
    setIndex(next);
    setPaused(false);
    setAwaitingLogin(false);
    setComplete(false);
    setOpen(false);
    void run(() => openPortal(target.path, demoAccounts.find((account) => account.username === target.account)));
  }

  const actions = useRef({ go, finish });
  actions.current = { go, finish };

  useEffect(() => {
    if (!sampleSignIn || switching || location.pathname !== '/evaluator') return;
    // Leave the old protected view before replacing its persona and applying the next route guard.
    setSampleSignIn(undefined);
    void run(async () => {
      const current = await auth.login({ usernameOrEmail: sampleSignIn.account.username, password: mockSignInProof });
      setHandoff({ path: stopTarget(sampleSignIn.path, current.userId), userId: current.userId });
    });
  }, [sampleSignIn, switching, location.pathname, auth.login, run]);

  useEffect(() => {
    if (handoff && auth.user?.id === handoff.userId) {
      setHandoff(undefined);
      navigate(handoff.path);
    }
  }, [auth.user?.id, handoff, navigate]);

  useEffect(() => {
    if (awaitingLogin && matchesPath && auth.isAuthenticated) setAwaitingLogin(false);
  }, [auth.isAuthenticated, awaitingLogin, matchesPath]);

  useEffect(() => {
    if (index == null || paused || switching || awaitingLogin || !matchesPath || open || auth.isInitializing) return;
    let instance: ReturnType<typeof driver> | undefined;
    let disposing = false;
    const activate = (target?: Element) => {
      if (instance || disposing) return;
      instance = driver({
        animate: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        disableActiveInteraction: false,
        showProgress: true,
        progressText: 'Stop {{current}} of {{total}}',
        nextBtnText: 'Next stop',
        prevBtnText: 'Previous stop',
        doneBtnText: 'Finish tour',
        popoverClass: 'evaluator-popover',
        onNextClick: () => actions.current.go(index + 1),
        onPrevClick: () => actions.current.go(index - 1),
        onDestroyed: () => { if (!disposing) actions.current.finish(); },
        onPopoverRender: (popover) => {
          const explore = document.createElement('button');
          explore.type = 'button';
          explore.className = 'evaluator-explore';
          explore.textContent = 'Explore this page';
          explore.addEventListener('click', () => setPaused(true));
          popover.footer.prepend(explore);
        },
        steps: journey.map((item, position) => ({
          element: position === index ? target : undefined,
          popover: { title: `${simulationActive ? 'Simulation · ' : ''}Chapter ${item.chapter} of 5 · ${item.title}`, description: item.description },
        })),
      });
      instance.drive(index);
    };
    const reveal = () => {
      const target = document.querySelector(journey[index].anchor);
      if (target) activate(target);
    };
    const observer = new MutationObserver(reveal);
    observer.observe(document.body, { childList: true, subtree: true });
    // A slow or unavailable data view can still display its instructions.
    const fallback = window.setTimeout(() => activate(), 4500);
    const afterDialog = window.setTimeout(reveal, 60);
    return () => {
      disposing = true;
      observer.disconnect();
      window.clearTimeout(fallback);
      window.clearTimeout(afterDialog);
      instance?.destroy();
    };
  }, [index, paused, switching, awaitingLogin, matchesPath, open, auth.isInitializing]);

  function shortcut(path: string, account: Account) {
    finish();
    setOpen(false);
    void run(() => openPortal(path, account));
  }

  function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!confirmation) return;
    void run(async () => {
      if (confirmation === 'mode') { changeSimulationMode(!simulationActive); return; }
      const { resetSimulation } = await import('./simulation/worker');
      resetSimulation(confirmation);
      for (const account of accounts.values()) sessionStorage.removeItem('sih26135_last_import_batch_id.' + account.id);
      auth.logout();
      setIndex(null);
      setPaused(false);
      setComplete(false);
      setMessage(confirmation === 'edgeCases' ? 'Edge-case fixtures loaded.' : 'Journey fixtures reset.');
      setConfirmation(undefined);
      navigate('/evaluator');
    });
  }

  const draftAccount = stop?.account && demoAccounts.find((item) => item.username === stop.account);
  return (
    <>
      <div className="fixed bottom-4 right-4 z-40 flex max-w-[calc(100vw-2rem)] flex-wrap items-center justify-end gap-2">
        {stop && (paused || awaitingLogin || !matchesPath) && (
          <div className="flex flex-wrap items-center gap-2 rounded-lg border border-slate-400 bg-white p-2 text-sm text-navy shadow-md">
            <span>{awaitingLogin ? 'Sign in to continue the tour' : `Chapter ${stop.chapter} of 5`}</span>
            {paused && matchesPath && <>
              <button type="button" className="btn-secondary" onClick={() => setPaused(false)}>Resume guide</button>
              <button type="button" className="btn-primary" disabled={switching} onClick={() => go(index! + 1)}>{index === journey.length - 1 ? 'Finish tour' : 'Next stop'}</button>
            </>}
            {!matchesPath && !awaitingLogin && <button type="button" className="btn-secondary" onClick={() => go(index!)}>Resume this stop</button>}
            <button type="button" className="btn-secondary" onClick={() => finish()}>End tour</button>
          </div>
        )}
        <button ref={launcher} type="button" className="btn-primary evaluator-launcher shadow-lg" aria-haspopup="dialog"
          aria-expanded={open} onClick={() => { setConfirmation(undefined); setOpen(true); }}>
          Evaluator tools
        </button>
      </div>
      {error && !open && <p role="alert" className="fixed bottom-20 right-4 z-40 max-w-xs rounded border border-red-800 bg-red-50 p-3 text-sm text-red-900">{error}</p>}
      {stop && awaitingLogin && draftAccount && location.pathname !== '/login' && !open && (
        <button type="button" className="btn-secondary fixed bottom-20 right-4 z-40" onClick={() => go(index!)}>
          Sign in as {draftAccount.label}
        </button>
      )}
      <Modal open={open} onClose={() => { if (!switching) setOpen(false); }} title="Evaluator tools" className="evaluator-drawer">
        <div className="space-y-5 p-5 text-sm text-slate-800">
          <div className={`rounded-lg border p-3 ${simulationActive ? 'border-amber-800 bg-amber-100 text-amber-950' : 'border-slate-300 bg-slate-50 text-slate-900'}`}>
            <p className="font-bold">{simulationActive ? 'Simulation mode' : 'Live backend mode'}</p>
            <p className="mt-1">{simulationActive ? 'Synthetic fixtures and browser-only actions.' : 'Portal actions use your authenticated backend account.'}</p>
          </div>
          {message && <p role="status" className="rounded border border-green-800 bg-green-50 p-2 text-green-950">{message}</p>}
          {error && <p role="alert" className="rounded border border-red-800 bg-red-50 p-2 text-red-950">{error}</p>}
          {confirmation ? (
            <form className="space-y-3 rounded border border-navy p-3" onSubmit={confirm}>
              <p>{confirmation === 'mode' ? (simulationActive ? 'Return to the live backend?' : 'Use synthetic sample data in this tab?')
                : 'Replace this tab’s sample data and sign out the sample account?'}</p>
              <div className="flex flex-wrap gap-2">
                <button type="submit" className="btn-primary" disabled={switching}>
                  {confirmation === 'mode' ? (simulationActive ? 'Confirm live mode' : 'Confirm simulation') : 'Confirm fixture reset'}
                </button>
                <button type="button" className="btn-secondary" disabled={switching} onClick={() => setConfirmation(undefined)}>Cancel</button>
              </div>
            </form>
          ) : (
            <div className="flex flex-wrap gap-2">
              <button type="button" className="btn-primary" disabled={switching || auth.isInitializing} onClick={() => go(0)}>
                {complete ? 'Restart 5-minute tour' : 'Start 5-minute tour'}
              </button>
              {stop && <button type="button" className="btn-secondary" onClick={() => go(index!)}>Resume current stop</button>}
              <button type="button" className="btn-secondary" disabled={switching} onClick={() => setConfirmation('mode')}>
                {simulationActive ? 'Return to live mode' : 'Enable offline simulation'}
              </button>
            </div>
          )}
          <section aria-labelledby="sample-personas" className="space-y-2">
            <h3 id="sample-personas" className="font-semibold text-navy">Persona shortcuts</h3>
            {!simulationActive && <p>Other accounts use their assigned sign-in credentials.</p>}
            <div className="flex flex-wrap gap-2">
              {demoAccounts.map((account) => <button key={account.username} type="button" className="btn-secondary"
                disabled={switching || auth.isInitializing} onClick={() => shortcut(account.desktop, account)}>{account.label}</button>)}
            </div>
          </section>
          {simulationActive && (
            <section aria-labelledby="sample-fixtures" className="space-y-2">
              <h3 id="sample-fixtures" className="font-semibold text-navy">Sample fixtures</h3>
              <div className="flex flex-wrap gap-2">
                <button type="button" className="btn-secondary" disabled={switching} onClick={downloadSample}>Download sample CSV</button>
                <button type="button" className="btn-secondary" disabled={switching} onClick={() => setConfirmation('journey')}>Reset journey fixtures</button>
                <button type="button" className="btn-secondary" disabled={switching} onClick={() => setConfirmation('edgeCases')}>Load edge-case fixtures</button>
              </div>
            </section>
          )}
          <section aria-labelledby="demo-edge-cases" className="space-y-2">
            <h3 id="demo-edge-cases" className="font-semibold text-navy">Edge-case shortcuts</h3>
            <div className="flex flex-wrap gap-2">
              <button type="button" className="btn-secondary" disabled={switching} onClick={() => shortcut('/provider/quarantine', demoAccounts[1])}>Inspect quarantine</button>
              <button type="button" className="btn-secondary" disabled={switching} onClick={() => shortcut('/communication?view=logs', demoAccounts[0])}>Review consent blocks</button>
              {simulationActive && <button type="button" className="btn-secondary" disabled={switching}
                onClick={() => shortcut('/analytics?stateId=27&districtId=272', demoAccounts[0])}>Show small-cell suppression</button>}
            </div>
          </section>
          <Link to="/evaluator" className="font-semibold text-navy underline" onClick={() => setOpen(false)}>Open local runbook</Link>
        </div>
      </Modal>
    </>
  );
}
