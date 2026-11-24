import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getCandidateExportColumns, makeExportUrl, type ExportFormat, type Report } from '@/api/specializedAnalyticsApi';
import type { MisFilters, MisOptions } from '@/api/misApi';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import {
  canChooseExportPath, canStageExport, prepareExportDestination, saveMisExport,
  type DownloadProgress, type ReadyExport,
} from '@/lib/streamMisExport';

const reports: Record<Report, string> = {
  candidates: 'De-identified candidate outcomes',
  dsdp: 'District Skill Development Plan',
  qpr: 'Ministry quarterly progress report',
  'provider-scorecard': 'Provider performance scorecard',
};
type ExportRequest = {
  report: Report; format: ExportFormat; filters: MisFilters; columns: string[]; quarter: string; direct: boolean;
};
type Job = {
  name: string; status: 'starting' | 'streaming' | 'ready' | 'saved' | 'cancelled' | 'failed';
  progress: DownloadProgress; error?: string; ready?: ReadyExport;
};

const formatBytes = (bytes: number) => {
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} bytes`;
};

export function MisExports({ filters, options }: { filters: MisFilters; options?: MisOptions }) {
  const [open, setOpen] = useState(false);
  const [job, setJob] = useState<Job>();
  const current = useRef<AbortController | null>(null);
  const ready = useRef<{ file: ReadyExport; handedToBrowser: boolean } | undefined>(undefined);
  const { hasAuthority } = useAuth();
  const canExportCandidates = hasAuthority('trainee.read');

  function clearReady() {
    if (!ready.current) return;
    const { file, handedToBrowser } = ready.current;
    ready.current = undefined;
    if (handedToBrowser) window.setTimeout(file.release, 60_000);
    else file.release();
  }

  useEffect(() => () => {
    current.current?.abort();
    clearReady();
  }, []);

  async function start(input: ExportRequest) {
    if (current.current) return;
    clearReady();
    setOpen(false);
    const controller = new AbortController();
    current.current = controller;
    const name = `${input.report === 'candidates' ? 'candidate-outcomes' : input.report}.${input.format}`;
    const mediaType = input.format === 'csv' ? 'text/csv' : 'application/json';
    setJob({ name, status: 'starting', progress: { bytes: 0 } });
    try {
      const destination = await prepareExportDestination(name, mediaType, input.direct);
      const url = makeExportUrl(input.report, input.format, input.filters, input.columns, input.quarter);
      const result = await saveMisExport(destination, url, mediaType, controller.signal, (progress) => {
        if (!controller.signal.aborted) setJob({ name, status: 'streaming', progress });
      });
      if (controller.signal.aborted) {
        result.ready?.release();
        return;
      }
      if (result.ready) ready.current = { file: result.ready, handedToBrowser: false };
      setJob({ name, status: result.ready ? 'ready' : 'saved', ...result });
    } catch (error) {
      const cancelled = controller.signal.aborted || (error instanceof DOMException && error.name === 'AbortError');
      setJob({ name, status: cancelled ? 'cancelled' : 'failed', progress: { bytes: 0 },
        error: cancelled ? undefined : error instanceof Error ? error.message : 'The download could not be completed.' });
    } finally {
      if (current.current === controller) current.current = null;
    }
  }

  const busy = job?.status === 'starting' || job?.status === 'streaming';
  return (
    <div className="space-y-2">
      <button type="button" className="btn-secondary" disabled={busy || !options} onClick={() => setOpen(true)}>Export MIS report</button>
      {job && (
        <div className="space-y-2 rounded-lg border border-slate-200 bg-white p-3 text-sm text-slate-700" aria-label="Download manager">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="font-semibold text-navy">{job.name}</p>
            {busy ? <button type="button" className="btn-secondary" onClick={() => current.current?.abort()}>Cancel download</button>
              : <button type="button" className="btn-secondary" onClick={() => { clearReady(); setJob(undefined); }}>Dismiss download</button>}
          </div>
          <p role={job.error ? 'alert' : 'status'}>
            {job.status === 'starting' ? 'Preparing the export…' : job.status === 'streaming' ? `Received ${formatBytes(job.progress.bytes)}`
              : job.status === 'ready' ? `Ready to save · ${formatBytes(job.progress.bytes)}`
                : job.status === 'saved' ? `Saved to disk · ${formatBytes(job.progress.bytes)}`
                  : job.status === 'cancelled' ? 'Download cancelled.' : job.error}
          </p>
          {job.status === 'streaming' && job.progress.totalBytes != null && (
            <progress aria-label="Bytes received" className="w-full" value={job.progress.bytes} max={job.progress.totalBytes} />
          )}
          {job.ready && <a className="btn-primary" href={job.ready.href} download={job.name}
            onClick={() => { if (ready.current) ready.current.handedToBrowser = true; }}>Save to browser downloads</a>}
        </div>
      )}
      {open && options && <ExportDialog filters={filters} options={options} allowCandidates={canExportCandidates}
        onClose={() => setOpen(false)} onSubmit={(input) => { void start(input); }} />}
    </div>
  );
}

function ExportDialog({ filters, options, allowCandidates, onClose, onSubmit }: {
  filters: MisFilters; options: MisOptions; allowCandidates: boolean; onClose: () => void; onSubmit: (request: ExportRequest) => void;
}) {
  const [report, setReport] = useState<Report>(allowCandidates ? 'candidates' : 'qpr');
  const [format, setFormat] = useState<ExportFormat>('csv');
  const [quarter, setQuarter] = useState('Q1');
  const [fiscalYear, setFiscalYear] = useState(filters.fiscalYear ?? options.financialYears[0] ?? '');
  const [district, setDistrict] = useState<number | undefined>(filters.districtId);
  const [chosen, setChosen] = useState<Set<string>>();
  const browser = canStageExport();
  const direct = canChooseExportPath();
  const [destination, setDestination] = useState<'direct' | 'browser'>(direct ? 'direct' : 'browser');
  const columns = useQuery({
    queryKey: ['government-mis', 'export-columns'], queryFn: ({ signal }) => getCandidateExportColumns(signal),
    enabled: report === 'candidates', retry: false,
  });
  const selected = chosen ?? new Set(columns.data?.map((item) => item.key) ?? []);
  const eligibleDistricts = options.districts.filter((item) => !filters.stateId || item.stateId === filters.stateId);
  const cohortLabels = [
    report === 'qpr' ? fiscalYear : filters.fiscalYear,
    options.schemes.find((item) => item.id === filters.schemeId)?.name,
    options.sectors.find((item) => item.id === filters.sectorId)?.name,
    options.states.find((item) => item.id === filters.stateId)?.name,
    options.districts.find((item) => item.id === (report === 'dsdp' ? district : filters.districtId))?.name,
    options.genders.find((item) => item.code === filters.gender)?.name,
  ].filter(Boolean);
  const valid = (browser || direct) && (report !== 'candidates' || selected.size > 0)
    && (report !== 'dsdp' || district != null) && (report !== 'qpr' || Boolean(fiscalYear));

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!valid) return;
    const chosenFilters = report === 'qpr' ? { ...filters, fiscalYear }
      : report === 'dsdp' ? { ...filters, districtId: district } : filters;
    onSubmit({ report, format, quarter, filters: chosenFilters, columns: [...selected], direct: destination === 'direct' });
  }

  return (
    <Modal open title="Export MIS report" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4 p-5">
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label className="form-label" htmlFor="mis-export-report">Report</label>
            <select id="mis-export-report" className="form-input" value={report} onChange={(event) => setReport(event.target.value as Report)}>
              {(Object.keys(reports) as Report[]).filter((key) => key !== 'candidates' || allowCandidates)
                .map((key) => <option key={key} value={key}>{reports[key]}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="mis-export-format">Format</label>
            <select id="mis-export-format" className="form-input" value={format} onChange={(event) => setFormat(event.target.value as ExportFormat)}>
              <option value="csv">CSV</option><option value="json">JSON</option>
            </select>
          </div>
        </div>
        <p className="rounded-md bg-slate-50 p-2.5 text-sm text-slate-700">
          <strong>Reporting cohort</strong> · {cohortLabels.length ? cohortLabels.join(' · ') : 'All eligible records'}
        </p>
        {report === 'candidates' ? (
          <>
            <p className="text-sm text-slate-600">One row per candidate’s latest enrollment with active analytics consent. Small groups are excluded and no identifiers are exported.</p>
            {columns.isPending && <LoadingSpinner label="Loading export columns" />}
            {columns.isError && <ErrorState error={columns.error} title="Unable to load columns" onRetry={() => void columns.refetch()} />}
            {columns.data && <fieldset className="rounded-md border border-slate-200 p-3">
              <legend className="px-1 text-sm font-semibold">Include columns</legend>
              <div className="grid gap-2 sm:grid-cols-2">
                {columns.data.map((column) => <label key={column.key} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={selected.has(column.key)} onChange={(event) => {
                    const next = new Set(selected);
                    if (event.target.checked) next.add(column.key);
                    else next.delete(column.key);
                    setChosen(next);
                  }} />
                  {column.label}
                </label>)}
              </div>
            </fieldset>}
          </>
        ) : <p className="text-sm text-slate-600">This statutory report uses its standard set of columns.</p>}
        {report === 'dsdp' && <div>
          <label className="form-label" htmlFor="mis-export-district">Report district</label>
          <select id="mis-export-district" className="form-input" value={district ?? ''} onChange={(event) => setDistrict(Number(event.target.value) || undefined)} required>
            <option value="">Select a district</option>
            {eligibleDistricts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </div>}
        {report === 'qpr' && <div className="grid gap-3 sm:grid-cols-2">
          <div><label className="form-label" htmlFor="mis-export-year">Report financial year</label>
            <select id="mis-export-year" className="form-input" value={fiscalYear} onChange={(event) => setFiscalYear(event.target.value)}>
              {options.financialYears.map((year) => <option key={year}>{year}</option>)}
            </select>
          </div>
          <div><label className="form-label" htmlFor="mis-export-quarter">Quarter</label>
            <select id="mis-export-quarter" className="form-input" value={quarter} onChange={(event) => setQuarter(event.target.value)}>
              {['Q1', 'Q2', 'Q3', 'Q4'].map((item) => <option key={item}>{item}</option>)}
            </select>
          </div>
        </div>}
        <div>
          <label className="form-label" htmlFor="mis-export-destination">Save destination</label>
          <select id="mis-export-destination" className="form-input" value={destination}
            onChange={(event) => setDestination(event.target.value as 'direct' | 'browser')}>
            {direct && <option value="direct">Choose a file location</option>}
            {browser && <option value="browser">Browser downloads</option>}
          </select>
          {!browser && !direct && <p className="form-error">This browser cannot stream an export to disk. Open the local site in a browser with file saving support.</p>}
        </div>
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={!valid}>Start export</button>
        </div>
      </form>
    </Modal>
  );
}
