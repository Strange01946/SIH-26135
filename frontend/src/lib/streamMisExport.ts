import { getAccessToken, getSessionGeneration, isAccessTokenExpiring, refreshAccessToken } from './sessionManager';

type Picker = (options: { suggestedName: string; types: Array<{ description: string; accept: Record<string, string[]> }> }) =>
  Promise<FileSystemFileHandle>;
const choice = () => (window as Window & { showSaveFilePicker?: Picker }).showSaveFilePicker;
export const canChooseExportPath = () => typeof choice() === 'function';
export const canStageExport = () => typeof globalThis.navigator?.storage?.getDirectory === 'function'
  && typeof globalThis.FileSystemFileHandle?.prototype.createWritable === 'function';

export type DownloadProgress = { bytes: number; totalBytes?: number };
export type ReadyExport = { href: string; release: () => void };

export async function prepareExportDestination(filename: string, mediaType: string, direct: boolean) {
  if (direct) {
    const pick = choice();
    if (!pick) throw new Error('This browser does not offer a save-location picker.');
    const handle = await pick.call(window, {
      suggestedName: filename, types: [{ description: 'MIS report', accept: { [mediaType]: [filename.slice(filename.lastIndexOf('.'))] } }],
    });
    return { handle, release: async () => {}, direct } as const;
  }
  const root = await navigator.storage.getDirectory();
  const directory = await root.getDirectoryHandle('sih-mis-downloads', { create: true });
  const key = `${Date.now()}-${crypto.randomUUID()}.part`;
  const handle = await directory.getFileHandle(key, { create: true });
  const release = () => directory.removeEntry(key).catch(() => {});
  return { handle, release, direct } as const;
}

export async function fetchMisExport(url: string, mediaType: string, signal: AbortSignal) {
  const generation = getSessionGeneration();
  const validateSession = () => {
    if (signal.aborted || generation !== getSessionGeneration()) {
      throw new DOMException('Export cancelled', 'AbortError');
    }
  };
  let token = getAccessToken();
  if (!token || isAccessTokenExpiring(token)) token = await refreshAccessToken();
  const request = () => {
    validateSession();
    return fetch(url, {
      signal, cache: 'no-store', redirect: 'error',
      headers: { Authorization: `Bearer ${token}`, Accept: mediaType },
    });
  };
  let response = await request();
  if (response.status === 401) {
    await response.body?.cancel();
    validateSession();
    token = await refreshAccessToken();
    response = await request();
  }
  if (!response.ok) {
    const details = await response.json().catch(() => undefined);
    throw new Error(details?.message || `The server declined the export (HTTP ${response.status}).`);
  }
  if (!response.body || !response.headers.get('content-type')?.startsWith(mediaType)) {
    await response.body?.cancel();
    throw new Error('The server did not return a downloadable report stream.');
  }
  return response;
}

export async function writeMisStream(
  response: Response, writable: WritableStream<Uint8Array>, signal: AbortSignal,
  onProgress: (progress: DownloadProgress) => void,
) {
  if (!response.body) throw new Error('The response contains no report stream.');
  const length = Number(response.headers.get('content-length'));
  const encoding = response.headers.get('content-encoding');
  const identityEncoding = encoding === 'identity' || (!encoding && response.type !== 'cors');
  const totalBytes = Number.isSafeInteger(length) && length > 0 && identityEncoding ? length : undefined;
  let bytes = 0;
  let notifiedAt = -Infinity;
  await response.body.pipeThrough(new TransformStream<Uint8Array, Uint8Array>({
    transform(chunk, controller) {
      bytes += chunk.byteLength;
      controller.enqueue(chunk);
      if (performance.now() - notifiedAt >= 100) {
        notifiedAt = performance.now();
        onProgress({ bytes, totalBytes });
      }
    },
    flush() {
      if (totalBytes != null && bytes !== totalBytes) throw new Error('The report ended before all bytes were received.');
    },
  })).pipeTo(writable, { signal });
  const progress = { bytes, totalBytes };
  onProgress(progress);
  return progress;
}

export async function saveMisExport(
  destination: Awaited<ReturnType<typeof prepareExportDestination>>, url: string, mediaType: string,
  signal: AbortSignal, onProgress: (progress: DownloadProgress) => void,
): Promise<{ progress: DownloadProgress; ready?: ReadyExport }> {
  let body: ReadableStream<Uint8Array> | null = null;
  try {
    const response = await fetchMisExport(url, mediaType, signal);
    body = response.body;
    const writable = await destination.handle.createWritable();
    const progress = await writeMisStream(response, writable, signal, onProgress);
    if (destination.direct) return { progress };
    const file = await destination.handle.getFile();
    const href = URL.createObjectURL(file);
    return { progress, ready: {
      href,
      release: () => { URL.revokeObjectURL(href); void destination.release(); },
    } };
  } catch (error) {
    if (body && !body.locked) await body.cancel().catch(() => {});
    await destination.release();
    throw error;
  }
}
