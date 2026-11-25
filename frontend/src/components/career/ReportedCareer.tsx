import type { ReportedCareerUpdate } from '@/api/traineeApi';
import { careerDate } from './CareerInsights';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

export function ReportedCareer({ update }: { update: ReportedCareerUpdate }) {
  const details = [
    ['Employment status', update.status],
    ['Monthly income', update.monthlyIncome != null ? inr.format(update.monthlyIncome) : undefined],
    ['Employer or business', update.organization],
    ['Work role', update.role],
    ['Work location', update.location],
    ['Since', update.since ? careerDate(update.since) : undefined],
    ['Skills mentioned in the report', update.skills],
    ['Barriers to work', update.barriers],
  ];

  return (
    <section className="space-y-3 rounded-lg border border-navy-100 bg-navy-50 p-4" aria-label="Current career information">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <h3 className="font-semibold text-navy">Current career information</h3>
        <p className="text-xs text-slate-700">Self-reported · {careerDate(update.submittedAt?.slice(0, 10))}</p>
      </div>
      <dl className="grid gap-3 break-words text-sm sm:grid-cols-2 lg:grid-cols-3">
        {details.filter(([, value]) => value != null && value !== '').map(([label, value]) => (
          <div className="min-w-0" key={label}>
            <dt className="text-slate-700">{label}</dt>
            <dd className="font-medium text-slate-900">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
