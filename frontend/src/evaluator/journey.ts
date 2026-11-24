import { simulationActive } from '@/lib/demoEnvironment';
import { sampleBatchId } from './simulation/fixtures';

type Stop = {
  chapter: number;
  account?: string;
  path: string;
  anchor: string;
  title: string;
  description: string;
};

export const journey: Stop[] = [
  { chapter: 1, path: '/register', anchor: '[data-evaluator-registration]',
    title: 'Candidate registration',
    description: 'Show candidate self-registration with district and demographic references. Use a synthetic candidate when rehearsing.' },
  { chapter: 1, account: 'trainee_user', path: '/trainee/consent', anchor: '#consent-management-heading',
    title: 'DPDP consent',
    description: 'Show mandatory data processing and the separate choices for employment follow-up and messaging. Optional permissions can be withdrawn.' },
  { chapter: 2, account: 'provider_user', path: '/provider/imports', anchor: '#main-content h1',
    title: 'Bulk multipart CSV intake',
    description: 'Select Trainee, stage a sample CSV, and process it. The import status tracks accepted, failed, and duplicate rows.' },
  { chapter: 2, account: 'provider_user', path: '/provider/quarantine', anchor: '#main-content h1',
    title: 'Quarantine and correction',
    description: 'Inspect row numbers and validation codes before correcting and resubmitting a file. Export the quarantined rows for review.' },
  { chapter: 3, account: 'assessor_user', path: '/assessor/dashboard', anchor: '#main-content h1',
    title: 'Assessment and scoring',
    description: 'Open the assigned Solar PV assessment, enter component scores, and review the difference between drafts and finalized results.' },
  { chapter: 3, account: 'trainee_user', path: '/trainee/assessments', anchor: '#certifications-heading',
    title: 'Candidate certificates',
    description: 'Show the issued certificate linked to the assessed course. The learner can review scores and download the certificate.' },
  { chapter: 3, account: 'employer_user', path: '/employer/placements', anchor: '#main-content h1',
    title: 'Placement and joining',
    description: 'Explain how a joined placement captures the job role, joining date, and starting wage for later outcome tracking.' },
  { chapter: 4, account: 'employer_user', path: '/employer/verifications', anchor: '#main-content h1',
    title: 'Employer verification',
    description: 'Open a pending request to demonstrate attesting employment, disputing a claim, or reporting an exit. The form accepts evidence when required.' },
  { chapter: 4, account: 'admin_user', path: '/communication?view=logs', anchor: '#delivery-logs',
    title: 'Email, SMS, and WhatsApp follow-up',
    description: 'Review channel and delivery status for verification notices. Masked contact hints and consent blocks support safe follow-up.' },
  { chapter: 5, account: 'admin_user', path: '/analytics', anchor: '[data-evaluator-mis]',
    title: 'Executive KPIs and geographic heatmap',
    description: 'Compare enrollments, certificates, placements, and six-month retention. Select Maharashtra on the map to show district outcomes and small-cell suppression.' },
];

export function stopTarget(path: string, userId?: number) {
  if (path !== '/provider/imports' && path !== '/provider/quarantine') return path;
  if (path === '/provider/imports' && !simulationActive) return path;
  try {
    const storage = simulationActive ? sessionStorage : localStorage;
    const saved = userId ? storage.getItem('sih26135_last_import_batch_id.' + userId) : null;
    if (saved && /^[1-9]\d*$/.test(saved)) return path + '?batchId=' + saved;
  } catch {
    // The default sample remains available when storage is restricted.
  }
  return simulationActive ? path + '?batchId=' + sampleBatchId : path;
}
