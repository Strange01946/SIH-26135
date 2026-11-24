import { apiClient, apiGet, apiGetPaged } from './client';

export type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };
export type JsonObject = Record<string, JsonValue>;

export type AuditEntry = {
  id: number;
  actorUserId?: number;
  auditActionCode: string;
  entityType: string;
  entityId?: number;
  occurredAt: string;
  correlationId?: string;
  changeSummary?: string;
  stateChange?: { before: JsonObject | null; after: JsonObject | null; redactedFields: string[] } | null;
};

export type Incident = {
  id: number;
  eventCode: string;
  systemEventSeverityCode: string;
  systemEventCategoryName: string;
  sourceComponent?: string;
  entityType?: string;
  entityId?: number;
  eventSummary: string;
  occurredAt: string;
};

export type ThrottleStatus = {
  eventDedupWindowSeconds: number;
  rateLimitCooldownSeconds: number;
  dataQualityCooldownSeconds: number;
  suppressedSinceStartup: { duplicateEvents: number; rateLimitAlerts: number; dataQualityAlerts: number };
};

type Health = {
  status: string;
  components?: Record<string, { status: string }>;
};

type VersionInfo = {
  build?: { name?: string; version?: string };
  app?: { name?: string; version?: string };
};

export type AuditFilters = {
  entityType: string;
  entityId: string;
  actorUserId: string;
  auditActionCode: string;
  start: string;
  end: string;
  correlationId: string;
};
export type EventFilters = { severityCode: string; correlationId: string; eventCode: string };

function searchParams<T extends Record<string, string>>(filters: T, page: number) {
  return { ...Object.fromEntries(Object.entries(filters).filter(([, value]) => value.trim()).map(([key, value]) => [key, value.trim()])), page };
}

const health = async (path: string, signal?: AbortSignal) =>
  (await apiClient.get<Health>(path, { signal, validateStatus: (status) => status === 200 || status === 503 })).data;

export const governanceApi = {
  auditScope: (signal?: AbortSignal) => apiGet<{ nationwide: boolean }>('/api/v1/audit-logs/scope', { signal }),
  audits: (filters: AuditFilters, page: number, signal?: AbortSignal) =>
    apiGetPaged<AuditEntry>('/api/v1/audit-logs', { params: searchParams(filters, page), signal }),
  incidents: (filters: EventFilters, page: number, signal?: AbortSignal) =>
    apiGetPaged<Incident>('/api/v1/system-event-logs', { params: searchParams(filters, page), signal }),
  throttles: (signal?: AbortSignal) => apiGet<ThrottleStatus>('/api/v1/system-event-logs/throttle-status', { signal }),
  health: (signal?: AbortSignal) => health('/actuator/health', signal),
  diagnostics: (signal?: AbortSignal) => health('/actuator/health/diagnostics', signal),
  info: async (signal?: AbortSignal) => (await apiClient.get<VersionInfo>('/actuator/info', { signal })).data,
  prometheus: async (signal?: AbortSignal) => (await apiClient.get<string>('/actuator/prometheus', {
    signal, responseType: 'text', headers: { Accept: 'text/plain' },
  })).data,
};
