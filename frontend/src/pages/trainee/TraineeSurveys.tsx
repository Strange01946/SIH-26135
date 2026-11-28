import { useRef, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  getFollowupSurveys,
  getFollowupQuestionnaire,
  submitFollowupQuestionnaire,
  getCareerUpdateQuestionnaire,
  submitCareerUpdate,
  getOwnSurveySubmission,
  type FollowupQuestion,
  type FollowupSubmitRequest,
  type FollowupSubmission,
  type FollowupAnswer,
} from '@/api/traineeApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { SurveySubmissions } from '@/components/career/SurveySubmissions';
import { careerDate } from '@/components/career/CareerInsights';

export function TraineeSurveys({ traineeId }: { traineeId: number }) {
  const queryClient = useQueryClient();
  const [taskId, setTaskId] = useState<number | 'career-update'>();
  const [receipt, setReceipt] = useState('');
  const overviewKey = ['trainee', 'surveys', traineeId];
  const overview = useQuery({
    queryKey: overviewKey, queryFn: ({ signal }) => getFollowupSurveys(signal),
    refetchOnMount: 'always', refetchOnWindowFocus: 'always',
  });
  const questionnaire = useQuery({
    queryKey: ['trainee', 'questionnaire', traineeId, taskId],
    queryFn: async ({ signal }) => {
      if (typeof taskId === 'number') {
        const result = await getFollowupQuestionnaire(taskId, signal);
        return { name: result.assignment.name, versionId: result.versionId, questions: result.questions, initialAnswers: [] };
      }
      return getCareerUpdateQuestionnaire(signal);
    },
    enabled: taskId !== undefined && overview.data?.followupConsentGranted === true,
    retry: false,
    staleTime: taskId === 'career-update' ? 0 : undefined,
  });

  const open = (id: number | 'career-update') => { setTaskId(id); setReceipt(''); };

  return (
    <section className="card space-y-4 p-4 sm:p-6" aria-labelledby="followup-surveys-heading">
      <h2 id="followup-surveys-heading" className="text-lg font-bold text-navy">Post-training follow-up surveys</h2>
      <p className="text-sm text-gray-600">Follow-up is optional. Use <a className="underline" href="#career-milestones-heading">Employment history</a> to record a job at any time.</p>
      <p className="text-sm text-gray-600">
        Automatic and staff-assigned surveys appear here when available. Use them to share changes in work, income or skills.
      </p>
      {receipt && <p role="status" className="text-sm text-emerald-800">{receipt}</p>}
      {overview.isPending ? <LoadingSpinner label="Loading follow-up surveys" /> : overview.isError ? (
        <ErrorState error={overview.error} title="Unable to load follow-up surveys" onRetry={() => void overview.refetch()} />
      ) : (
        <>
          <section className="space-y-2 rounded border bg-slate-50 p-4" aria-labelledby="employment-update-heading">
            <h3 id="employment-update-heading" className="font-semibold text-navy">Report employment update</h3>
            <p className="text-sm text-gray-600">Update your work status, wages, job details or skills. Submitted answers appear in your current career information immediately.</p>
            {!overview.data.followupConsentGranted ? (
              <>
                <p className="text-sm">Employment follow-up consent is required to report an update or take an assigned survey.</p>
                <Link className="btn-secondary" to="/trainee/consent#employment-follow-up">Review employment follow-up consent</Link>
              </>
            ) : !overview.data.careerUpdateAvailable ? (
              <p className="text-sm text-gray-600">The employment update questionnaire is currently unavailable.</p>
            ) : taskId !== 'career-update' && (
              <button type="button" className="btn-primary" onClick={() => open('career-update')}>Report employment update</button>
            )}
          </section>
          {overview.data.followupConsentGranted && (overview.data.assignments.length === 0 ? (
            <p className="text-sm text-gray-600">You have no open survey assignments.</p>
          ) : (
            <ul className="space-y-2">
              {overview.data.assignments.map((assignment) => (
                <li key={assignment.taskId} className="flex flex-wrap items-center justify-between gap-3 rounded border p-3">
                  <div>
                    <p className="font-medium">{assignment.name}</p>
                    {assignment.closesOn && <p className="text-sm text-gray-600">Closes {careerDate(assignment.closesOn)}</p>}
                  </div>
                  {assignment.taskId !== taskId && (
                    <button type="button" className="btn-secondary" onClick={() => open(assignment.taskId)}>
                      Open {assignment.name}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          ))}
          {taskId !== undefined && overview.data.followupConsentGranted && (
            questionnaire.isLoading || (taskId === 'career-update' && questionnaire.isFetching)
              ? <LoadingSpinner label="Loading questionnaire" /> : questionnaire.isError ? (
              <div className="space-y-2">
                <ErrorState error={questionnaire.error} title="Unable to open questionnaire" onRetry={() => void questionnaire.refetch()} />
                <button type="button" className="btn-secondary" onClick={() => { setTaskId(undefined); void overview.refetch(); }}>Back to surveys</button>
              </div>
            ) : questionnaire.data && (
              <QuestionnaireForm
                key={`${taskId}-${questionnaire.data.versionId}`}
                questionnaire={questionnaire.data}
                submitAnswers={payload => typeof taskId === 'number'
                  ? submitFollowupQuestionnaire(taskId, payload) : submitCareerUpdate(payload)}
                onCancel={() => setTaskId(undefined)}
                onSubmitted={async (name) => {
                  setTaskId(undefined);
                  setReceipt(`Survey submitted · ${name}`);
                  await Promise.all([
                    queryClient.invalidateQueries({ queryKey: overviewKey }),
                    queryClient.invalidateQueries({ queryKey: ['trainee', 'recorded-career-status', traineeId] }),
                    queryClient.invalidateQueries({ queryKey: ['trainee', 'own-profile'] }),
                  ]);
                }}
              />
            )
          )}
          <SurveySubmissions entries={overview.data.recentSubmissions} scopeKey={overviewKey} load={getOwnSurveySubmission} />
        </>
      )}
    </section>
  );
}

function QuestionnaireForm({ questionnaire, submitAnswers, onCancel, onSubmitted }: {
  questionnaire: { name: string; versionId: number; questions: FollowupQuestion[]; initialAnswers?: FollowupAnswer[] };
  submitAnswers: (payload: FollowupSubmitRequest) => Promise<FollowupSubmission>;
  onCancel: () => void;
  onSubmitted: (name: string) => Promise<void>;
}) {
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState<Record<number, FollowupAnswer>>(() =>
    Object.fromEntries((questionnaire.initialAnswers ?? []).map(answer => [answer.questionId, answer])));
  const [fieldError, setFieldError] = useState('');
  const submitting = useRef(false);
  const { name, questions, versionId } = questionnaire;
  const question = questions[step];
  const currentAnswer = question && answers[question.id];
  const submit = useMutation({
    mutationFn: () => submitAnswers({
      versionId,
      answers: questions.flatMap((item) => answers[item.id] ? [answers[item.id]] : []),
    }),
    onSuccess: (result) => onSubmitted(result.name),
    onSettled: () => { submitting.current = false; },
  });

  function update(answer?: Omit<FollowupAnswer, 'questionId'>) {
    if (!question) return;
    setAnswers((previous) => {
      const next = { ...previous };
      if (answer) next[question.id] = { questionId: question.id, ...answer };
      else delete next[question.id];
      return next;
    });
    setFieldError('');
    submit.reset();
  }

  function next(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question || submitting.current) return;
    if (question.required && !currentAnswer) {
      setFieldError('Answer this question to continue.');
      return;
    }
    if (step < questions.length - 1) {
      setFieldError('');
      setStep(step + 1);
      return;
    }
    submitting.current = true;
    submit.mutate();
  }

  return (
    <form className="space-y-4 rounded border bg-gray-50 p-4" onSubmit={next}>
      <h3 className="font-semibold">{name}</h3>
      {question ? (
        <fieldset disabled={submit.isPending} key={question.id} className="space-y-2" aria-describedby={fieldError ? 'survey-answer-error' : undefined}>
          <legend className="font-medium">{question.text} <span className="text-sm text-gray-600">({question.required ? 'required' : 'optional'})</span></legend>
          <p className="text-sm text-gray-600">Question {step + 1} of {questions.length}</p>
          <QuestionInput question={question} answer={currentAnswer} update={update} />
          {fieldError && <p id="survey-answer-error" role="alert" className="text-sm text-red-800">{fieldError}</p>}
        </fieldset>
      ) : <p role="status">This questionnaire has no questions yet.</p>}
      {submit.isError && <p role="alert" className="text-sm text-red-800">{submit.error.message}</p>}
      <div className="flex flex-wrap gap-2">
        <button type="button" className="btn-secondary" disabled={submit.isPending} onClick={onCancel}>Close survey</button>
        {step > 0 && <button type="button" className="btn-secondary" disabled={submit.isPending} onClick={() => { setStep(step - 1); setFieldError(''); }}>Previous question</button>}
        {question && <button type="submit" className="btn-primary" disabled={submit.isPending}>
          {step < questions.length - 1 ? 'Next question' : submit.isPending ? 'Submitting…' : 'Submit survey'}
        </button>}
      </div>
    </form>
  );
}

function QuestionInput({ question, answer, update }: {
  question: FollowupQuestion;
  answer?: FollowupAnswer;
  update: (answer?: Omit<FollowupAnswer, 'questionId'>) => void;
}) {
  switch (question.type) {
    case 'MULTI_CHOICE':
      return question.options.map((option) => (
        <label key={option.id} className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={answer?.optionIds?.includes(option.id) ?? false} onChange={(event) => {
            const current = answer?.optionIds ?? [];
            const selected = event.target.checked ? [...current, option.id] : current.filter((id) => id !== option.id);
            update(selected.length ? { optionIds: selected } : undefined);
          }} /> {option.label}
        </label>
      ));
    case 'SINGLE_CHOICE':
      return <select className="form-input" aria-label={question.text} value={answer?.optionIds?.[0] ?? ''} onChange={(event) =>
        update(event.target.value ? { optionIds: [Number(event.target.value)] } : undefined)}>
        <option value="">Select an answer</option>
        {question.options.map((option) => <option key={option.id} value={option.id}>{option.label}</option>)}
      </select>;
    case 'BOOLEAN':
      return <select className="form-input" aria-label={question.text} value={answer?.yes === undefined ? '' : String(answer.yes)} onChange={(event) =>
        update(event.target.value ? { yes: event.target.value === 'true' } : undefined)}>
        <option value="">Select an answer</option>
        <option value="true">Yes</option>
        <option value="false">No</option>
      </select>;
    case 'TEXT':
      return <textarea className="form-input" aria-label={question.text} maxLength={1000} defaultValue={answer?.text}
        onChange={(event) => update(event.target.value.trim() ? { text: event.target.value } : undefined)} />;
    case 'DATE':
      return <input type="date" className="form-input" aria-label={question.text} value={answer?.date ?? ''}
        onChange={(event) => update(event.target.value ? { date: event.target.value } : undefined)} />;
    case 'NUMBER':
    case 'DECIMAL':
    case 'SCALE':
      return <input type="number" className="form-input" aria-label={question.text} min={0}
        max={question.type === 'DECIMAL' ? 9_999_999_999.99 : 9_999_999_999} step={question.type === 'DECIMAL' ? 0.01 : 1}
        defaultValue={answer?.number} onChange={(event) => update(Number.isFinite(event.target.valueAsNumber) ?
          { number: event.target.valueAsNumber } : undefined)} />;
    default:
      return <p role="alert">This question uses an unsupported answer type.</p>;
  }
}
