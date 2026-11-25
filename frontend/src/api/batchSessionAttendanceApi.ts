import { apiGet, apiPut } from './client';

export interface AttendanceSession {
  id: number;
  startsAt: string;
  endsAt: string;
  started: boolean;
  canMark: boolean;
}

export interface AttendanceTimetable {
  batchCode: string;
  title: string;
  batchStatus: string;
  sessions: AttendanceSession[];
}

export interface AttendanceRosterRow {
  enrollmentId: number;
  name: string;
  registrationNumber: string;
  eligible: boolean;
  status?: string | null;
  remarks?: string | null;
  version?: string | null;
}

export interface AttendanceSessionRoster {
  session: AttendanceSession;
  rows: AttendanceRosterRow[];
}

export interface SessionAttendanceMark {
  enrollmentId: number;
  status: string;
  remarks?: string;
  expectedVersion?: string | null;
}

export const batchAttendanceKey = (batchId: number) => ['provider', 'batch-attendance', batchId] as const;
const sessionsPath = (batchId: number) => `/api/v1/training-batches/${batchId}/attendance/sessions`;

export const getAttendanceTimetable = (batchId: number, signal?: AbortSignal) =>
  apiGet<AttendanceTimetable>(sessionsPath(batchId), { signal });
export const getSessionRoster = (batchId: number, sessionId: number, signal?: AbortSignal) =>
  apiGet<AttendanceSessionRoster>(`${sessionsPath(batchId)}/${sessionId}`, { signal });
export const saveSessionRoster = (batchId: number, sessionId: number, marks: SessionAttendanceMark[]) =>
  apiPut<{ marks: SessionAttendanceMark[] }, AttendanceSessionRoster>(`${sessionsPath(batchId)}/${sessionId}`, { marks });
