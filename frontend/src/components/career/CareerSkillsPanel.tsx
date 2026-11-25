import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { careerSkillsKey, getCareerSkillsReference, getOwnCareerSkills, getStaffCareerSkills, getAssessmentCareerSkills,
  recordAssessmentSkill, saveOwnCareerSkills, type CareerOption, type CareerSkillsProfile,
  type CareerSkillsReference, type SaveCareerSkills } from '@/api/careerSkillsApi';
import type { ApiError } from '@/api/types';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { simulationActive } from '@/lib/demoEnvironment';
import { matchKeywords } from '@/lib/keywordSearch';

function useCareerReference(enabled: boolean) {
  return useQuery({ queryKey: [...careerSkillsKey, 'reference'], queryFn: ({ signal }) => getCareerSkillsReference(signal), enabled });
}

export function OwnCareerSkills({ traineeId, edit = false }: { traineeId: number; edit?: boolean }) {
  const { hash } = useLocation();
  const section = useRef<HTMLElement>(null);
  const queryClient = useQueryClient();
  const key = [...careerSkillsKey, 'me', traineeId] as const;
  const profile = useQuery({ queryKey: key, queryFn: ({ signal }) => getOwnCareerSkills(signal),
    enabled: !simulationActive, refetchOnMount: 'always', refetchOnWindowFocus: 'always', retry: false });
  const reference = useCareerReference(edit && !simulationActive);
  const save = useMutation<CareerSkillsProfile, ApiError, SaveCareerSkills>({ mutationFn: saveOwnCareerSkills, onSuccess: async (saved) => {
    queryClient.setQueryData(key, saved);
    await queryClient.invalidateQueries({ queryKey: [...careerSkillsKey, 'training'] });
  } });

  useEffect(() => {
    if (edit && hash === '#career-skills') section.current?.focus();
  }, [edit, hash]);

  if (simulationActive) return null;
  return (
    <section ref={section} id={edit ? 'career-skills' : undefined} tabIndex={edit ? -1 : undefined}
      className="card scroll-mt-24 space-y-4 p-5" aria-label="My skills and job goal">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="section-title">My skills and job goal</h2>
        {edit ? <Link className="btn-secondary" to="/trainee/journey#training-suggestions">See upcoming training</Link>
          : <Link className="btn-secondary" to="/trainee/profile#career-skills">Edit skills and job goal</Link>}
      </div>
      {profile.isPending ? <LoadingSpinner label="Loading career skills" /> : profile.isError ? (
        <ErrorState error={profile.error} title="Could not load career skills" onRetry={() => void profile.refetch()} />
      ) : <>
        <CareerSkillsDetails profile={profile.data} />
        {edit && (reference.isPending ? <LoadingSpinner label="Loading skills and job roles" /> : reference.isError ? (
          <ErrorState error={reference.error} title="Could not load skill options" onRetry={() => void reference.refetch()} />
        ) : <CareerSkillsForm key={profile.data.revision} profile={profile.data} reference={reference.data}
          saving={save.isPending} save={input => save.mutate(input)} />)}
        {save.isSuccess && <p role="status" className="text-sm text-emerald-800">Skills and job goal saved.</p>}
        {save.isError && <p role="alert" className="text-sm text-red-800">{save.error.message}{' '}
          {save.error.status === 409 && <button type="button" className="underline" onClick={() => {
            void profile.refetch(); save.reset();
          }}>Reload saved profile</button>}</p>}
      </>}
    </section>
  );
}

type EditableSkill = { id: number; name: string; levelId?: number };

