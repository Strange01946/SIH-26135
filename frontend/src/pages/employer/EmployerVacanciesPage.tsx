import { useDeferredValue, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getEmployerVacancies, publishEmployerVacancy, searchEmployerSkills,
  type EmployerReferences, type VacancyInput,
} from '@/api/employerApi';
import { Modal } from '@/components/common/Modal';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmployerFormError, EmployerPageHeading, EmployerPager, EmployerSelect, formatPay, indiaToday, showStatus, useEmployerReferences } from './EmployerUi';

export function EmployerVacanciesPage() {
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [receipt, setReceipt] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const queryClient = useQueryClient();
  const reference = useEmployerReferences();
  const jobs = useQuery({
    queryKey: ['employer', 'vacancies', page],
    queryFn: ({ signal }) => getEmployerVacancies(page, signal),
  });

  return (
    <div className="space-y-5">
      <EmployerPageHeading title="Job vacancies">
        <button type="button" className="btn-primary" onClick={() => setOpen(true)}>New vacancy</button>
      </EmployerPageHeading>
      {receipt && <p role="status" className="rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{receipt}</p>}
      {jobs.isPending ? <LoadingSpinner label="Loading vacancies" /> : jobs.isError ? (
        <ErrorState error={jobs.error} title="Unable to load vacancies" onRetry={() => void jobs.refetch()} />
      ) : jobs.data.content.length === 0 ? (
        <EmptyState title="No vacancies yet" description="Publish a vacancy to record the role, location, pay and skills your organisation needs." />
      ) : (
        <>
          <ul className="grid gap-4 lg:grid-cols-2">
            {jobs.data.content.map((job) => (
              <li key={job.id} className="card space-y-3">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <h2 className="text-lg font-semibold text-navy">{job.title}</h2>
                    <p className="text-sm text-slate-600">{job.jobRole} · {job.district}</p>
                  </div>
                  <span className="badge-info">{showStatus(job.status)}</span>
                </div>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div><dt className="text-slate-600">Pay range{job.payFrequency && ` · ${job.payFrequency}`}</dt>
                    <dd className="font-medium">{formatPay(job.minimumPay, job.currency)} – {formatPay(job.maximumPay, job.currency)}</dd></div>
                  <div><dt className="text-slate-600">Openings</dt><dd>{job.vacancies}</dd></div>
                  <div><dt className="text-slate-600">Qualification</dt><dd>{job.qualification || '—'}</dd></div>
                  <div><dt className="text-slate-600">Closes</dt><dd>{job.closesOn || '—'}</dd></div>
                </dl>
                {job.skills.length > 0 && (
                  <ul aria-label="Required skills" className="flex flex-wrap gap-2">
                    {job.skills.map((skill) => (
                      <li className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-800" key={skill.id}>
                        {skill.name} · {skill.level}
                      </li>
                    ))}
                  </ul>
                )}
                <p className="text-xs text-slate-500">{job.code}</p>
              </li>
            ))}
          </ul>
          <EmployerPager data={jobs.data} onPage={setPage} />
        </>
      )}
      <Modal open={open} onClose={() => { if (!submitting) setOpen(false); }} title="Publish a job vacancy" className="max-w-2xl">
        {reference.isPending ? <LoadingSpinner label="Loading vacancy options" /> : reference.isError ? (
          <ErrorState error={reference.error} title="Unable to load vacancy options" onRetry={() => void reference.refetch()} />
        ) : open && (
          <VacancyForm references={reference.data} onPending={setSubmitting} onPublished={async (title) => {
            setOpen(false);
            setPage(0);
            setReceipt(`Vacancy published · ${title}`);
            await queryClient.invalidateQueries({ queryKey: ['employer', 'vacancies'] });
          }} />
        )}
      </Modal>
    </div>
  );
}

function VacancyForm({ references, onPublished, onPending }: {
  references: EmployerReferences;
  onPublished: (title: string) => Promise<void>;
  onPending: (pending: boolean) => void;
}) {
  const [search, setSearch] = useState('');
  const [skillId, setSkillId] = useState('');
  const [skillLevel, setSkillLevel] = useState('');
  const [selected, setSelected] = useState<{ skillId: number; levelId: number; label: string }[]>([]);
  const [formError, setFormError] = useState('');
  const searched = useDeferredValue(search.trim());
  const skills = useQuery({
    queryKey: ['employer', 'skills', searched],
    queryFn: ({ signal }) => searchEmployerSkills(searched, signal),
  });
  const submit = useMutation({
    mutationFn: publishEmployerVacancy,
    onMutate: () => onPending(true),
    onSuccess: (job) => onPublished(job.title),
    onSettled: () => onPending(false),
  });

  function addSkill() {
    const record = skills.data?.find(({ id }) => id === Number(skillId));
    if (!record || !skillLevel || selected.some(({ skillId }) => skillId === record.id)) return;
    setSelected([...selected, { skillId: record.id, levelId: Number(skillLevel), label: record.label }]);
    setSkillId('');
    setSkillLevel('');
    setFormError('');
  }

  function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submit.isPending) return;
    const form = new FormData(event.currentTarget);
    const read = (field: string) => String(form.get(field) ?? '').trim();
    const input: VacancyInput = {
      title: read('title'), description: read('description') || undefined,
      jobRoleId: Number(read('jobRoleId')), engagementTypeId: Number(read('engagementTypeId')),
      qualificationId: Number(read('qualificationId')), districtId: Number(read('districtId')),
      vacancies: Number(read('vacancies')), minimumPay: Number(read('minimumPay')), maximumPay: Number(read('maximumPay')),
      payFrequencyId: Number(read('payFrequencyId')), closingDate: read('closingDate') || undefined,
      skills: selected.map(({ skillId, levelId }) => ({ skillId, levelId })),
    };
    if (!input.title || input.minimumPay > input.maximumPay || selected.length === 0) {
      setFormError(!input.title ? 'Enter a vacancy title.'
        : input.minimumPay > input.maximumPay ? 'Maximum pay must be at least the minimum pay.'
          : 'Add at least one required skill.');
      return;
    }
    setFormError('');
    submit.mutate(input);
  }

  return (
    <form onSubmit={publish} className="space-y-4 p-6">
      <fieldset disabled={submit.isPending} className="grid gap-4 sm:grid-cols-2">
        <label className="space-y-1 text-sm font-medium sm:col-span-2">
          <span>Vacancy title</span><input name="title" className="form-input" required maxLength={200} />
        </label>
        <EmployerSelect name="jobRoleId" label="Job role" options={references.jobRoles} required />
        <EmployerSelect name="engagementTypeId" label="Engagement type" options={references.engagementTypes} required />
        <EmployerSelect name="qualificationId" label="Required qualification" options={references.qualifications} required />
        <EmployerSelect name="districtId" label="Work district" options={references.districts} required />
        <label className="space-y-1 text-sm font-medium"><span>Number of openings</span>
          <input name="vacancies" className="form-input" type="number" min={1} max={65535} defaultValue={1} required /></label>
        <EmployerSelect name="payFrequencyId" label="Pay frequency" options={references.payFrequencies} required />
        <label className="space-y-1 text-sm font-medium"><span>Minimum pay (INR)</span>
          <input name="minimumPay" className="form-input" type="number" min={0} max={9999999999.99} step={0.01} required /></label>
        <label className="space-y-1 text-sm font-medium"><span>Maximum pay (INR)</span>
          <input name="maximumPay" className="form-input" type="number" min={0} max={9999999999.99} step={0.01} required /></label>
        <label className="space-y-1 text-sm font-medium"><span>Closing date</span>
          <input name="closingDate" type="date" className="form-input" min={indiaToday()} /></label>
        <label className="space-y-1 text-sm font-medium sm:col-span-2"><span>Job description</span>
          <textarea name="description" rows={3} maxLength={8000} className="form-input" /></label>
      </fieldset>
      <fieldset disabled={submit.isPending} className="space-y-2 rounded-lg border border-slate-200 p-3">
        <legend className="px-1 text-sm font-semibold">Required skills</legend>
        <label className="block space-y-1 text-sm font-medium"><span>Search skills by name or code</span>
          <input className="form-input" value={search} onChange={(event) => { setSearch(event.target.value); setSkillId(''); }} maxLength={100} /></label>
        {skills.isError && <p role="alert" className="text-sm text-red-800">Skill search failed. <button type="button" className="underline" onClick={() => void skills.refetch()}>Retry</button></p>}
        {skills.isSuccess && skills.data.length === 0 && <p className="text-sm text-slate-600">No active skills match this search.</p>}
        <div className="grid items-end gap-2 sm:grid-cols-[2fr_1fr_auto]">
          <EmployerSelect label="Skill" value={skillId} onChange={(event) => setSkillId(event.target.value)}
            options={(skills.data ?? []).filter(({ id }) => !selected.some(({ skillId }) => skillId === id))} />
          <EmployerSelect label="Skill level" value={skillLevel} onChange={(event) => setSkillLevel(event.target.value)} options={references.skillLevels} />
          <button type="button" className="btn-outline" onClick={addSkill} disabled={!skillId || !skillLevel || selected.length >= 20}>Add skill</button>
        </div>
        {selected.length > 0 && (
          <ul className="space-y-1">
            {selected.map((skill) => (
              <li key={skill.skillId} className="flex items-center justify-between gap-2 text-sm">
                <span>{skill.label} · {references.skillLevels.find(({ id }) => id === skill.levelId)?.label}</span>
                <button type="button" className="text-navy underline" aria-label={`Remove ${skill.label}`} onClick={() => setSelected(selected.filter(({ skillId }) => skillId !== skill.skillId))}>Remove</button>
              </li>
            ))}
          </ul>
        )}
      </fieldset>
      {formError && <p role="alert" className="text-sm text-red-800">{formError}</p>}
      {submit.isError && <EmployerFormError error={submit.error} />}
      <button type="submit" className="btn-primary" disabled={submit.isPending}>{submit.isPending ? 'Publishing…' : 'Publish vacancy'}</button>
    </form>
  );
}
