import { localEvaluatorEnabled } from './demoEnvironment';

export const demoAccounts = [
  { username: 'admin_user', label: 'Administrator', persona: 'SUPER_ADMIN', desktop: '/analytics' },
  { username: 'provider_user', label: 'Training provider', persona: 'TRAINING_PROVIDER', desktop: '/provider/imports' },
  { username: 'assessor_user', label: 'Assessor', persona: 'ASSESSOR', desktop: '/assessor/dashboard' },
  { username: 'employer_user', label: 'Employer', persona: 'EMPLOYER', desktop: '/employer/verifications' },
  { username: 'trainee_user', label: 'Trainee', persona: 'TRAINEE', desktop: '/trainee/consent' },
] as const;

export const localDemoAccountsEnabled = localEvaluatorEnabled;