function CareerSkillsForm({ profile, reference, saving, save }: {
  profile: CareerSkillsProfile; reference: CareerSkillsReference; saving: boolean; save: (input: SaveCareerSkills) => void;
}) {
  const catalogRole = reference.roles.find(role => role.id === profile.desiredRoleId && role.name === profile.desiredJobTitle);
  const [jobRole, setJobRole] = useState(String(catalogRole?.id ?? ''));
  const [jobTitle, setJobTitle] = useState(catalogRole ? '' : profile.desiredJobTitle ?? '');
  const [skills, setSkills] = useState<EditableSkill[]>(profile.skills.map((skill, id) => ({ ...skill, id })));
  const [nextId, setNextId] = useState(skills.length);

  function changeSkill(id: number, update: Partial<EditableSkill>) {
    setSkills(previous => previous.map(skill => skill.id === id ? { ...skill, ...update } : skill));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    save({ revision: profile.revision, desiredRoleId: jobRole ? Number(jobRole) : undefined,
      desiredJobTitle: jobRole ? undefined : jobTitle.trim(),
      skills: skills.map(({ name, levelId }) => ({ name: name.trim(), levelId })) });
  }

  return (
    <form className="space-y-4 border-t pt-4" onSubmit={submit}>
      <fieldset disabled={saving} className="space-y-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <CareerOptionField label="Desired job role" options={reference.roles} value={jobRole} onChange={setJobRole} empty="Enter a job title below" searchable />
          {!jobRole && <label className="form-label">Desired job title
            <input className="form-input" value={jobTitle} onChange={event => setJobTitle(event.target.value)} maxLength={200} />
          </label>}
        </div>
        <div className="space-y-3">
          <h3 className="font-semibold text-navy">Skills I have</h3>
          <datalist id="career-known-skills">{reference.skills.map(skill => <option key={skill.id} value={skill.name} />)}</datalist>
          {skills.map((skill, index) => (
            <div key={skill.id} className="grid items-end gap-2 rounded border border-slate-200 p-3 sm:grid-cols-[1fr_1fr_auto]">
              <label className="form-label mb-0">Skill name {index + 1}
                <input className="form-input" list="career-known-skills" value={skill.name} maxLength={150} required
                  onChange={event => changeSkill(skill.id, { name: event.target.value })} />
              </label>
              <CareerOptionField label={`Skill level ${index + 1}`} options={reference.levels} value={String(skill.levelId ?? '')}
                onChange={value => changeSkill(skill.id, { levelId: value ? Number(value) : undefined })} empty="Level not specified" />
              <button className="btn-secondary" type="button" aria-label={`Remove skill ${index + 1}`}
                onClick={() => setSkills(items => items.filter(item => item.id !== skill.id))}>Remove</button>
            </div>
          ))}
          <button type="button" className="btn-secondary" disabled={skills.length >= 50} onClick={() => {
            setSkills(previous => [...previous, { id: nextId, name: '' }]); setNextId(value => value + 1);
          }}>Add a skill</button>
        </div>
        <button type="submit" className="btn-primary">{saving ? 'Saving…' : 'Save skills and job goal'}</button>
      </fieldset>
    </form>
  );
}

export function CareerSkillsDetails({ profile, staffView = false }: { profile: CareerSkillsProfile; staffView?: boolean }) {
  const emptyRequirements = staffView ? 'This trainee hasn’t set a job goal yet.' : 'Set a job goal to compare your skills.';
  return (
    <div className="space-y-4 text-sm">
      <p><strong>Desired job</strong> · {profile.desiredJobTitle || 'No job goal entered'}</p>
      <div className="grid gap-4 md:grid-cols-2">
        <div><h3 className="font-semibold">Self-reported skills</h3>
          {profile.skills.length ? <ul className="mt-1 flex flex-wrap gap-2">{profile.skills.map(skill => (
            <li key={skill.name} className="rounded bg-navy-50 px-2 py-1">{skill.name}{skill.level ? ` · ${skill.level}` : ''}</li>
          ))}</ul> : <p className="text-slate-600">No skills added.</p>}
        </div>
        <div><h3 className="font-semibold">Assessed skills</h3>
          {profile.evidence.length ? <ul className="mt-1 flex flex-wrap gap-2">{profile.evidence.map(item => (
            <li key={item.skillId} className="rounded bg-emerald-50 px-2 py-1">{item.skill} · {item.level}</li>
          ))}</ul> : <p className="text-slate-600">No skill observations recorded.</p>}
        </div>
      </div>
      <div>
        <h3 className="font-semibold">Skills for the desired job</h3>
        {profile.requirements.length ? <ul className="mt-2 divide-y rounded border border-slate-200">
          {profile.requirements.map(item => <li key={item.skillId} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
            <div><strong>{item.skill}</strong><p className="text-slate-600">Required {item.requiredLevel} · {item.currentLevel || 'Level not recorded'}
              {item.currentLevel && (item.source === 'SELF_REPORTED' ? ' (self-reported)' : ' (assessed)')}</p></div>
            <span className={item.needsTraining ? 'badge-warning' : 'badge-success'}>
              {item.needsTraining ? item.currentRank == null ? 'Level unknown' : 'Skill gap' : 'Meets level'}
            </span>
          </li>)}
        </ul> : <p className="text-slate-600">{profile.desiredJobTitle ? 'No skill requirements are recorded for this job.' : emptyRequirements}</p>}
      </div>
    </div>
  );
}

