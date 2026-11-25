import { useId, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { careerSkillsKey, createCareerCatalogEntry, getCareerCatalogOptions, getCareerSkillsReference,
  saveCareerRoleRequirement, type CareerCatalogKind } from '@/api/careerSkillsApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { CareerOptionField } from './CareerSkillsPanel';

export function CareerSkillsCatalog() {
  const [open, setOpen] = useState(false);
  const [kind, setKind] = useState<CareerCatalogKind>('CATEGORY');
  const [name, setName] = useState('');
  const [parent, setParent] = useState('');
  const queryClient = useQueryClient();
  const options = useQuery({ queryKey: [...careerSkillsKey, 'catalog'], enabled: open,
    queryFn: ({ signal }) => getCareerCatalogOptions(signal), retry: false });
  const create = useMutation({ mutationFn: () => createCareerCatalogEntry(kind, { name: name.trim(), parentId: parent ? Number(parent) : undefined }),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: careerSkillsKey });
    } });
  const parentOptions = kind === 'SKILL' ? options.data?.categories : options.data?.sectors;
  return (
    <details className="card space-y-3 p-4" onToggle={event => setOpen(event.currentTarget.open)}>
      <summary className="cursor-pointer font-semibold text-navy">Skills and job role catalog</summary>
      {open && (options.isPending ? <LoadingSpinner label="Loading catalog references" /> : options.isError ? (
        <ErrorState error={options.error} title="Could not load the skills catalog" onRetry={() => void options.refetch()} />
      ) : (
        <form className="space-y-3" onSubmit={event => { event.preventDefault(); if (!create.isPending) create.mutate(); }}>
          <fieldset className="grid items-end gap-3 sm:grid-cols-3" disabled={create.isPending}>
            <div>
              <label className="form-label" htmlFor="career-catalog-kind">Entry type</label>
              <select id="career-catalog-kind" className="form-input" value={kind} onChange={event => {
                setKind(event.target.value as CareerCatalogKind); setName(''); setParent(''); create.reset();
              }}>
                <option value="CATEGORY">Skill category</option><option value="SKILL">Skill</option><option value="ROLE">Job role</option>
              </select>
            </div>
            <label className="form-label mb-0">{kind === 'CATEGORY' ? 'Category name' : kind === 'SKILL' ? 'Skill name' : 'Job role name'}
              <input className="form-input" value={name} onChange={event => setName(event.target.value)} maxLength={kind === 'ROLE' ? 200 : 150} required />
            </label>
            {kind !== 'CATEGORY' && <div>
              <label className="form-label" htmlFor="career-catalog-parent">{kind === 'SKILL' ? 'Skill category' : 'Sector'}</label>
              <select id="career-catalog-parent" className="form-input" value={parent} onChange={event => setParent(event.target.value)} required>
                <option value="">Select an option</option>
                {parentOptions?.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
            </div>}
          </fieldset>
          {kind === 'SKILL' && !parentOptions?.length && <p className="text-sm text-slate-600">Create a skill category first.</p>}
          <button type="submit" className="btn-primary" disabled={create.isPending || !name.trim() || (kind !== 'CATEGORY' && !parent)}>Add catalog entry</button>
          {create.isSuccess && <p role="status" className="text-sm text-emerald-800">Added {create.data.name}.</p>}
          {create.isError && <p role="alert" className="text-sm text-red-800">{create.error.message}</p>}
        </form>
      ))}
      {open && <RoleRequirementsForm />}
    </details>
  );
}

function RoleRequirementsForm() {
  const headingId = useId();
  const [roleId, setRoleId] = useState('');
  const [skillId, setSkillId] = useState('');
  const [levelId, setLevelId] = useState('');
  const queryClient = useQueryClient();
  const reference = useQuery({
    queryKey: [...careerSkillsKey, 'reference'],
    queryFn: ({ signal }) => getCareerSkillsReference(signal),
    retry: false,
  });
  const save = useMutation({
    mutationFn: () => saveCareerRoleRequirement(Number(roleId), { skillId: Number(skillId), levelId: Number(levelId) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: careerSkillsKey }),
  });
  const canSubmit = !save.isPending && Boolean(roleId && skillId && levelId);

  return (
    <section className="space-y-3 border-t pt-4" aria-labelledby={headingId}>
      <div>
        <h2 id={headingId} className="font-semibold">Set a job role skill requirement</h2>
        <p className="text-sm text-slate-600">Applies to every trainee who chooses this job role.</p>
      </div>
      {reference.isPending ? <LoadingSpinner label="Loading role and skill options" /> : reference.isError ? (
        <ErrorState error={reference.error} title="Could not load role and skill options" onRetry={() => void reference.refetch()} />
      ) : (
        <form className="space-y-3" onSubmit={event => { event.preventDefault(); if (canSubmit) save.mutate(); }}>
          <fieldset className="grid gap-3 sm:grid-cols-3" disabled={save.isPending} onChange={() => save.reset()}>
            <CareerOptionField label="Job role" options={reference.data.roles} value={roleId} onChange={setRoleId} required searchable />
            <CareerOptionField label="Skill" options={reference.data.skills} value={skillId} onChange={setSkillId} required searchable />
            <CareerOptionField label="Required skill level" options={reference.data.levels} value={levelId} onChange={setLevelId} required />
          </fieldset>
          <button className="btn-primary" type="submit" disabled={!canSubmit}>Save role requirement</button>
          {save.isSuccess && <p className="text-sm text-emerald-800" role="status">Role requirement saved.</p>}
          {save.isError && <p className="text-sm text-red-800" role="alert">{save.error.message}</p>}
        </form>
      )}
    </section>
  );
}
