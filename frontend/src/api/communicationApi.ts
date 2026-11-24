import { apiGet, apiGetPaged, apiPost } from './client';
import type { PagedResponse } from './types';

export type TemplateDefinition = {
  templateCode: string;
  subject: string;
  bodyTemplate: string;
  allowedParameterKeys: string[];
};

export type RenderedTemplate = {
  templateCode: string;
  subject: string;
  bodyText: string;
};

export type DeliveryLog = {
  id: number;
  templateCode?: string;
  purpose: string;
  channel: string;
  status: string;
  recipientMasked?: string;
  optedOut: boolean;
  demoReceipt: boolean;
  createdAt: string;
  sentAt?: string;
  deliveredAt?: string;
};

export type QueueJob = {
  id: number;
  templateCode: string;
  channel: string;
  status: string;
  attemptCount: number;
  maxAttempts: number;
  deliveryMode: string;
  scheduledAt: string;
  updatedAt: string;
  communicationLogId?: number;
};

type DispatchQueue = {
  counts: { status: string; count: number }[];
  jobs: PagedResponse<QueueJob>;
};

const root = '/api/v1/communication';

export const communicationApi = {
  templates: (signal?: AbortSignal) =>
    apiGet<TemplateDefinition[]>(root + '/templates', { signal }),
  render: (code: string, variables: Record<string, string>, signal?: AbortSignal) =>
    apiPost<typeof variables, RenderedTemplate>(root + '/templates/' + encodeURIComponent(code) + '/render', variables, { signal }),
  logs: (params: { q: string; channel: string; status: string; page: number }, signal?: AbortSignal) =>
    apiGetPaged<DeliveryLog>(root + '/logs', { params, signal }),
  queue: (params: { status: string; page: number }, signal?: AbortSignal) =>
    apiGet<DispatchQueue>(root + '/jobs', { params, signal }),
  capabilities: (signal?: AbortSignal) =>
    apiGet<{ demoAvailable: boolean }>(root + '/center', { signal }),
  createDemo: () => apiPost<undefined, DeliveryLog>(root + '/demo/receipts', undefined),
  deliverDemo: (id: number) => apiPost<undefined, DeliveryLog>(root + '/demo/receipts/' + id + '/deliver', undefined),
  checkConsent: () => apiPost<undefined, { status: string; simulated: boolean }>(root + '/demo/consent', undefined),
};
