import { useRef, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  getFollowupSurveys,
  getFollowupQuestionnaire,
  submitFollowupQuestionnaire,
  type FollowupQuestion,
  type FollowupQuestionnaire,
  type FollowupAnswer,
} from '@/api/traineeApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function TraineeSurveys({ traineeId }: { traineeId: number }) {
  const queryClient = useQueryClient();
  const [taskId, setTaskId] = useState<number>();
  const [receipt, setReceipt] = useState('');
  const overviewKey = ['trainee', 'surveys', traineeId];
  const overview = useQuery({ queryKey: overviewKey, queryFn: ({ signal }) => getFollowupSurveys(signal) });
  const questionnaire = useQuery({
    queryKey: ['trainee', 'questionnaire', traineeId, taskId],
    queryFn: ({ signal }) => getFollowupQuestionnaire(taskId!, signal),
    enabled: taskId !== undefined,
  });

  return (
    <section className="card space-y-4" aria-labelledby="followup-surveys-heading">
      <h2 id="followup-surveys-heading" className="text-lg font-bold text-navy">Post-training follow-up surveys</h2>
      <p className="text-sm text-gray-600">
        Report what happened after training so providers can improve courses and help trainees find suitable work.
      </p>
      {receipt && <p role="status" className="text-sm text-emerald-800">{receipt}</p>}
      {overview.isPending ? <LoadingSpinner label="Loading follow-up surveys" /> : overview.isError ? (
        <ErrorState error={overview.error} title="Unable to load follow-up surveys" onRetry={() => void overview.refetch()} />
      ) : (
        <>
          {!overview.data.followupConsentGranted ? (
            <p className="text-sm">
              Employment follow-up consent is required to take an assigned survey.{' '}
              <Link className="font-semibold text-navy underline" to="/trainee/consent">Manage consent</Link>
            </p>
          ) : overview.data.assignments.length === 0 ? (
            <p className="text-sm text-gray-600">You have no open survey assignments.</p>
          ) : (
            <ul className="space-y-2">
              {overview.data.assignments.map((assignment) => (
                <li key={assignment.taskId} className="flex flex-wrap items-center justify-between gap-3 rounded border p-3">
                  <div>
                    <p className="font-medium">{assignment.name}</p>
                    {assignment.closesOn && <p className="text-sm text-gray-600">Closes {assignment.closesOn}</p>}
                  </div>
                  {assignment.taskId !== taskId && (
                    <button type="button" className="btn-outline" onClick={() => { setTaskId(assignment.taskId); setReceipt(''); }}>
                      Open {assignment.name}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
          {taskId !== undefined && overview.data.followupConsentGranted && (
            questionnaire.isLoading ? <LoadingSpinner label="Loading questionnaire" /> : questionnaire.isError ? (
              <ErrorState error={questionnaire.error} title="Unable to open questionnaire" onRetry={() => void questionnaire.refetch()} />
            ) : questionnaire.data && (
              <QuestionnaireForm
                key={`${taskId}-${questionnaire.data.versionId}`}
                questionnaire={questionnaire.data}
                onCancel={() => setTaskId(undefined)}
                onSubmitted={async (name) => {
                  setTaskId(undefined);
                  setReceipt(`Survey submitted · ${name}`);
                  await queryClient.invalidateQueries({ queryKey: overviewKey });
                }}
              />
            )
          )}
          <h3 className="border-t pt-4 font-semibold">Recent submissions</h3>
          {overview.data.recentSubmissions.length ? (
            <ul className="space-y-2 text-sm">
              {overview.data.recentSubmissions.map((submission) => (
                <li key={submission.id}>{submission.name}{submission.submittedAt && ` · ${new Date(submission.submittedAt).toLocaleDateString('en-IN')}`}</li>
              ))}
            </ul>
          ) : <p className="text-sm text-gray-600">You have no previous survey submissions.</p>}
        </>
      )}
    </section>
  );
}

function QuestionnaireForm({ questionnaire, onCancel, onSubmitted }: {
  questionnaire: FollowupQuestionnaire;
  onCancel: () => void;
  onSubmitted: (name: string) => Promise<void>;
}) {
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState<Record<number, FollowupAnswer>>({});
  const [fieldError, setFieldError] = useState('');
  const submitting = useRef(false);
  const { assignment, questions, versionId } = questionnaire;
  const question = questions[step];
  const currentAnswer = question && answers[question.id];
  const submit = useMutation({
    mutationFn: () => submitFollowupQuestionnaire(assignment.taskId, {
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
      <h3 className="font-semibold">{assignment.name}</h3>
      {question ? (
        <fieldset disabled={submit.isPending} key={question.id} className="space-y-2" aria-describedby={fieldError ? 'survey-answer-error' : undefined}>
          <legend className="font-medium">{question.text} {question.required && <span className="text-sm text-gray-600">(required)</span>}</legend>
          <p className="text-sm text-gray-600">Question {step + 1} of {questions.length}</p>
          <QuestionInput question={question} answer={currentAnswer} update={update} />
          {fieldError && <p id="survey-answer-error" role="alert" className="text-sm text-red-800">{fieldError}</p>}
        </fieldset>
      ) : <p role="status">This questionnaire has no questions yet.</p>}
      {submit.isError && <p role="alert" className="text-sm text-red-800">{submit.error.message}</p>}
      <div className="flex flex-wrap gap-2">
        <button type="button" className="btn-outline" disabled={submit.isPending} onClick={onCancel}>Close survey</button>
        {step > 0 && <button type="button" className="btn-outline" disabled={submit.isPending} onClick={() => { setStep(step - 1); setFieldError(''); }}>Previous question</button>}
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
