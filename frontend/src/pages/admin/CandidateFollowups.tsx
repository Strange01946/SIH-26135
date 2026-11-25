import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getCandidateFollowups, getCandidateSurveySubmission, scheduleCandidateFollowup } from '@/api/careerApi';
import { careerDate } from '@/components/career/CareerInsights';
import { SurveySubmissions } from '@/components/career/SurveySubmissions';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function CandidateFollowups({ traineeId }: { traineeId: number }) {
  const queryClient = useQueryClient();
  const today = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' }).format(new Date());
  const [campaignId, setCampaignId] = useState('');
  const [scheduledOn, setScheduledOn] = useState(today);
  const key = ['career', 'candidate-surveys', traineeId] as const;
  const followups = useQuery({
    queryKey: key, queryFn: ({ signal }) => getCandidateFollowups(traineeId, signal), retry: false,
    refetchOnWindowFocus: 'always',
  });
  const schedule = useMutation({
    mutationFn: (input: { campaignId: number; scheduledOn: string }) => scheduleCandidateFollowup(traineeId, input),
    onSuccess: async () => {
      setCampaignId('');
      await queryClient.invalidateQueries({ queryKey: key });
    },
  });
  const campaign = followups.data?.campaigns.find(item => String(item.id) === campaignId);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (campaign && !schedule.isPending) schedule.mutate({ campaignId: campaign.id, scheduledOn });
  }

  return (
    <section className="card space-y-4 p-4 sm:p-5" aria-labelledby="candidate-followups-heading">
      <h2 className="section-title" id="candidate-followups-heading">Candidate follow-up surveys</h2>
      {followups.isPending ? <LoadingSpinner label="Loading candidate follow-ups" /> : followups.isError ? (
        <ErrorState title="Could not load candidate follow-ups" error={followups.error} onRetry={() => void followups.refetch()} />
      ) : <>
        {followups.data.followupConsentGranted ? <>
          {followups.data.campaigns.length > 0 ? <form className="space-y-3" onSubmit={submit}>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label className="form-label" htmlFor="candidate-campaign">Campaign</label>
                <select id="candidate-campaign" className="form-input" required value={campaignId}
                  onChange={event => { setCampaignId(event.target.value); schedule.reset(); }}>
                  <option value="">Select a campaign</option>
                  {followups.data.campaigns.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
                </select>
              </div>
              <label className="form-label">Available from
                <input className="form-input" type="date" required min={today} max={campaign?.closesOn ?? undefined}
                  value={scheduledOn} onChange={event => setScheduledOn(event.target.value)} />
              </label>
            </div>
            {campaign && <p className="text-sm text-slate-600">{campaign.survey}
              {campaign.closesOn && ` · Closes ${careerDate(campaign.closesOn)}`}</p>}
            <button type="submit" className="btn-primary" disabled={!campaign || schedule.isPending}>Assign survey</button>
          </form> : <p className="text-sm text-slate-600">No additional open campaigns are available for this candidate.</p>}
          <SurveySubmissions entries={followups.data.recentSubmissions} scopeKey={key}
            load={(id, signal) => getCandidateSurveySubmission(traineeId, id, signal)} />
        </> : <p className="text-sm text-slate-600">The candidate needs active employment follow-up consent.</p>}
        {followups.data.assignments.length > 0 && <div className="space-y-2 border-t pt-3">
          <h3 className="font-semibold">Assignments</h3>
          <ul className="space-y-2 text-sm">
            {followups.data.assignments.map(item => <li key={item.id} className="rounded border p-3">
              <strong>{item.name}</strong><p>{careerDate(item.scheduledOn)} · {item.status}</p>
            </li>)}
          </ul>
        </div>}
        {schedule.isSuccess && <p role="status" className="text-sm text-emerald-800">Survey assigned.</p>}
        {schedule.isError && <p role="alert" className="text-sm text-red-800">{schedule.error.message}</p>}
      </>}
    </section>
  );
}