export function StaffCareerSkills({ traineeId, assessmentId }: { traineeId: number; assessmentId?: number }) {
  const key = [...careerSkillsKey, 'staff', traineeId, assessmentId] as const;
  const profile = useQuery({
    queryKey: key,
    queryFn: ({ signal }) => assessmentId
      ? getAssessmentCareerSkills(assessmentId, traineeId, signal) : getStaffCareerSkills(traineeId, signal),
    retry: false, refetchOnMount: 'always', refetchOnWindowFocus: 'always', refetchInterval: assessmentId ? false : 15_000,
  });
  const title = assessmentId ? 'Candidate skill observations' : 'Candidate skills and job goal';

  return (
    <section className="card space-y-4 p-5" aria-label={title}>
      <h2 className="section-title">{title}</h2>
      {profile.isPending ? <LoadingSpinner label="Loading candidate skills" /> : profile.isError ? (
        <ErrorState error={profile.error} title="Could not load candidate skills" onRetry={() => void profile.refetch()} />
      ) : <CareerSkillsDetails profile={profile.data} staffView />}
      {assessmentId && profile.data && <SkillObservationForm traineeId={traineeId} assessmentId={assessmentId} />}
    </section>
  );
}

function SkillObservationForm({ traineeId, assessmentId }: { traineeId: number; assessmentId: number }) {
  const queryClient = useQueryClient();
  const reference = useCareerReference(true);
  const [skillId, setSkillId] = useState('');
  const [levelId, setLevelId] = useState('');
  const save = useMutation({
    mutationFn: () => recordAssessmentSkill(assessmentId, traineeId, { skillId: Number(skillId), levelId: Number(levelId) }),
    onSuccess: async updated => {
      queryClient.setQueryData([...careerSkillsKey, 'staff', traineeId, assessmentId], updated);
      await queryClient.invalidateQueries({ queryKey: careerSkillsKey });
    },
  });
  const canSubmit = !save.isPending && Boolean(skillId && levelId);

  if (reference.isPending) return <LoadingSpinner label="Loading skill options" />;
  if (reference.isError) return <ErrorState error={reference.error} title="Could not load skill options"
    onRetry={() => void reference.refetch()} />;

  return (
    <form className="space-y-3 border-t pt-4" onSubmit={event => { event.preventDefault(); if (canSubmit) save.mutate(); }}>
      <h3 className="font-semibold">Record an observed skill level</h3>
      <fieldset className="grid gap-3 sm:grid-cols-2" disabled={save.isPending}>
        <CareerOptionField label="Skill" options={reference.data.skills} value={skillId} onChange={setSkillId} required searchable />
        <CareerOptionField label="Observed skill level" options={reference.data.levels} value={levelId} onChange={setLevelId} required />
      </fieldset>
      <button className="btn-primary" type="submit" disabled={!canSubmit}>Save observation</button>
      {save.isSuccess && <p className="text-sm text-emerald-800" role="status">Observation saved.</p>}
      {save.isError && <p className="text-sm text-red-800" role="alert">{save.error.message}</p>}
    </form>
  );
}

export function CareerOptionField({ label, value, options, onChange, empty = 'Select an option', required = false, searchable = false }: {
  label: string; value: string; options: CareerOption[]; onChange: (value: string) => void; empty?: string; required?: boolean; searchable?: boolean;
}) {
  const id = useId();
  const [search, setSearch] = useState('');
  const matches = matchKeywords(options, search, item => item.name);
  const selected = options.find(item => String(item.id) === value);
  return <div>
    {searchable && <input type="search" className="form-input mb-2" aria-label={`Search ${label.toLowerCase()}`}
      placeholder={`Search ${label.toLowerCase()}`} maxLength={120} value={search} onChange={event => setSearch(event.target.value)} />}
    <label className="form-label" htmlFor={id}>{label}</label>
    <select id={id} className="form-input" value={value} onChange={event => onChange(event.target.value)} required={required}>
      <option value="">{search && !matches.length ? 'No matching options' : empty}</option>
      {selected && !matches.includes(selected) && <option value={selected.id}>{selected.name}</option>}
      {matches.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
    </select>
  </div>;
}
